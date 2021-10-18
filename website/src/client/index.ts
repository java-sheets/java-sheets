import {
  EvaluateResponse,
  StartEvaluationRequest,
  EvaluationResult,
  StopEvaluationRequest,
  EvaluationError,
  MissingSources,
  EvaluateRequest
} from "@jsheets/protocol/src/jsheets/api/snippet_runtime_pb";
import * as SheetProtocol from "@jsheets/protocol/src/jsheets/api/sheet_pb";
import {ComponentState, SheetState, SnippetState} from '../sheet'

export interface EvaluationListener {
  onEnd(): void
  onResult(result: EvaluationResult): void
  onServiceError(code: number): void
  onEvaluationError(error: EvaluationError): void
  onMissingSources(sources: MissingSources): void
}

export interface Evaluation {
  stop(): void
}

export interface Location {
  host: string
  protocol: string
}

export default class Client {
  static create(): Client {
    return Client.forHost(window.location)
  }

  static forHost(location: Location): Client {
    return new Client(
      `${location.protocol}//${location.host}`,
      this.resolveWebSocketHost(location)
    )
  }

  private static resolveWebSocketHost(location: Location): string {
    const uri = location.protocol === "https:" ? "wss:" : "ws:"
    return `${uri}//${location.host}`
  }

  private readonly sheets_: SheetClient

  private constructor(
    private readonly baseUrl: string,
    private readonly webSocketUrl: string
  ) {
    this.sheets_ = new SheetClient(baseUrl)
  }

  evaluate(start: StartEvaluationRequest, listener: EvaluationListener): Evaluation {
    const client = new WebSocket(`${this.webSocketUrl}/api/v1/evaluate`)

    client.onerror = () => {
      listener.onEnd()
    }

    client.onopen = () => {
      const request = new EvaluateRequest()
      request.setStart(start)
      client.send(request.serializeBinary())
    }

    client.onmessage = async (message) => {
      const blob = message.data as Blob
      if (blob === undefined) {
        return
      }
      const bytes = await blob.arrayBuffer()
      let response
      try {
        response = EvaluateResponse.deserializeBinary(new Uint8Array(bytes))
      } catch (error) {
        throw new Error(JSON.stringify({error, note: 'received invalid message', message: message.data}))
      }
      response.getErrorList()?.forEach(listener.onEvaluationError)
      response.getMissingSourcesList()?.forEach(listener.onMissingSources)
      response.getResultList()?.forEach(listener.onResult)
    }

    client.onclose = event => {
      console.log({message: 'evaluation was closed', event})
      if (isWebSocketErrorCode(event.code)) {
        listener.onServiceError(event.code)
      }
      listener.onEnd()
    }

    return new WebSocketEvaluation(client)
  }

  sheets(): SheetClient {
    return this.sheets_
  }
}

function isWebSocketErrorCode(code: number) {
  return code >= 400 && code < 600
}

class WebSocketEvaluation implements Evaluation {
  constructor(private readonly connection: WebSocket) {}

  stop() {
    const request = new EvaluateRequest()
    request.setStop(new StopEvaluationRequest())
    this.connection.send(request.serializeBinary())
  }
}

export class SheetClient {
  constructor(private readonly baseUrl: string) { }

  async find(id: string): Promise<SheetState> {
    const call = await fetch(`${this.baseUrl}/api/v1/sheets/${id}`)
    if (call.ok) {
      const body = await call.json()
      return createSheetFromResponse(body)
    }
    throw new Error(`${call.status}`)
  }

  async post(snippet: Partial<SheetProtocol.Sheet.AsObject>): Promise<SheetState> {
    convertRequest(snippet)
    const call = await fetch(`${this.baseUrl}/api/v1/sheets`, {
      method: 'POST',
      body: JSON.stringify(snippet)
    })
    if (call.ok) {
      const body = await call.json()
      return createSheetFromResponse(body)
    }
    throw new Error(`${call.status}`)
  }
}

function createSheetFromResponse(response: Record<string, any>): SheetState {
  convertResponse(response)
  const message = response as SheetProtocol.Sheet.AsObject
  const snippets: Record<string, SnippetState> = {}
  for (const snippet of message.snippetsList) {
    const components: Record<string, ComponentState> = {}
    for (const component of snippet.componentsList) {
      const kind = (component.kind as unknown) as string
      components[component.id] = {
        id: component.id,
        order: component.order,
        type: kind.toLowerCase() === "code" ? "code" : "text",
        content: component.content
      }
    }
    snippets[snippet.id] = {
      id: snippet.id,
      order: snippet.order,
      title: snippet.name,
      components
    }
  }
  return {
    id: message.id,
    title: message.title,
    description: message.description,
    snippets: snippets
  } as SheetState
}

function convertResponse(object: Record<string, any>) {
  return process(object, '', convertResponseKey(['snippets', 'snippets.components']))
}

function convertResponseKey(listKeys: string[]) {
  return (key: string, path: string) => {
    const converted = convertSnakeCaseToCamelCase(key)
    return listKeys.includes(path)
      ? `${converted}List`
      : converted
  }
}

function convertRequest(object: Record<string, any>) {
  return process(object, '', convertRequestKey)
}

function process(
  object: Record<string, any>,
  path: string,
  mapKey: (key: string, path: string) => string
) {
  if (typeof object !== 'object') {
    return
  }
  for (const [key, value] of Object.entries(object)) {
    const childPath = path === '' ? key : path + '.' + key
    const fixedKey = mapKey(key, childPath)
    if (fixedKey !== key) {
      object[fixedKey] = value
      delete object[key]
    }
    if (value == null) {
      continue
    }
    if (Array.isArray(value)) {
      for (const element of value) {
        process(element, childPath, mapKey)
      }
    } else {
      process(value, childPath, mapKey)
    }
  }
}

function convertRequestKey(key: string) {
  return convertCamelCaseToSnakeCase(key).replace(/_list$/, "")
}

function convertSnakeCaseToCamelCase(input: string) {
  return input.replace(/_([a-z])?/, match => match?.toUpperCase() || '')
}

function convertCamelCaseToSnakeCase(input: string) {
  return input.split(/(?=[A-Z])/).join('_').toLowerCase()
}
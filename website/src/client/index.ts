import {
  EvaluateResponse,
  StartEvaluationRequest,
  EvaluationResult,
  StopEvaluationRequest,
  EvaluationError,
  MissingSources,
  EvaluateRequest
} from "@jsheets/protocol/src/jsheets/api/snippet_runtime_pb";
import {Sheet} from "@jsheets/protocol/src/jsheets/api/sheet_pb";

export interface EvaluationListener {
  onEnd(): void
  onResult(result: EvaluationResult): void
  onError(error: EvaluationError): void
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
    this.sheets_ = this.sheets()
  }

  evaluate(start: StartEvaluationRequest, listener: EvaluationListener): Evaluation {
    const client = new WebSocket(`${this.webSocketUrl}/api/v1/evaluate`)

    client.onerror = error => {
      console.log({error})
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
      response.getErrorList()?.forEach(listener.onError)
      response.getMissingSourcesList()?.forEach(listener.onMissingSources)
      response.getResultList()?.forEach(listener.onResult)
    }

    client.onclose = event => {
      listener.onEnd()
      console.log({event})
    }

    return new WebSocketEvaluation(client)
  }

  sheets(): SheetClient {
    return this.sheets_
  }
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

  async find(id: string): Promise<Sheet.AsObject> {
    const call = await fetch(`${this.baseUrl}/api/v1/sheets/${id}`)
    if (call.ok) {
      const body = await call.json()
      return JSON.parse(body) as Sheet.AsObject
    }
    throw new Error(`${call.status}`)
  }

  async post(snippet: Sheet.AsObject): Promise<Sheet.AsObject> {
    const call = await fetch(`${this.baseUrl}/api/v1/sheets`, {
      method: 'POST',
      body: JSON.stringify(snippet)
    })
    if (call.ok) {
      const body = await call.json()
      return JSON.parse(body) as Sheet.AsObject
    }
    throw new Error(`${call.status}`)
  }
}
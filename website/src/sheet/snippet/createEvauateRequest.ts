import {SheetSnippet} from '../index'
import * as EvaluationProtocol from '@jsheets/protocol/src/jsheets/api/snippet_runtime_pb'
import * as SnippetProtocol from '@jsheets/protocol/src/jsheets/api/snippet_pb'

export default function createEvaluateRequest(
  sheetId: string,
  snippet: SheetSnippet,
  componentSources: Map<string, string>
) {
  const reference = createSnippetReference(sheetId, snippet)
  const evaluatedSnippet = new EvaluationProtocol.EvaluatedSnippet()
  evaluatedSnippet.setReference(reference)

  const sources = new EvaluationProtocol.SnippetSources()
  sources.setReference(reference)
  for (const [id, code] of componentSources) {
    sources.addCodeComponents(createCodeComponent(id, code, snippet))
  }

  const request = new EvaluationProtocol.StartEvaluationRequest()
  request.setSnippet(evaluatedSnippet)
  request.setSourcesList([sources])
  return request
}

function createSnippetReference(sheetId: string, snippet: SheetSnippet) {
  const reference = new SnippetProtocol.Snippet.Reference()
  reference.setSheetId(sheetId)
  reference.setSnippetId(snippet.id)
  return reference
}

function createCodeComponent(id: string, code: string, snippet: SheetSnippet) {
  const message = new EvaluationProtocol.SnippetSources.CodeComponent()
  message.setId(id)
  message.setCode(code)
  const component = snippet.components.find(target => target.id === id)
  message.setOrder(component?.order || 1)
  return message
}
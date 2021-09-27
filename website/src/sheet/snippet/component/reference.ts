import * as SnippetProtocol from '@jsheets/protocol/src/jsheets/api/snippet_pb'

export interface SnippetComponentReference {
  content(): string | null
  serialize(): SnippetProtocol.Snippet.Component
  updateContent(target: string): void
}

export interface SnippetComponentListRef {
  components: Map<string, SnippetComponentReference>
}
export interface SnippetComponentRef {
  content(): string | null
  updateContent(target: string): void
}

export interface SnippetComponentListRef {
  components: Map<string, SnippetComponentRef>
}
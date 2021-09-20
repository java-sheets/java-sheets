export interface Sheet {
	id: string
	title: string
	description: string
	snippets: SheetSnippet[]
}

export interface SheetSnippet {
	id: string
	order: number
	title: string
	components: SheetSnippetComponent[]
}

export interface SheetSnippetComponent {
	id: string
	order: number
	type: 'code' | 'text'
	content: string
  output?: SheetSnippetComponentOutput
}

export type SheetSnippetComponentOutput = ErrorOutput | MessageOutput

export interface ErrorOutput {
  span: {
    start: number
    end: number
  }
  code: string
  message: string
}

export interface MessageOutput {
  type: "error" | "info"
  message: string
}
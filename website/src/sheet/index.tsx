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
}
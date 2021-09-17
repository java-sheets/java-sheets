import * as Styled from './EditorComponent.style'
import React from 'react'
import {EditorView} from '@codemirror/view'
import Editor from '../editor/Editor'

export interface EditorComponentProperties {
	value: string
}

export default function EditorComponent(properties: EditorComponentProperties) {
	const [editor, setEditor] = React.useState<EditorView>()

	return (
		<Styled.EditorComponent>
			<Editor onMount={setEditor} code={properties.value}/>
		</Styled.EditorComponent>
	)
}
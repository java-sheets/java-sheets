import * as Styled from './TextComponent.style'
import {createGlobalStyle} from 'styled-components'
import React from "react";
import Editor from "../../../editor/Editor";
import {EditorComponentProperties} from "./EditorComponent";

export interface TextComponentProperties {
  value: string
}

const MarkdownEditorStyle = createGlobalStyle`
	#block-menu-container, .block-menu-trigger, .heading-actions, .ProseMirror > .placeholder {
		display: none !important;
	}
`

export default class TextComponent extends React.Component<TextComponentProperties>{
	render() {
		return (
			<Styled.TextComponent>
				<MarkdownEditorStyle/>
				<Styled.Editor
					disableExtensions={['placeholder']}
					defaultValue={this.props.value}
				/>
			</Styled.TextComponent>
		)
	}

	shouldComponentUpdate(
		nextProps: Readonly<EditorComponentProperties>,
		nextState: Readonly<{}>,
		nextContext: any
	): boolean {
		return nextProps.value !== this.props.value
	}
}
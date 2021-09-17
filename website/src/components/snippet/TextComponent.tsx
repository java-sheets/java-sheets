import * as Styled from './TextComponent.style'
import {createGlobalStyle} from 'styled-components'

export interface TextComponentProperties {
  value: string
}


const MarkdownEditorStyle = createGlobalStyle`
	#block-menu-container, .block-menu-trigger, .heading-actions, .ProseMirror > .placeholder {
		display: none !important;
	}
`

export default function TextComponent(properties: TextComponentProperties) {
	return (
  	<Styled.TextComponent>
			<MarkdownEditorStyle/>
			<Styled.Editor
				disableExtensions={['placeholder']}
				defaultValue={properties.value}
			/>
		</Styled.TextComponent>
  )
}
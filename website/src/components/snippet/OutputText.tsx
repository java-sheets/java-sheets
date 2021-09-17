import * as Styled from './OutputText.style'

export interface OutputTextProperties {
	content?: string
}

export default function OutputText(properties: OutputTextProperties) {
	return (
		<Styled.OutputText>
			{properties.content}
		</Styled.OutputText>
	)
}
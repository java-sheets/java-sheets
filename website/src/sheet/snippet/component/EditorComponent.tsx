import * as Styled from './EditorComponent.style'
import React from 'react'
import Editor from '../../../editor/Editor'

export interface EditorComponentProperties {
	value: string
}

export default class EditorComponent extends React.Component<EditorComponentProperties>{
	render() {
		return (
			<Styled.EditorComponent>
				<Editor code={this.props.value}/>
			</Styled.EditorComponent>
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
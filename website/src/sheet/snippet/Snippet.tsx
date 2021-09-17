import * as Styled from './Snippet.style'
import {ExperimentOutlined} from '@ant-design/icons'
import React, {useState} from 'react'
import Title from './Title'
import OutputText from './OutputText'
import {UseSnippet, useSnippet} from './useSheet'
import {SheetSnippet} from '../index'
import ComponentList from "./component/ComponentList";
import SnippetExtras from "./SnippetExtras";

export interface SnippetPosition {
	highestOrder: number
	lowestOrder: number
	moveUp: () => void
	moveDown: () => void
}

interface ExistingSnippetProperties extends UseSnippet {
	snippet: SheetSnippet
	position: SnippetPosition
	headProperties?: any
}

const MemoizedTitle = React.memo(Title)
const MemoizedExtras = React.memo(SnippetExtras)

function ExistingSnippet(
	properties: ExistingSnippetProperties
) {
	const {snippet, deleteComponent, changeDetails} = properties
	const [editingTitle, setEditingTitle] = useState(properties.snippet.title === '')

	const changeEditingTitle = (target: boolean) => {
		if (!target && snippet.title === '') {
			changeDetails({title: 'None'})
		}
		setEditingTitle(target)
	}

	return (
		<Styled.Card>
			<Styled.CardHead{...properties.headProperties}>
				<MemoizedTitle
					editing={editingTitle}
					icon={<ExperimentOutlined/>}
					onChange={title => changeDetails({title})}
					onEditingChange={changeEditingTitle}
					text={snippet.title}
				/>
				<MemoizedExtras
					editingTitle={editingTitle}
					setEditingTitle={changeEditingTitle}
					delete={properties.delete}
					addComponent={properties.addComponent}
				/>
			</Styled.CardHead>
			<Styled.CardBody>
				<ComponentList
					snippetId={snippet.id}
					components={snippet.components}
					deleteComponent={deleteComponent}
				/>
				<OutputText content={''}/>
			</Styled.CardBody>
		</Styled.Card>
	)
}

export interface SnippetProperties {
	id: string
	position: SnippetPosition
	dragHandleProps?: any
}

export default function Snippet(properties: SnippetProperties) {
	const snippetContext = useSnippet(properties.id)
	if (!snippetContext.snippet) {
		return <></>
	}
	return <ExistingSnippet
		{...snippetContext}
		position={properties.position}
		headProperties={properties.dragHandleProps}
		snippet={snippetContext.snippet}
	/>
}
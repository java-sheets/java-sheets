import * as Styled from './Snippet.style'
import {Button, Dropdown, Menu, Space} from 'antd'
import {
	CheckOutlined, CommentOutlined,
	DeleteOutlined,
	DragOutlined,
	EditOutlined,
	ExperimentOutlined,
	FireOutlined, PlusOutlined,
	MoreOutlined,
	CodeOutlined
} from '@ant-design/icons'
import React, {useCallback, useMemo, useRef, useState} from 'react'
import {useTranslation} from 'react-i18next'
import Title from './Title'
import TextComponent from './TextComponent'
import EditorComponent from './EditorComponent'
import OutputText from './OutputText'
import {
	DragDropContext,
	Draggable,
	DraggableStateSnapshot, DraggingStyle,
	Droppable, DropResult, NotDraggingStyle
} from 'react-beautiful-dnd'
import {UseSnippet, useSnippet} from './useSheet'
import {useDispatch} from 'react-redux'
import {reorderComponent} from '../../sheet/state'
import {SheetSnippet, SheetSnippetComponent} from '../../sheet'

export interface SnippetPosition {
	highestOrder: number
	lowestOrder: number
	moveUp: () => void
	moveDown: () => void
}

export interface SnippetProperties {
	id: string
	position: SnippetPosition
	dragHandleProps?: any
}

function getStyle(style: DraggingStyle | NotDraggingStyle | undefined, snapshot: DraggableStateSnapshot) {
	if (style?.transform) {
		const axisLockY =
			'translate(0px' + style.transform.slice(style.transform.indexOf(','), style.transform.length)
		return {...style, transform: axisLockY}
	}
	return style
}

function useReorder(snippetId: string): (result: DropResult) => void {
	const dispatch = useDispatch()
	return React.useCallback(result => {
		if (!result.destination) {
			return
		}
		const from = result.source.index
		const to = result.destination.index
		dispatch(reorderComponent({snippetId, from, to}))
	}, [snippetId, dispatch])
}

export default function Snippet(properties: SnippetProperties) {
	const snippetContext = useSnippet(properties.id)
	if (!snippetContext.snippet) {
		return <></>
	}
	return <ExistingSnippet
		{...snippetContext}
		position={properties.position}
		dragHandleProps={properties.dragHandleProps}
		snippet={snippetContext.snippet}
	/>
}

interface ExistingSnippetProperties extends UseSnippet {
	snippet: SheetSnippet
	position: SnippetPosition
	dragHandleProps?: any
}

function ExistingSnippet(
	properties: ExistingSnippetProperties
) {
	const {snippet, deleteComponent, changeDetails} = properties
	const [editingTitle, setEditingTitle] = useState(properties.snippet.title == '')
	const onTitleEdited = () => {
		if (snippet.title == '') {
			changeDetails({title: 'None'})
		}
	}

	return (
		<Styled.Card>
			<Styled.CardHead
				{...properties.dragHandleProps}
			>
				<Title
					editing={editingTitle}
					icon={<ExperimentOutlined/>}
					onChange={title => changeDetails({title})}
					onEditingChange={target => {
						if (!target) {
							onTitleEdited()
						}
						setEditingTitle(target)
					}}
					text={snippet.title}
				/>
				<Styled.CardExtra>
					<Dropdown
						overlay={
							<Menu>
								<Menu.Item
									key="add-code"
									icon={<CodeOutlined />}
									onClick={() => properties.addComponent({type: 'code'})}
								>Code</Menu.Item>
								<Menu.Item
									key="add-text"
									icon={<CommentOutlined />}
									onClick={() => properties.addComponent({type: 'text'})}
								>Comment</Menu.Item>
							</Menu>
						}>
						<Button type="primary" ghost icon={<PlusOutlined/>}>Add</Button>
					</Dropdown>
					<Button
						type="primary"
						icon={<FireOutlined/>}
					>Run</Button>
					<Dropdown overlay={
						<Menu>
							<Menu.Item
								key="rename"
								icon={<EditOutlined/>}
								onClick={() => {
									if (editingTitle) {
										setEditingTitle(false)
										onTitleEdited()
									} else {
										setEditingTitle(true)
									}
								}}
							>Rename</Menu.Item>
							<Menu.Item
								key="delete"
								danger
								icon={<DeleteOutlined/>}
								onClick={() => properties.delete()}
							>
								Delete
							</Menu.Item>
						</Menu>
					}>
						<Button
							type="primary"
							ghost
							icon={<MoreOutlined/>}/>
					</Dropdown>
				</Styled.CardExtra>
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

interface ComponentListProperties {
	snippetId: string
	components: SheetSnippetComponent[]
	deleteComponent: (id: string) => void
}

type TimeoutId = ReturnType<typeof setTimeout>

function useTimedFlag(initial: boolean, timeout: number): [boolean, (target:
																																			 boolean) => void] {
	const active = useRef<{ active?: TimeoutId }>({active: undefined})
	const [state, setState] = useState(initial)
	const toggle = useCallback((target) => {
		if (target) {
			setState(true)
			active.current.active = setTimeout(() => {
				setState(false)
			}, timeout)
			return true
		} else {
			setState(false)
			const currentTimeout = active.current.active
			if (currentTimeout != undefined) {
				clearTimeout(currentTimeout)
			}
			return false
		}
	}, [active, timeout])
	return [state, toggle]
}

function ComponentList(properties: ComponentListProperties) {
	const reorder = useReorder(properties.snippetId)

	const components = useMemo(() => [...properties.components]
		.sort((left, right) => left.order - right.order)
		.map(component => {
				const content = component.type === 'text'
					? <TextComponent value={component.content}/>
					: <EditorComponent value={component.content}/>
				return {id: component.id, order: component.order, content}
			}
		), [properties.components])

	return (
		<DragDropContext onDragEnd={reorder}>
			<Droppable droppableId="droppable">
				{(provided) => (
					<div
						{...provided.droppableProps}
						ref={provided.innerRef}
					>
						{components.map((item) => (
							<ComponentContainer
								item={item}
								onDelete={() => properties.deleteComponent(item.id)}
							/>
						))}
						{provided.placeholder}
					</div>
				)}
			</Droppable>
		</DragDropContext>
	)
}

interface ComponentContainerProperties {
	item: { id: string, order: number, content: React.ReactNode },
	onDelete?: () => void
}

function ComponentContainer(
	{item, onDelete}: ComponentContainerProperties
) {
	const [confirmDelete, setConfirmDelete] = useTimedFlag(false, 2000)
	return (
		<Draggable key={item.id} draggableId={item.id} index={item.order}>
			{(provided, snapshot) => (
				<Styled.Component
					isDragging={snapshot.isDragging}
					ref={provided.innerRef}
					{...provided.draggableProps}
					style={getStyle(provided.draggableProps.style, snapshot)}
				>
					<Styled.ComponentOptions>
						<Styled.DeleteButton
							size="small"
							danger
							ghost
							icon={confirmDelete ? <CheckOutlined/> : <DeleteOutlined/>}
							onClick={() => {
								if (confirmDelete) {
									onDelete?.()
								} else {
									setConfirmDelete(true)
								}
							}}
						/>
						<Styled.DragHandle{...provided.dragHandleProps}>
							<DragOutlined/>
						</Styled.DragHandle>
					</Styled.ComponentOptions>
					<Styled.ComponentContent>
						{item.content}
					</Styled.ComponentContent>
				</Styled.Component>
			)}
		</Draggable>
	)
}
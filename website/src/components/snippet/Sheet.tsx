import * as Styled from './Sheet.style'
import Snippet, {SnippetPosition} from './Snippet'
import {useSheet} from './useSheet'
import React, {useMemo} from 'react'
import * as icons from '@ant-design/icons'
import {v4 as uuid} from 'uuid'
import {
	DragDropContext,
	Draggable,
	Droppable,
	DropResult
} from 'react-beautiful-dnd'
import {useDispatch} from 'react-redux'
import {reorderSnippet} from '../../sheet/state'

export interface SheetProperties {
}

const MemoizedSnippet = React.memo(Snippet)

function useReorder(): (result: DropResult) => void {
	const dispatch = useDispatch()
	return React.useCallback(result => {
		if (!result.destination) {
			return
		}
		const from = result.source.index
		const to = result.destination.index
		dispatch(reorderSnippet({from, to}))
	}, [dispatch])
}

export default function Sheet(_properties: SheetProperties) {
	const {sheet, addSnippet, moveSnippet} = useSheet()
	const reorder = useReorder()
	const snippets = useMemo(() => {
		const snippets = [...sheet.snippets]
		snippets.sort((left, right) => left.order - right.order)
		const lowestOrder = snippets[0]?.order
		const highestOrder = snippets[snippets.length - 1]?.order
		return {sorted: snippets, lowestOrder, highestOrder}
	}, [sheet.snippets.map(snippet => snippet.order)])
	return (
		<Styled.Sheet>
			<DragDropContext onDragEnd={reorder}>
				<Droppable droppableId="droppable">
					{(provided) => (
						<div
							{...provided.droppableProps}
							ref={provided.innerRef}
						>
							{snippets.sorted.map((snippet, index) => (
								<Draggable key={snippet.id} draggableId={snippet.id} index={snippet.order}>
									{(provided, snapshot) => (
										<Styled.SnippetContainer
											{...provided.draggableProps}
											ref={provided.innerRef}
										>
											<MemoizedSnippet
												dragHandleProps={provided.dragHandleProps}
												key={snippet.id}
												id={snippet.id}
												position={
													{
														lowestOrder: snippets.lowestOrder,
														highestOrder: snippets.highestOrder,
														moveUp: () => moveSnippet(snippet.id, 'up'),
														moveDown: () => moveSnippet(snippet.id, 'down'),
													}
												}
											/>
										</Styled.SnippetContainer>
									)}
								</Draggable>
							))}
							{provided.placeholder}
						</div>
					)}
				</Droppable>
			</DragDropContext>
			<Styled.AddButtonContainer>
				<Styled.AddButton
					icon={<icons.PlusOutlined/>}
					onClick={() => {
						addSnippet({
							components: [
								{
									id: uuid(),
									type: 'code',
									order: 0,
									content: '// TODO: Write Code'
								}
							]
						})
					}}
					type="primary"
					ghost
				/>
			</Styled.AddButtonContainer>
		</Styled.Sheet>
	)
}
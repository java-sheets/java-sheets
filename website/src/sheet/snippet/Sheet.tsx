import * as Styled from './Sheet.style'
import Snippet from './Snippet'
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
import {SheetSnippet} from "../index";
import {reorderSnippet} from "../state";
import {StartEvaluationRequest} from "@jsheets/protocol/src/jsheets/api/snippet_runtime_pb";

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

const newSnippetTemplate: () => Partial<SheetSnippet> = () => ({
	components: [
		{
			id: uuid(),
			type: 'code',
			order: 0,
			content: '// TODO: Write Code'
		}
	]
})

export interface SheetProperties {
  onRun: (request: StartEvaluationRequest) => void
}

export default function Sheet(properties: SheetProperties) {
	const {sheet, addSnippet, moveSnippet} = useSheet()
	const reorder = useReorder()
	const snippets = useMemo(() => {
		const snippets = [...sheet.snippets]
		snippets.sort((left, right) => left.order - right.order)
		const lowestOrder = snippets[0]?.order
		const highestOrder = snippets[snippets.length - 1]?.order
		return {sorted: snippets, lowestOrder, highestOrder}
	}, [sheet.snippets])
	return (
		<Styled.Sheet>
			<DragDropContext onDragEnd={reorder}>
				<Droppable droppableId="sheets">
					{droppable => (
						<div
							{...droppable.droppableProps}
							ref={droppable.innerRef}
						>
							{snippets.sorted.map((snippet) => (
								<Draggable key={snippet.id} draggableId={snippet.id} index={snippet.order}>
									{(draggable) => (
										<Styled.SnippetContainer
											{...draggable.draggableProps}
											ref={draggable.innerRef}
										>
											<MemoizedSnippet
												key={snippet.id}
                        sheetId={sheet.id}
                        onRun={properties.onRun}
												dragHandleProps={draggable.dragHandleProps}
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
							{droppable.placeholder}
						</div>
					)}
				</Droppable>
			</DragDropContext>
			<Styled.AddButtonContainer>
				<Styled.AddButton
					icon={<icons.PlusOutlined/>}
					type="primary"
					ghost
					onClick={() => addSnippet(newSnippetTemplate())}
				/>
			</Styled.AddButtonContainer>
		</Styled.Sheet>
	)
}
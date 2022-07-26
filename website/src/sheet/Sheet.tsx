import * as Styled from './Sheet.style'
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
import {StartEvaluationRequest} from "@jsheets/protocol/src/jsheets/api/snippet_runtime_pb";
import {reorderSnippet} from './state'
import {listSnippetsInState, SheetState, SnippetState} from './index'
import SnippetContext from './snippet/Snippet'
import {useDraggableIds} from './snippet/draggableId'
import {SnippetReference} from './snippet/SnippetX'

const MemoizedSnippet = React.memo(SnippetContext)

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

function newSnippetTemplate(): Partial<SnippetState> {
  const componentId = uuid()
  return {
    components: {
      [componentId]: {
        id: componentId,
        type: 'code',
        order: 0,
        content: '// TODO: Write Code'
      }
    }
  }
}

export type CaptureSnippetReference = (id: string, snippet: SnippetReference) => void

export interface SheetProperties {
  initial?: SheetState
  evaluating?: boolean
  isCooldown?: boolean
  evaluate: (request: StartEvaluationRequest) => void
  captureSnippet?: CaptureSnippetReference
}

export default function Sheet(properties: SheetProperties) {
  const {sheet, addSnippet, moveSnippet} = useSheet(properties.initial)
  const reorder = useReorder()

  const snippets = useMemo(() => {
    const snippets = listSnippetsInState(sheet)
    snippets.sort((left, right) => left.order - right.order)
    const lowestOrder = snippets[0]?.order
    const highestOrder = snippets[snippets.length - 1]?.order
    return {sorted: snippets, lowestOrder, highestOrder}
  }, [sheet.snippets])

  const [associateDraggableId] = useDraggableIds()

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
                <Draggable key={snippet.id} draggableId={`${associateDraggableId(snippet.id)}`} index={snippet.order}>
                  {(draggable) => (
                    <Styled.SnippetContainer
                      {...draggable.draggableProps}
                      ref={draggable.innerRef}
                    >
                      <MemoizedSnippet
                        key={snippet.id}
                        isCooldown={properties.isCooldown}
                        running={properties.evaluating}
                        sheetId={sheet.id}
                        evaluate={properties.evaluate}
                        dragHandleProps={draggable.dragHandleProps}
                        id={snippet.id}
                        capture={reference => properties.captureSnippet?.(snippet.id, reference)}
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
import React, {useCallback} from 'react'
import {
  Draggable,
  DraggingStyle,
  NotDraggingStyle
} from "react-beautiful-dnd"
import * as Styled from "./ComponentContainer.style"
import {
  CheckOutlined,
  DeleteOutlined,
  DragOutlined
} from '@ant-design/icons'
import useTimedFlag from "../../../util/useTimedFlag"
import {SheetSnippetComponentOutput} from "../../index";
import OutputText from "./OutputText";
import {useDispatch} from "react-redux";
import {removeOutput} from "../../state";

function fixToVerticalAxis(style: DraggingStyle | NotDraggingStyle | undefined) {
  if (style?.transform) {
    const otherPosition = style.transform.slice(
      style.transform.indexOf(','),
      style.transform.length
    )
    return {
      ...style,
      transform: `translate(0px${otherPosition}`
    }
  }
  return style
}

export interface ComponentNode {
  id: string
  order: number
  node: React.ReactNode
  output?: SheetSnippetComponentOutput[]
}

interface ComponentContainerProperties {
  item: ComponentNode
  onDelete?: () => void
}

export default function ComponentContainer(properties: ComponentContainerProperties) {
  const dispatch =  useDispatch()
  const [confirmDelete, setConfirmDelete] = useTimedFlag(false, 2000)
  const {item} = properties

  const onOutputClose = useCallback(() => {
    dispatch(removeOutput({componentId: item.id}))
  }, [dispatch, item])

  const outputs = React.useMemo(() =>
    item.output?.map((output, index) => (
      <OutputText
        key={index}
        output={output}
        onClose={onOutputClose}
      />
    )),
    [item.output, onOutputClose]
  )

  return (
    <Draggable key={item.id} draggableId={item.id} index={item.order}>
      {(provided, snapshot) => (
        <Styled.Component
          className={snapshot.isDragging ? 'dragging-component' : ''}
          key={item.id}
          ref={provided.innerRef}
          {...provided.draggableProps}
          style={fixToVerticalAxis(provided.draggableProps.style)}
        >
          <Styled.ComponentInputArea>
            <Styled.ComponentOptions>
              <Styled.DeleteButton
                size="small"
                danger
                ghost
                icon={confirmDelete ? <CheckOutlined/> : <DeleteOutlined/>}
                onClick={() => {
                  if (confirmDelete) {
                    properties.onDelete?.()
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
              {properties.item.node}
            </Styled.ComponentContent>
          </Styled.ComponentInputArea>
          {outputs}
        </Styled.Component>
      )}
    </Draggable>
  )
}
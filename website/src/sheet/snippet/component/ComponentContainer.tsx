import React from "react"
import {
	Draggable,
	DraggingStyle,
	NotDraggingStyle
} from "react-beautiful-dnd"
import * as Styled from "./ComponentContainer.style"
import {CheckOutlined, DeleteOutlined, DragOutlined} from "@ant-design/icons"
import useTimedFlag from "../../../util/useTimedFlag"
import {SheetSnippetComponentOutput} from "../../index";
import OutputText from "./OutputText";

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

interface ComponentContainerProperties {
	item: {
	  id: string
    order: number
    output: SheetSnippetComponentOutput | undefined
    content: React.ReactNode
  }
	onDelete?: () => void
  output?: SheetSnippetComponentOutput
}

export default function ComponentContainer(
	{item, onDelete}: ComponentContainerProperties
) {
	const [confirmDelete, setConfirmDelete] = useTimedFlag(false, 2000)
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
          </Styled.ComponentInputArea>
          <OutputText output={item.output}/>
				</Styled.Component>
			)}
		</Draggable>
	)
}
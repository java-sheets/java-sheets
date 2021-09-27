import React, {useMemo} from "react";
import {DragDropContext, Droppable, DropResult} from "react-beautiful-dnd";
import ComponentContainer, {ComponentNode} from './ComponentContainer'
import TextComponent from "./TextComponent";
import EditorComponent from "./EditorComponent";
import {useDispatch} from "react-redux";
import {reorderComponent} from "../../state";
import {SheetSnippetComponent} from "../../index";
import {SnippetComponentReference} from './reference'
import {useDraggableId, useDraggableIds} from '../draggableId'

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

type CaptureComponent = (id: string, reference: SnippetComponentReference) => void

export interface ComponentListProperties {
  snippetId: string
  components: SheetSnippetComponent[]
  deleteComponent: (id: string) => void
  capture?: CaptureComponent
}

function useComponents(
  components: SheetSnippetComponent[],
  captureComponent?: CaptureComponent
): ComponentNode[] {
  return useMemo(() => [...components]
    .sort((left, right) => left.order - right.order)
    .map(component => {
      const {id, type, content} = component
      const capture = (reference: SnippetComponentReference) => captureComponent?.(id, reference)
      const Component = type === 'text' ? TextComponent : EditorComponent
      const node = <Component capture={capture} id={id} value={content}/>
      return {...component, node}
    }),
    [captureComponent, components]
  )
}

export default function ComponentList(properties: ComponentListProperties) {
  const reorder = useReorder(properties.snippetId)
  const components = useComponents(properties.components, properties.capture)
  const draggableId = useDraggableId(properties.snippetId)
  return (
    <DragDropContext onDragEnd={reorder}>
      <Droppable droppableId={`components-${draggableId}`}>
        {(droppable) => (
          <div ref={droppable.innerRef}{...droppable.droppableProps}>
            {components.map((item) => (
              <ComponentContainer
                key={item.id}
                item={item}
                onDelete={() => properties.deleteComponent(item.id)}
              />
            ))}
            {droppable.placeholder}
          </div>
        )}
      </Droppable>
    </DragDropContext>
  )
}

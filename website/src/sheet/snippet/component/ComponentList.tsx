import React, {MutableRefObject, useMemo} from "react";
import {DragDropContext, Droppable, DropResult} from "react-beautiful-dnd";
import ComponentContainer from "./ComponentContainer";
import TextComponent from "./TextComponent";
import EditorComponent from "./EditorComponent";
import {useDispatch} from "react-redux";
import {reorderComponent} from "../../state";
import {SheetSnippetComponent} from "../../index";
import {SnippetComponentListRef} from "./reference";

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

export interface ComponentListProperties {
  snippetId: string
  components: SheetSnippetComponent[]
  deleteComponent: (id: string) => void
  listRef?: MutableRefObject<SnippetComponentListRef | null>
}

export default function ComponentList(properties: ComponentListProperties) {
  const reorder = useReorder(properties.snippetId)

  const components = useMemo(() => [...properties.components]
    .sort((left, right) => left.order - right.order)
    .map(component => {
        const content = component.type === 'text'
          ? <TextComponent listRef={properties.listRef} id={component.id} value={component.content}/>
          : <EditorComponent listRef={properties.listRef} id={component.id} value={component.content}/>
        return {id: component.id, order: component.order, output: component.output, content}
      }
    ), [properties.listRef, properties.components])

  return (
    <DragDropContext onDragEnd={reorder}>
      <Droppable droppableId="components">
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

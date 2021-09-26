import {useDispatch, useSelector} from 'react-redux'
import {Sheet, SheetSnippet, SheetSnippetComponent} from './index'
import {RootState} from '../store'
import {
  addComponent,
  addSnippet,
  changeSnippetDetails, deleteComponent,
  deleteSnippet,
  moveSnippet, reorderComponent,
  reorderSnippet
} from './state'

export interface UseSheet {
  sheet: Sheet
  moveSnippet: (id: string, direction: 'up' | 'down') => void
  reorderSnippet: (from: number, to: number) => void
  addSnippet: (snippet: Partial<SheetSnippet>) => void
  deleteSnippet: (id: string) => void
}

export function useSheet(): UseSheet {
  const dispatch = useDispatch()
  const sheet = useSelector((state: RootState) => state.sheet)
  return {
    sheet,
    moveSnippet: (id, direction) => dispatch(moveSnippet({id, direction})),
    reorderSnippet: (from, to) => dispatch(reorderSnippet({from, to})),
    addSnippet: (snippet) => dispatch(addSnippet(snippet)),
    deleteSnippet: (id: string) => dispatch(deleteSnippet({id}))
  }
}

export interface UseSnippet {
  snippet: SheetSnippet | undefined
  changeDetails: (details: {title: string}) => void
  reorderComponent: (from: number, to: number) => void
  addComponent: (component: Partial<SheetSnippetComponent>) => void
  deleteComponent: (id: string) => void
  delete: () => void
}

export function useSnippet(id: string): UseSnippet {
  const dispatch = useDispatch()
  const snippet = useSelector(
    (state: RootState) => state.sheet.snippets.find(snippet => snippet.id === id)
  )
  return {
    snippet,
    delete: () => dispatch(deleteSnippet({id})),
    changeDetails: details => dispatch(changeSnippetDetails({snippetId: id, ...details})),
    reorderComponent: (from, to) => dispatch(reorderComponent({snippetId: id, from, to})),
    addComponent: component => dispatch(addComponent({snippetId: id, component})),
    deleteComponent: componentId => dispatch(deleteComponent({snippetId: id, componentId}))
  }
}

export function useComponent(
  snippetId: string,
  componentId: string
): SheetSnippetComponent | undefined {
  return useSelector(
    (state: RootState) =>
      state.sheet.snippets.find(snippet => snippet.id === snippetId)
        ?.components.find(component => component.id === componentId)
  )
}
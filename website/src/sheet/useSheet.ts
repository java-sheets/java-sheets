import {useDispatch, useSelector} from 'react-redux'
import {
  SheetState,
  SnippetState,
  ComponentState,
  findSnippetByIdInState, findComponentByIdInState
} from './index'
import {RootState} from '../store'
import {
  addComponent,
  addSnippet,
  changeSnippetDetails, deleteComponent,
  deleteSnippet,
  moveSnippet, reorderComponent,
  reorderSnippet, updateSheet
} from './state'

export interface UseSheet {
  sheet: SheetState
  update: (target: SheetState) => void
  moveSnippet: (id: string, direction: 'up' | 'down') => void
  reorderSnippet: (from: number, to: number) => void
  addSnippet: (snippet: Partial<SnippetState>) => void
  deleteSnippet: (id: string) => void
}

export function useSheet(): UseSheet {
  const dispatch = useDispatch()
  const sheet = useSelector((state: RootState) => state.sheet)
  return {
    sheet,
    update: sheet => dispatch(updateSheet(sheet)),
    moveSnippet: (id, direction) => dispatch(moveSnippet({id, direction})),
    reorderSnippet: (from, to) => dispatch(reorderSnippet({from, to})),
    addSnippet: (snippet) => dispatch(addSnippet(snippet)),
    deleteSnippet: (id: string) => dispatch(deleteSnippet({id}))
  }
}

export interface UseSnippet {
  snippet: SnippetState | undefined
  changeDetails: (details: {title: string}) => void
  reorderComponent: (from: number, to: number) => void
  addComponent: (component: Partial<ComponentState>) => void
  deleteComponent: (id: string) => void
  delete: () => void
}

export function useSnippet(id: string): UseSnippet {
  const dispatch = useDispatch()
  const snippet = useSelector(
    (state: RootState) => findSnippetByIdInState(state.sheet, id)
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
): ComponentState | undefined {
  return useSelector(
    (state: RootState) => {
      const snippet = findSnippetByIdInState(state.sheet, snippetId)
      return snippet ? findComponentByIdInState(snippet, componentId) : undefined
    }
  )
}
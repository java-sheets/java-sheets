import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import {
  SheetState,
  SnippetState,
  ComponentState,
  SnippetComponentOutput,
  listSnippetsInState,
  listComponentsInState,
  findSnippetByIdInState,
  insertSnippetToState,
  findSnippetInState,
  removeComponentFromState,
  insertComponentToState,
  findComponentInState, removeSnippetFromState
} from './index'
import { v4 as uuid } from 'uuid'

const initialState: SheetState = {id: uuid(), title: '', description: '', snippets: {}}

const slice = createSlice({
  name: "sheet",
  initialState,
  reducers: {
    updateSheet(state, action: PayloadAction<SheetState>) {
      return action.payload
    },
    changeDetails(state, action: PayloadAction<{title?: string, description?: string}>) {
      const {title, description} = action.payload
      if (title !== undefined) {
        state.title = title
      }
      if (description !== undefined) {
        state.description = description
      }
    },
    changeSnippetDetails(state, action: PayloadAction<{snippetId: string, title?: string}>) {
      const {snippetId, title} = action.payload
      const snippet = findSnippetByIdInState(state, snippetId)
      if (snippet && title !== undefined) {
        snippet.title = title
      }
    },
    addSnippet(state, action: PayloadAction<Partial<SnippetState>>) {
      const id = action.payload.id || uuid()
      insertSnippetToState(state, {
        title: '',
        components: {},
        order: findLastOrder(listSnippetsInState(state)),
        ...action.payload,
        id: id
      });
    },
    deleteSnippet(state, action: PayloadAction<Partial<{id: string}>>) {
      if (action.payload.id) {
        removeSnippetFromState(state, action.payload.id)
      }
    },
    moveSnippet(state, action: PayloadAction<{id: string, direction: 'up' | 'down'}>) {
      const {id ,direction} = action.payload
      const snippet = findSnippetByIdInState(state, id)
      if (!snippet) {
        return
      }
      const target = direction === 'down'
        ? findNextInOrder(snippet.order, listSnippetsInState(state))
        : findLastInOrder(snippet.order, listSnippetsInState(state))
      if (target) {
        const snippetOrder = snippet.order
        snippet.order = target.order
        target.order = snippetOrder
      }
    },
    reorderSnippet(state, action: PayloadAction<{from: number, to: number}>) {
      const {from, to} = action.payload
      const current = findSnippetInState(state, snippet => snippet.order == from)
      const target = findSnippetInState(state, snippet => snippet.order == to)
      if (current && target) {
        current.order = to
        target.order = from
      }
    },
    deleteComponent(state, action: PayloadAction<{snippetId: string, componentId: string}>) {
      const {snippetId, componentId} = action.payload
      const snippet = findSnippetByIdInState(state, snippetId)
      if (!snippet) {
        return
      }
      removeComponentFromState(snippet, componentId)
    },
    addComponent(state, action: PayloadAction<{snippetId: string, component: Partial<ComponentState>}>) {
      const {snippetId, component} = action.payload
      const snippet = findSnippetByIdInState(state, snippetId)
      if (!snippet) {
        return
      }
      const id = component.id || uuid()
      insertComponentToState(snippet, {
        order: findLastOrder(listComponentsInState(snippet)),
        type: 'code',
        content: '',
        ...component,
        id: id,
      })
    },
    reorderComponent(state, action: PayloadAction<ReorderComponent>) {
      const {snippetId, from, to} = action.payload
      const snippet = findSnippetByIdInState(state, snippetId)
      if (!snippet) {
        return
      }
      const current = findComponentInState(snippet,component => component.order == from)
      if (!current) {
        return
      }
      if (current.order >= to) {
        addRightGap(to, listComponentsInState(snippet))
      } else {
        addLeftGap(to, listComponentsInState(snippet))
      }
      current.order = to
    },
    removeOutput(state, action: PayloadAction<RemoveOutput>) {
      const {snippetId, componentId} = action.payload

      const findComponents = (): ComponentState[] => {
        if (snippetId) {
          const snippet = findSnippetByIdInState(state, snippetId)
          return snippet ? listComponentsInState(snippet) : []
        }
        if (action.payload.componentId) {
          return listSnippetsInState(state).flatMap(snippet =>
              listComponentsInState(snippet)
                .filter(component => component.id === componentId)
            )
        }
        return listSnippetsInState(state)
          .flatMap(snippet => listComponentsInState(snippet))
      }

      findComponents()?.forEach(component => component.output = undefined)
    },
    reportOutput(state, action: PayloadAction<ReportOutput>) {
      listSnippetsInState(state).forEach(snippet => {
        listComponentsInState(snippet)
          .filter(component => component.id === action.payload.componentId)
          .forEach(component => {
            if (component.output) {
              component.output.push(action.payload.output)
            } else {
              component.output = [action.payload.output]
            }
          })
      })
    }
  }
})

interface ReportOutput {
  output: SnippetComponentOutput
  componentId: string
}

interface RemoveOutput {
  snippetId?: string
  componentId?: string
}

function addRightGap(start: number, elements: {order: number}[]) {
  for (const component of elements) {
    if (component.order >= start) {
      component.order++
    }
  }
}

function addLeftGap(start: number, elements: {order: number}[]) {
  for (const component of elements) {
    if (component.order <= start) {
      component.order--
    }
  }
}

export interface ReorderComponent {
  snippetId: string
  from: number
  to: number
}

function findNextInOrder<T extends {order: number}>(
  current: number, elements: T[]): T | undefined {

  const index = findIndexSorted(current, elements)
  return index ? elements[index + 1] : undefined
}

function findLastInOrder<T extends {order: number}>(
  current: number, elements: T[]): T | undefined {

  const index = findIndexSorted(current, elements)
  return index ? elements[index - 1] : undefined
}

function findIndexSorted<T extends {order: number}>(
  current: number, elements: T[]): number | undefined {

  return elements.sort((left, right) => left.order - right.order)
    .findIndex(element => element.order === current)
}

function findLastOrder(values: {order: number}[]): number {
  let highest = 0
  for (const value of values) {
    if (value.order > highest) {
      highest = value.order
    }
  }
  return highest + 1
}

export const {
  updateSheet,
  addSnippet,
  deleteSnippet,
  moveSnippet,
  reorderSnippet,
  changeDetails,
  reorderComponent,
  addComponent,
  reportOutput,
  deleteComponent,
  removeOutput,
  changeSnippetDetails
} = slice.actions

export default slice.reducer

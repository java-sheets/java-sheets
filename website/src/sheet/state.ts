import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import {
  Sheet,
  SheetSnippet,
  SheetSnippetComponent,
  SheetSnippetComponentOutput
} from './index'
import { v4 as uuid } from 'uuid'

const initialState: Sheet = {
  id: uuid(),
  title: 'Welcome!',
  description: '',
  snippets: [
    {
      id: uuid(),
      title: 'Welcome!',
      order: 0,
      components: [
        {
          id: uuid(),
          order: 0,
          content: `Welcome to the JSheets Demo`,
          type: 'text'
        },
        {
          id: uuid(),
          order: 1,
          content: `Math.min(420, 1337)`,
          type: 'code'
        }
      ]
    }
  ]
}

const slice = createSlice({
  name: "sheet",
  initialState,
  reducers: {
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
      const snippet = state.snippets.find(snippet => snippet.id === snippetId)
      if (snippet && title !== undefined) {
        snippet.title = title
      }
    },
    addSnippet(state, action: PayloadAction<Partial<SheetSnippet>>) {
      const id = action.payload.id || uuid()
      state.snippets.push({
        title: '',
        components: [],
        order: findLastOrder(state.snippets.values()),
        ...action.payload,
        id: id
      });
    },
    deleteSnippet(state, action: PayloadAction<Partial<{id: string}>>) {
      if (action.payload.id) {
        const index = state.snippets.findIndex(snippet => snippet.id === action.payload.id)
        if (index >= 0) {
          state.snippets.splice(index, 1)
        }
      }
    },
    moveSnippet(state, action: PayloadAction<{id: string, direction: 'up' | 'down'}>) {
      const {id ,direction} = action.payload
      const snippet = state.snippets.find(snippet => snippet.id === id)
      if (!snippet) {
        return
      }
      const target = direction === 'down'
        ? findNextInOrder(snippet.order, state.snippets)
        : findLastInOrder(snippet.order, state.snippets)
      if (target) {
        const snippetOrder = snippet.order
        snippet.order = target.order
        target.order = snippetOrder
      }
    },
    reorderSnippet(state, action: PayloadAction<{from: number, to: number}>) {
      const {from, to} = action.payload
      const current = state.snippets.find(snippet => snippet.order === from)
      const target = state.snippets.find(snippet => snippet.order === to)
      if (current && target) {
        current.order = to
        target.order = from
      }
    },
    deleteComponent(state, action: PayloadAction<{snippetId: string, componentId: string}>) {
      const {snippetId, componentId} = action.payload
      const snippet = state.snippets.find(snippet => snippet.id === snippetId)
      if (!snippet) {
        return
      }
      const index = snippet.components.findIndex(component => component.id === componentId)
      if (index >= 0) {
        snippet.components.splice(index, 1)
      }
    },
    addComponent(state, action: PayloadAction<{snippetId: string, component: Partial<SheetSnippetComponent>}>) {
      const {snippetId, component} = action.payload
      const snippet = state.snippets.find(snippet => snippet.id === snippetId)
      if (!snippet) {
        return
      }
      const id = component.id || uuid()
      snippet.components.push({
        order: findLastOrder(snippet.components.values()),
        type: 'code',
        content: '',
        ...component,
        id: id,
      });
    },
    reorderComponent(state, action: PayloadAction<ReorderComponent>) {
      const {snippetId, from, to} = action.payload
      const snippet = state.snippets.find(snippet => snippet.id === snippetId)
      if (!snippet) {
        return
      }
      const current = snippet.components.find(component => component.order === from)
      if (!current) {
        return
      }
      if (current.order >= to) {
        addRightGap(to, snippet.components)
      } else {
        addLeftGap(to, snippet.components)
      }
      current.order = to
    },
    removeOutput(state, action: PayloadAction<RemoveOutput>) {
      const {snippetId, componentId} = action.payload

      const findComponents = () => {
        if (action.payload.snippetId) {
          return state.snippets.find(snippet => snippet.id === snippetId)?.components
        }
        if (action.payload.componentId) {
          return state.snippets.flatMap(snippet =>
            snippet.components.filter(component => component.id === componentId)
          )
        }
        return state.snippets.flatMap(snippet => snippet.components)
      }

      findComponents()?.forEach(component => component.output = undefined)
    },
    reportOutput(state, action: PayloadAction<ReportOutput>) {
      state.snippets.forEach(snippet => {
        snippet.components
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
  output: SheetSnippetComponentOutput
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

function findNextInOrder<T extends {order: number}>(current: number, elements: T[]): T | undefined {
  const index = findIndexSorted(current, elements)
  return index ? elements[index + 1] : undefined
}

function findLastInOrder<T extends {order: number}>(current: number, elements: T[]): T | undefined {
  const index = findIndexSorted(current, elements)
  return index ? elements[index - 1] : undefined
}

function findIndexSorted<T extends {order: number}>(current: number, elements: T[]): number | undefined {
  return elements.sort((left, right) => left.order - right.order)
    .findIndex(element => element.order === current)
}


function findLastOrder(values: IterableIterator<{order: number}>): number {
  let highest = 0
  for (const value of values) {
    if (value.order > highest) {
      highest = value.order
    }
  }
  return highest + 1
}

export const {
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

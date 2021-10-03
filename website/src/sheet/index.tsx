export interface SheetState {
  id: string
  title: string
  description: string
  snippets: Record<string, SnippetState>
}

export function insertSnippetToState(state: SheetState, snippet: SnippetState) {
  state.snippets[snippet.id] = snippet
}

export function removeSnippetFromState(state: SheetState, id: string) {
  delete state.snippets[id]
}

export function listSnippetsInState(state: SheetState): SnippetState[] {
  return Object.values(state.snippets)
}

export function findSnippetByIdInState(
  state: SheetState,
  id: string
): SnippetState | undefined {
  return state.snippets[id]
}

export function findSnippetIndexByIdInState(
  state: SheetState,
  id: string
): number {
  return Object.values(state.snippets)
    .findIndex(state => state.id === id)
}

export function findSnippetInState(
  state: SheetState,
  filter: (state: SnippetState) => boolean
): SnippetState | undefined {
  return Object.values(state.snippets).find(filter)
}

export interface SnippetState {
  id: string
  order: number
  title: string
  components: Record<string, ComponentState>
}

export function insertComponentToState(state: SnippetState, component: ComponentState) {
  state.components[component.id] = component
}

export function removeComponentFromState(state: SnippetState, id: string) {
  delete state.components[id]
}

export function listComponentsInState(state: SnippetState): ComponentState[] {
  return Object.values(state.components)
}

export function findComponentByIdInState(
  state: SnippetState,
  id: string
): ComponentState | undefined {
  return state.components[id]
}

export function findComponentInState(
  state: SnippetState,
  filter: (state: ComponentState) => boolean
): ComponentState | undefined {
  return Object.values(state.components).find(filter)
}

export interface ComponentState {
  id: string
  order: number
  type: 'code' | 'text'
  content: string
  output?: SnippetComponentOutput[]
}

export type SnippetComponentOutput = ErrorOutput | MessageOutput

export interface ErrorOutput {
  span: {
    start: number
    end: number
  }
  code: string
  message: string
}

export interface MessageOutput {
  type: "error" | "info"
  message: string
}
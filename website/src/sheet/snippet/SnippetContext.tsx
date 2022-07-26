import {ComponentState, SnippetState} from '../index'
import React from 'react'

export interface SnippetContextProps {
  snippet: SnippetState
  addComponent?: (component: Partial<ComponentState>) => void
  removeComponent?: (id: string) => void
  setEditingTitle?: (state: boolean) => void
  changeTitle?: (target: string) => void
}

export default React.createContext<SnippetContextProps>(
  {snippet: {id: '', title: '', components: {}, order: 0}})
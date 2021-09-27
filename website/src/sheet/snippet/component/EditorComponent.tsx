import * as Styled from './EditorComponent.style'
import React, {MutableRefObject} from 'react'
import Editor from '../../../editor/Editor'
import {EditorView} from '@codemirror/view'
import {EditorState} from '@codemirror/state'
import * as SnippetProtocol from '@jsheets/protocol/src/jsheets/api/snippet_pb'
import {SnippetComponentListRef, SnippetComponentRef} from './reference'

export interface EditorComponentProperties {
  value: string
  id: string
  listRef?: MutableRefObject<SnippetComponentListRef | null>
}

export default class EditorComponent
  extends React.Component<EditorComponentProperties>
  implements SnippetComponentRef {

  private editorReference = React.createRef<EditorView | null>()

  render() {
    return (
      <Styled.EditorComponent>
        <Editor editorRef={this.editorReference} code={this.props.value}/>
      </Styled.EditorComponent>
    )
  }
  componentDidMount() {
    this.props.listRef?.current?.components.set(this.props.id, this)
  }

  shouldComponentUpdate(
    nextProps: Readonly<EditorComponentProperties>,
    nextState: Readonly<{}>,
    nextContext: any
  ): boolean {
    return nextProps.value !== this.props.value
  }

  serialize = (): SnippetProtocol.Snippet.Component =>{
    const component = new SnippetProtocol.Snippet.Component()
    component.setId(this.props.id)
    component.setKind(SnippetProtocol.Snippet.Component.Kind.CODE)
    const content = this.content()
    if (content) {
      component.setContent(content)
    }
    return component
  }

  content = (): string | null => {
    return this.editorReference.current?.state.doc.sliceString(0) || null
  }

  updateContent = (target: string) => {
    this.editorReference.current?.setState(EditorState.create({doc: target}))
  }
}
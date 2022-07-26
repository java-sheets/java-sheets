import * as Styled from './TextComponent.style'
import React from 'react'
import {EditorComponentProperties} from './EditorComponent'
import {SnippetComponentReference} from './reference'
import RichMarkdownEditor from 'rich-markdown-editor'
import * as SnippetProtocol from '@jsheets/protocol/src/jsheets/api/snippet_pb'

export interface TextComponentProperties {
  value: string
  id: string
  capture?: (reference: SnippetComponentReference) => void
}

// This is a hack: The accepted type of placeholder names is insufficient.
// @ts-ignore
const disabledExtensions: 'placeholder'[] = ['empty-placeholder', 'placeholder', 'blockmenu']

export default class TextComponent
  extends React.Component<TextComponentProperties>
  implements SnippetComponentReference {

  private readonly editorRef = React.createRef<RichMarkdownEditor>()

  render() {
    return (
      <Styled.TextComponent>
        <Styled.GlobalMarkdown/>
        <Styled.Editor
          ref={this.editorRef}
          disableExtensions={disabledExtensions}
          placeholder={''}
          defaultValue={this.props.value}
          dark={this.context.theme === 'dark'}
        />
      </Styled.TextComponent>
    )
  }

  componentDidMount() {
    this.props.capture?.(this)
  }

  content = (): string | null => {
    return this.editorRef.current?.value() || null
  }

  serialize = (): SnippetProtocol.Snippet.Component => {
    const component = new SnippetProtocol.Snippet.Component()
    component.setId(this.props.id)
    component.setKind(SnippetProtocol.Snippet.Component.Kind.TEXT)
    const content = this.content()
    if (content) {
      component.setContent(content)
    }
    return component
  }

  updateContent = (target: string) => {}

  shouldComponentUpdate(
    nextProps: Readonly<EditorComponentProperties>,
    nextState: Readonly<{}>,
    nextContext: any
  ): boolean {
    return nextProps.id !== this.props.id || nextProps.value !== this.props.value
  }
}

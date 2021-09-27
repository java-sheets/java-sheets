import * as Styled from './TextComponent.style'
import {createGlobalStyle} from 'styled-components'
import React, {MutableRefObject} from 'react'
import {EditorComponentProperties} from './EditorComponent'
import {SnippetComponentListRef, SnippetComponentRef} from './reference'
import RichMarkdownEditor from 'rich-markdown-editor'
import {ThemeContext} from '../../../theme/ThemeContext'
import * as SnippetProtocol
  from '../../../../../protocol/generated/js-protocol/src/jsheets/api/snippet_pb'

export interface TextComponentProperties {
  value: string
  id: string
  listRef?: MutableRefObject<SnippetComponentListRef | null>
}

const MarkdownEditorStyle = createGlobalStyle`
  #block-menu-container, .heading-actions, .block-menu-trigger {
    display: none !important;
  }
`

// @ts-ignore
const disabledExtensions: 'placeholder'[] = ['empty-placeholder', 'placeholder', 'blockmenu']

export default class TextComponent
  extends React.Component<TextComponentProperties>
  implements SnippetComponentRef {

  static contextType = ThemeContext

  private readonly editorRef = React.createRef<RichMarkdownEditor>()

  render() {
    return (
      <Styled.TextComponent>
        <MarkdownEditorStyle/>
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
    this.props.listRef?.current?.components.set(this.props.id, this)
  }

  content = (): string | null => {
    return this.editorRef.current?.value() || null
  }

  serialize = (): SnippetProtocol.Snippet.Component =>{
    const component = new SnippetProtocol.Snippet.Component()
    component.setId(this.props.id)
    component.setKind(SnippetProtocol.Snippet.Component.Kind.TEXT)
    const content = this.content()
    if (content) {
      component.setContent(content)
    }
    return component
  }

  updateContent = (target: string) => { }

  shouldComponentUpdate(
    nextProps: Readonly<EditorComponentProperties>,
    nextState: Readonly<{}>,
    nextContext: any
  ): boolean {
    return nextProps.value !== this.props.value
  }
}

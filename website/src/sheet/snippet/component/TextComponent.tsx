import * as Styled from './TextComponent.style'
import {createGlobalStyle} from 'styled-components'
import React, {MutableRefObject} from "react";
import {EditorComponentProperties} from "./EditorComponent";
import {SnippetComponentListRef, SnippetComponentRef} from "./Component";
import RichMarkdownEditor from "rich-markdown-editor";

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

  updateContent = (target: string) => { }

  shouldComponentUpdate(
    nextProps: Readonly<EditorComponentProperties>,
    nextState: Readonly<{}>,
    nextContext: any
  ): boolean {
    return nextProps.value !== this.props.value
  }
}

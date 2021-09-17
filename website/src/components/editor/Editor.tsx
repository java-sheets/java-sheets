import React, {useEffect} from 'react'
import {EditorView} from '@codemirror/view'
import {EditorState} from '@codemirror/state'
import {basicSetup} from '@codemirror/basic-setup'
import {javaLanguage} from '@codemirror/lang-java'
import {tags, HighlightStyle} from '@codemirror/highlight'

export interface EditorProperties {
  code: string
  onMount?: (editor: EditorView) => void
}

const theme = EditorView.theme({
  ".cm-scroller": {
    fontFamily: `'JetBrains Mono', Menlo, Monaco, source-code-pro, Consolas, monospace`
  },
  "&": {
    color: "#24292e",
    backgroundColor: "#f6f8fa",
    padding: "10px 0"
  },
  ".cm-activeLineGutter, .cm-activeLine": {
    backgroundColor: "#f8f8f8"
  },
  ".cm-content": {
    caretColor: "#424242"
  },
  "&.cm-focused .cm-cursor": {
    borderLeftColor: "#b6b6b6"
  },
  "&.cm-focused .cm-selectionBackground, ::selection": {
    backgroundColor: "#a2a2a2"
  },
  ".cm-gutters": {
    backgroundColor: "#f6f8fa",
    color: "#ddd",
    border: "none"
  },
}, {dark: false})

const githubHighlighting = HighlightStyle.define([
  {tag: tags.function, color: '#6f42c1'},
  {tag: tags.className, color: '#6f42c1'},
  {tag: tags.keyword, color: '#d73a49'},
  {tag: tags.definitionKeyword, color: '#d73a49'},
  {tag: tags.controlKeyword, color: '#d73a49'},
  {tag: tags.operatorKeyword, color: '#d73a49'},
  {tag: tags.function(tags.variableName), color: "#"},
  {tag: tags.definition(tags.variableName), color: "#6f42c1"},
  {tag: tags.constant, color: '#005cc5'},
  {tag: tags.operator, color: '#d73a49'},
  {tag: tags.number, color: '#005cc5'},
  {tag: tags.string, color: '#032f62'},
  {tag: tags.comment, color: '#6a737d'}
])

export default function Editor(properties: EditorProperties) {
  const element = React.createRef<HTMLDivElement>()
  const editor = React.useRef<{editor: EditorView | undefined}>({editor: undefined})
  useEffect(() => {
    if (element == null) {
      return
    }
    const created = new EditorView({
      state: EditorState.create({
        doc: properties.code,
        extensions: [theme, basicSetup, javaLanguage, githubHighlighting]
      }),
      parent: element.current!,
    })
    editor.current.editor = created
    properties.onMount?.(created)
    return () => created.destroy()
  }, [])
  return (<div ref={element}/>)
}
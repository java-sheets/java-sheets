import React, {useEffect} from 'react'
import {EditorView} from '@codemirror/view'
import {EditorState} from '@codemirror/state'
import {basicSetup} from '@codemirror/basic-setup'
import {tags as t, HighlightStyle} from '@codemirror/highlight'
import {customTags, java} from "./java/language";

export interface EditorProperties {
  code: string
  editorRef?: React.MutableRefObject<EditorView | null>
}

const theme = EditorView.theme({
  ".cm-scroller": {
    fontFamily: `'JetBrains Mono', 'Roboto Mono', Menlo, Monaco, source-code-pro, Consolas, monospace`
  },
  "&": {
    color: "#24292e",
    backgroundColor: "#f6f8fa",
    padding: "10px 0"
  },
  ".cm-matchingBracket": {
    color: "#2b2b2b",
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
  {tag: t.function(t.name), color: '#00627A'},
  {tag: t.standard(t.typeName), fontStyle: 'bold', color: '#E75A7C'},
  {tag: customTags.annotationAttribute, color: '#8250df'},
  {tag: t.typeName, fontStyle: 'bold', color: '#89D2DC'},
  {tag: t.function(customTags.call), color: '#502cc5'},
  {tag: t.constant(t.variableName), color: '#0550ae'},
  {tag: t.className, color: '#6564DB'},
  {tag: t.keyword, color: '#D73A49'},
  {tag: t.definitionKeyword, color: '#D73A49'},
  {tag: t.controlKeyword, color: '#D73A49'},
  {tag: t.operatorKeyword, color: '#D73A49'},
  {tag: t.annotation, color: "#9E880D"},
  {tag: t.function(t.definition(t.variableName)), color: '#871094'},
  {tag: t.definition(t.variableName), color: "#6f42c1"},
  {tag: t.constant, color: '#005cc5'},
  {tag: t.operator, color: '#D73A49'},
  {tag: t.number, color: '#1750EB'},
  {tag: t.string, color: '#067D17'},
  {tag: t.comment, color: '#6a737d'}
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
        extensions: [theme, basicSetup, java(), githubHighlighting]
      }),
      parent: element.current!,
    })
    editor.current.editor = created
    if (properties.editorRef) {
      console.log('assigning ref')
      properties.editorRef.current = created
    }
    return () => created.destroy()
  }, [properties.editorRef])
  return (<div ref={element}/>)
}
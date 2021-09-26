import React, {useEffect} from 'react'
import {EditorView} from '@codemirror/view'
import {EditorState, Transaction} from '@codemirror/state'
import {basicSetup} from '@codemirror/basic-setup'
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

const darkTheme = EditorView.theme({
	".cm-scroller": {
		fontFamily: `'JetBrains Mono', Menlo, Monaco, source-code-pro, Consolas, monospace`
	},
	"&": {
		color: "#f6f8fa",
		backgroundColor: "#24292e",
		padding: "10px 0"
	},
	".cm-activeLineGutter, .cm-activeLine": {
		backgroundColor: "#24292e"
	},
	".cm-content": {
		caretColor: "#24292e"
	},
	"&.cm-focused .cm-cursor": {
		borderLeftColor: "#24292e"
	},
	"&.cm-focused .cm-selectionBackground, ::selection": {
		backgroundColor: "#24292e"
	},
	".cm-gutters": {
		backgroundColor: "#24292e",
		color: "#24292e",
		border: "none"
	},
}, {dark: true})

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

const githubDarkHighlighting = HighlightStyle.define([
	{tag: tags.function, color: '#dcbdfb'},
	{tag: tags.className, color: '#dcbdfb'},
	{tag: tags.keyword, color: '#f47067'},
	{tag: tags.definitionKeyword, color: '#f47067'},
	{tag: tags.controlKeyword, color: '#f47067'},
	{tag: tags.operatorKeyword, color: '#f47067'},
	{tag: tags.function(tags.variableName), color: "#f69d50"},
	{tag: tags.constant, color: '#6cb6ff'},
	{tag: tags.operator, color: '#f47067'},
	{tag: tags.number, color: '#6cb6ff'},
	{tag: tags.string, color: '#96d0ff'},
	{tag: tags.comment, color: '#768390'}
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
        extensions: [darkTheme, basicSetup, javaLanguage, githubDarkHighlighting]
      }),
      parent: element.current!,
    })
    editor.current.editor = created
    properties.onMount?.(created)
    return () => created.destroy()
  }, [])
  return (<div ref={element}/>)
}
import {HighlightStyle, tags as t} from '@codemirror/highlight'
import {customTags} from '../java/language'
import {EditorView} from '@codemirror/view'

export const theme = EditorView.theme({
  ".cm-scroller": {
    fontVariantLigatures: 'none',
    fontFamily: `'JetBrains Mono', Menlo, Monaco, source-code-pro, Consolas, monospace`
  },
  "&": {
    color: "#f6f8fa",
    backgroundColor: "#444c56",
    padding: "10px 0"
  },
  ".cm-activeLineGutter, .cm-activeLine": {
    backgroundColor: "#444c56"
  },
  ".cm-content": {
    caretColor: "#24292e"
  },
  "&.cm-focused .cm-cursor": {
    borderLeftColor: "#f6f8fa"
  },
  "&.cm-focused .cm-selectionBackground, ::selection": {
    backgroundColor: "#444c56"
  },
  ".cm-gutters": {
    backgroundColor: "#444c56",
    color: "#a6a8a8",
    border: "none"
  },
}, {dark: true})

export const highlighting = HighlightStyle.define([
  {tag: t.function(t.name), color: '#dcbdfb'},
  {tag: t.standard(t.typeName), fontStyle: 'bold', color: '#f47067'},
  {tag: customTags.annotationAttribute, color: '#f69d50'},
  {tag: t.typeName, fontStyle: 'bold', color: '#dcbdfb'},
  {tag: t.function(customTags.call), color: 'inherit'},
  {tag: t.constant(t.variableName), color: '#6cb6ff'},
  {tag: t.className, color: '#dcbdfb'},
  {tag: t.keyword, color: '#f47067'},
  {tag: t.definitionKeyword, color: '#f47067'},
  {tag: t.controlKeyword, color: '#f47067'},
  {tag: t.operatorKeyword, color: '#f47067'},
  {tag: t.annotation, color: "#f47067"},
  {tag: t.function(t.definition(t.variableName)), color: '#f69d50'},
  {tag: t.definition(t.variableName), color: "inherit"},
  {tag: t.constant, color: '#6cb6ff'},
  {tag: t.operator, color: '#f47067'},
  {tag: t.number, color: '#6cb6ff'},
  {tag: t.string, color: '#96d0ff'},
  {tag: t.comment, color: '#768390'}
])

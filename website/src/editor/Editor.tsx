import React, {useEffect} from 'react'
import {EditorView, keymap} from '@codemirror/view'
import {Compartment, EditorState} from '@codemirror/state'
import {basicSetup} from '@codemirror/basic-setup'
import {java} from "./java/language";
import {ThemeKey, useTheme} from '../theme/ThemeContext'
import {editorThemes, highlightingThemes} from './themes/themes'
import {defaultKeymap, insertTab} from "@codemirror/commands"

const tabSize = 2

function createView(initialContent: string, element: Element, theme: ThemeKey) {
  return new EditorView({
    parent: element,
    state: EditorState.create({
      doc: initialContent,
      extensions: [
        basicSetup,
        keymap.of([
          ...defaultKeymap,
          {
            key: 'Tab',
            run: insertTab
          }
        ]),
        EditorState.tabSize.of(tabSize),
        java(),
        themeState.of(editorThemes[theme]),
        highlightingState.of(highlightingThemes[theme])
      ]
    })
  })
}

export interface EditorProperties {
  code: string
  editorRef?: React.MutableRefObject<EditorView | null>
}

const themeState = new Compartment()
const highlightingState = new Compartment()

export default function Editor(properties: EditorProperties) {
  const element = React.createRef<HTMLDivElement>()
  const editor = React.useRef<{editor: EditorView | undefined}>({editor: undefined})
  const [theme] = useTheme()

  useEffect(() => {
    if (element == null) {
      return
    }
    const created = createView(properties.code, element.current!, theme)
    editor.current.editor = created
    if (properties.editorRef) {
      properties.editorRef.current = created
    }
    return () => created.destroy()
  }, [properties.editorRef])

  useEffect(() => {
    properties.editorRef?.current?.dispatch({
      effects: [
        themeState.reconfigure(editorThemes[theme]),
        highlightingState.reconfigure(highlightingThemes[theme])
      ]
    })
  }, [theme])

  return (<div ref={element}/>)
}
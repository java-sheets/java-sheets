import React, {useEffect} from 'react'
import {EditorView} from '@codemirror/view'
import {Compartment, EditorState} from '@codemirror/state'
import {basicSetup} from '@codemirror/basic-setup'
import {java} from './java/language'
import {highlighting, theme} from './theme'

const tabSize = 2

function createView(initialContent: string, element: Element) {
  return new EditorView({
    parent: element,
    state: EditorState.create({
      doc: initialContent,
      extensions: [
        basicSetup,
        EditorState.tabSize.of(tabSize),
        java(),
        themeState.of(theme),
        highlightingState.of(highlighting)
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
  const editor = React.useRef<{ editor: EditorView | undefined }>({editor: undefined})

  useEffect(() => {
    if (element == null) {
      return
    }
    const created = createView(properties.code, element.current!)
    editor.current.editor = created
    if (properties.editorRef) {
      properties.editorRef.current = created
    }
    return () => created.destroy()
  }, [properties.editorRef])


  return (<div ref={element}/>)
}
import {Sheet as SheetModel} from './index'
import {CaptureSnippetReference} from './Sheet'
import React, {useCallback} from 'react'
import {SnippetReference} from './snippet/Snippet'
import {useSheet} from './useSheet'
import Client from '../client'
import {message} from 'antd'

export function useShare(callback?: (sheet: SheetModel) => void): [() => void, CaptureSnippetReference] {
  const references = React.useRef(new Map<string, SnippetReference>())
  const {sheet} = useSheet()

  const share = useCallback(async () => {
    const client = Client.create()
    const snippets = [...references.current.values()]
      .map(reference => reference.serialize().toObject())
    try {
      const created = await client.sheets().post({
        title: sheet.title,
        snippetsList: snippets
      })
      message.info(`Sheet was shared`)
      callback?.(created)
    } catch (error) {
      console.error(error)
      message.error(`Failed to share sheet: ${error}`)
    }
  }, [references, callback])

  return [
    share,
    (id, snippet) => references.current.set(id, snippet)
  ]
}
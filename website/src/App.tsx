import React, {useCallback, useEffect, useState} from 'react'
import {Layout, message} from 'antd'
import {Content} from 'antd/lib/layout/layout'
import Sheet, {CaptureSnippetReference} from './sheet/Sheet'
import {Sheet as SheetModel} from './sheet'
import useEvaluate from './sheet/useEvaluate'
import Header from './header/Header'
import {SnippetReference} from './sheet/snippet/Snippet'
import Client from './client'
import {useSheet} from './sheet/useSheet'
import {
  Route,
  Switch, useHistory,
  useParams
} from 'react-router-dom'
import {StartEvaluationRequest} from '@jsheets/protocol/src/jsheets/api/snippet_runtime_pb'
import {LoadingOutlined} from '@ant-design/icons'
import styled from 'styled-components'

export default function App() {
  const [evaluate, evaluating] = useEvaluate()
  const history = useHistory()
  const callback = useCallback(created => history.push(`/s/${created.id}`), [history])
  const [share, captureSnippet] = useShare(callback)
  return (
    <Layout>
      <Header onShare={share}/>
      <Content style={{padding: 50}}>
          <Switch>
            <Route path="/s/:sheetId">
              <ImportedSheet
                evaluating={evaluating}
                evaluate={evaluate}
                captureSnippet={captureSnippet}
              />
            </Route>
            <Route path="/">
              <Sheet
                evaluating={evaluating}
                evaluate={evaluate}
                captureSnippet={captureSnippet}
              />
            </Route>
          </Switch>
      </Content>
    </Layout>
  )
}

interface ImportedSheetProperties {
  evaluating?: boolean
  evaluate: (start: StartEvaluationRequest) => void
  captureSnippet: CaptureSnippetReference
}

function ImportedSheet(properties: ImportedSheetProperties) {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState()
  const {sheetId} = useParams<{sheetId: string}>()
  const {update} = useSheet()

  useEffect(() => {
    const client = Client.create()
    if (!sheetId) {
      return
    }
    client.sheets().find(sheetId)
      .then(sheet => {
        update(sheet)
        setLoading(false)
      }).catch(error => {
        setLoading(false)
        setError(error)
      })
  }, [sheetId])

  if (loading) {
    return <Loading/>
  }
   return error
     ? <Failed error={error}/>
     : <Sheet {...properties}/>
}

const Centered = styled.div`
  margin: auto;
`

const Failed = ({error}: {error: any}) => (
  <Centered>{JSON.stringify(error)}</Centered>
)

const Loading = () => (
  <Centered><LoadingOutlined/></Centered>
)

function useShare(callback?: (sheet: SheetModel) => void): [() => void, CaptureSnippetReference] {
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
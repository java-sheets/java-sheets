import {StartEvaluationRequest} from '@jsheets/protocol/src/jsheets/api/snippet_runtime_pb'
import Sheet, {CaptureSnippetReference} from './Sheet'
import React, {useEffect, useState} from 'react'
import {useParams} from 'react-router-dom'
import {useSheet} from './useSheet'
import Client from '../client'
import styled from 'styled-components'
import {LoadingOutlined} from '@ant-design/icons'
import {Empty} from 'antd'
import themed from '../theme/themed'

interface ImportedSheetProperties {
  evaluating?: boolean
  isCooldown?: boolean
  evaluate: (start: StartEvaluationRequest) => void
  captureSnippet: CaptureSnippetReference
}

export default function ImportedSheet(properties: ImportedSheetProperties) {
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
  display: flex;
`

const ErrorBox = styled.div`
  margin: 50px auto auto auto;
  background: ${themed('snippet.card.background')};
  border-radius: 5px;
  padding: 20px;
  max-width: 300px;
  pre {
    margin: 0;
    width: 100%;
  }
  code {
    background: ${themed('snippet.card.icon.background')};
    width: 100%;
    padding: 10px;
  }
`

const Failed = ({error}: {error: any}) => (
  <Centered>
    <ErrorBox>
      <Empty
        description="Could not find the sheet"
      >
        <pre>
          <code>{JSON.stringify(error)}</code>
        </pre>
      </Empty>
    </ErrorBox>
  </Centered>
)

const LoadingBox = styled.div`
  position: absolute;
  top: 50%;
  left: 50%;
  .anticon {
    font-size: 20px;
  }
`

const Loading = () => (
  <Centered>
    <LoadingBox>
      <LoadingOutlined/>
    </LoadingBox>
  </Centered>

)
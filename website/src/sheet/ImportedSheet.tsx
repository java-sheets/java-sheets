import {StartEvaluationRequest} from '@jsheets/protocol/src/jsheets/api/snippet_runtime_pb'
import Sheet, {CaptureSnippetReference} from './Sheet'
import React, {useEffect, useState} from 'react'
import {useParams} from 'react-router-dom'
import {useSheet} from './useSheet'
import Client from '../client'
import styled from 'styled-components'
import {LoadingOutlined} from '@ant-design/icons'

interface ImportedSheetProperties {
  evaluating?: boolean
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
`

const Failed = ({error}: {error: any}) => (
  <Centered>{JSON.stringify(error)}</Centered>
)

const Loading = () => (
  <Centered><LoadingOutlined/></Centered>
)
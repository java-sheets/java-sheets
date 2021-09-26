import * as Styled from './Snippet.style'
import {ExperimentOutlined} from '@ant-design/icons'
import React, {MutableRefObject} from 'react'
import Title from './Title'
import {UseSnippet, useSnippet} from '../useSheet'
import {SheetSnippet} from '../index'
import ComponentList from "./component/ComponentList";
import SnippetExtras from "./SnippetExtras";
import {SnippetComponentListRef} from "./component/Component";
import * as SnippetProtocol from "@jsheets/protocol/src/jsheets/api/snippet_pb";
import * as EvaluationProtocol from "@jsheets/protocol/src/jsheets/api/snippet_runtime_pb";
import {StartEvaluationRequest} from "@jsheets/protocol/src/jsheets/api/snippet_runtime_pb";

export interface SnippetPosition {
  highestOrder: number
  lowestOrder: number
  moveUp: () => void
  moveDown: () => void
}

interface ExistingSnippetProperties extends UseSnippet {
  sheetId: string
  snippet: SheetSnippet
  position: SnippetPosition
  headProperties?: any
  running?: boolean
  snippetRef?: MutableRefObject<SnippetRef | null>
  onRun: (request: StartEvaluationRequest) => void
}

const MemoizedTitle = React.memo(Title)
const MemoizedExtras = React.memo(SnippetExtras)

interface ExistingSnippetState {
  editingTitle: boolean
}

export interface SnippetRef {
  listSources(): Map<string, string>
}

class ExistingSnippet
  extends React.Component<ExistingSnippetProperties, ExistingSnippetState>
  implements SnippetRef {

  private readonly componentsReferences: React.MutableRefObject<SnippetComponentListRef | null>
    = React.createRef<SnippetComponentListRef | null>()

  constructor(props: ExistingSnippetProperties) {
    super(props)
    this.componentsReferences.current = {components: new Map()}
    this.state = {editingTitle: props.snippet.title === ''}
  }

  changeEditingTitle = (target: boolean) => {
    if (!target && this.props.snippet.title === '') {
      this.props.changeDetails({title: 'None'})
    }
    this.setState({editingTitle: target})
  }

  componentDidMount() {
    if (this.props.snippetRef) {
      this.props.snippetRef.current = this
    }
  }

  render() {
    return (
      <Styled.Card>
        <Styled.CardHead{...this.props.headProperties}>
          <MemoizedTitle
            editing={this.state.editingTitle}
            icon={<ExperimentOutlined/>}
            onChange={title => this.props.changeDetails({title})}
            onEditingChange={this.changeEditingTitle}
            text={this.props.snippet.title}
          />
          <MemoizedExtras
            running={this.props.running}
            editingTitle={this.state.editingTitle}
            setEditingTitle={this.changeEditingTitle}
            delete={this.props.delete}
            addComponent={this.props.addComponent}
            onRun={this.run}
          />
        </Styled.CardHead>
        <Styled.CardBody>
          <ComponentList
            listRef={this.componentsReferences}
            snippetId={this.props.snippet.id}
            components={this.props.snippet.components}
            deleteComponent={this.props.deleteComponent}
          />
        </Styled.CardBody>
      </Styled.Card>
    )
  }

  run = () => {
    const start = createEvaluateRequest(
      this.props.sheetId,
      this.props.snippet,
      this.listSources()
    )
    this.props.onRun(start)
  }

  listSources = (): Map<string, string> => {
    const componentRefTable = this.componentsReferences.current?.components
    if (!componentRefTable) {
      return new Map()
    }
    const sources = new Map<string, string>()
    for (const component of this.props.snippet.components) {
      if (component.type !== 'code') {
        continue
      }
      const ref = componentRefTable.get(component.id)
      const source = ref?.content()
      if (source) {
        sources.set(component.id, source)
      }
    }
    return sources
  }
}

function createEvaluateRequest(sheetId: string, snippet: SheetSnippet, componentSources: Map<string, string>) {
  const reference = createSnippetReference(sheetId, snippet)
  const evaluatedSnippet = new EvaluationProtocol.EvaluatedSnippet()
  evaluatedSnippet.setReference(reference)
  const sources = new EvaluationProtocol.SnippetSources()
  sources.setReference(reference)
  for (const [id, code] of componentSources) {
    sources.addCodeComponents(createCodeComponent(id, code, snippet))
  }
  const request = new EvaluationProtocol.StartEvaluationRequest()
  request.setSnippet(evaluatedSnippet)
  request.setSourcesList([sources])
  return request
}

function createSnippetReference(sheetId: string, snippet: SheetSnippet) {
  const reference = new SnippetProtocol.Snippet.Reference()
  reference.setSheetId(sheetId)
  reference.setSnippetId(snippet.id)
  return reference
}

function createCodeComponent(id: string, code: string, snippet: SheetSnippet) {
  const component = new EvaluationProtocol.SnippetSources.CodeComponent()
  component.setId(id)
  component.setCode(code)
  component.setOrder(snippet.components.find(target => target.id === id)?.order || 1)
  return component
}


export interface SnippetProperties {
  id: string
  sheetId: string
  position: SnippetPosition
  dragHandleProps?: any
  running?: boolean
  snippetRef?: MutableRefObject<SnippetRef | null>
  onRun: (request: StartEvaluationRequest) => void
}

export default function Snippet(properties: SnippetProperties) {
  const snippetContext = useSnippet(properties.id)
  if (!snippetContext.snippet) {
    return <></>
  }
  return (
    <ExistingSnippet
      {...snippetContext}
      running={properties.running}
      onRun={properties.onRun}
      sheetId={properties.sheetId}
      snippetRef={properties.snippetRef}
      position={properties.position}
      headProperties={properties.dragHandleProps}
      snippet={snippetContext.snippet}
    />
  )
}
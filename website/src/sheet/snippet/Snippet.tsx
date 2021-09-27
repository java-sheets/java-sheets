import * as Styled from './Snippet.style'
import {ExperimentOutlined} from '@ant-design/icons'
import React, {MutableRefObject} from 'react'
import Title from './Title'
import {UseSnippet, useSnippet} from '../useSheet'
import {SheetSnippet} from '../index'
import ComponentList from "./component/ComponentList";
import SnippetExtras from "./SnippetExtras";
import {SnippetComponentListRef} from "./component/reference";
import * as SnippetProtocol from "@jsheets/protocol/src/jsheets/api/snippet_pb";
import {StartEvaluationRequest} from "@jsheets/protocol/src/jsheets/api/snippet_runtime_pb";
import createEvaluateRequest from './createEvauateRequest'

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
  serialize(): SnippetProtocol.Snippet
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
      this.listSources('code')
    )
    this.props.onRun(start)
  }

  serialize = () => {
    const message = new SnippetProtocol.Snippet()
    message.setId(this.props.snippet.id)
    message.setName(this.props.snippet.title)
    message.setComponentsList(this.serializeComponents())
    return message
  }

  serializeComponents = () => {
    const references = this.componentsReferences.current?.components
    const output: SnippetProtocol.Snippet.Component[] = []
    if (!references) {
      return output
    }
    for (const component of this.props.snippet.components) {
      const reference = references.get(component.id)
      const serialized = reference?.serialize()
      if (serialized) {
        serialized.setOrder(component.order)
        output.push(serialized)
      }
    }
    return output
  }

  listSources = (type?: 'text' | 'code'): Map<string, string> => {
    const componentRefTable = this.componentsReferences.current?.components
    if (!componentRefTable) {
      return new Map()
    }
    const sources = new Map<string, string>()
    for (const component of this.props.snippet.components) {
      if (component.type !== type) {
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
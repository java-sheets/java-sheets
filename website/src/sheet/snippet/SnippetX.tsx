import {ExperimentOutlined} from '@ant-design/icons'
import React from 'react'
import {UseSnippet, useSnippet} from '../useSheet'
import {SnippetState} from '../index'
import ComponentList from './component/ComponentList'
import {
  SnippetComponentListRef,
  SnippetComponentReference
} from './component/reference'
import * as SnippetProtocol from '@jsheets/protocol/src/jsheets/api/snippet_pb'
import {
  StartEvaluationRequest
} from '@jsheets/protocol/src/jsheets/api/snippet_runtime_pb'
import createEvaluationRequest from './createEvaluationRequest'
import styles from './Snippet.module.css'
import SnippetContext from './SnippetContext'

export interface SnippetPosition {
  highestOrder: number
  lowestOrder: number
  moveUp: () => void
  moveDown: () => void
}

interface ExistingSnippetProperties extends UseSnippet {
  sheetId: string
  snippet: SnippetState
  position: SnippetPosition
  headProperties?: any
  isCooldown?: boolean
  running?: boolean
  evaluate: (request: StartEvaluationRequest) => void
  capture?: (reference: SnippetReference) => void
}

interface ExistingSnippetState {
  editingTitle: boolean
}

export interface SnippetReference {
  listSources(): Map<string, string>

  serialize(): SnippetProtocol.Snippet
}

class ExistingSnippet
  extends React.Component<ExistingSnippetProperties, ExistingSnippetState>
  implements SnippetReference {

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
    this.props.capture?.(this)
  }

  render() {
    const {snippet} = React.useContext(SnippetContext)
    return (
      <div className={styles['snippet']}>
        <div className={styles['snippet-head']}>
          <div className={styles['snippet-title']}>{snippet.title}</div>
          <div className={styles['snippet-actions']}>
            <MemoizedExtras
              running={this.props.running}
              isCooldown={this.props.isCooldown}
              editingTitle={this.state.editingTitle}
              setEditingTitle={this.changeEditingTitle}
              delete={this.props.delete}
              addComponent={this.props.addComponent}
              onRun={this.run}
            />
          </div>
        </div>
        <div className={styles['snippet-body']}>
          <ComponentList
            capture={this.registerComponent}
            snippetId={this.props.snippet.id}
            components={this.props.snippet.components}
            deleteComponent={this.props.deleteComponent}
          />
        </div>
      </div>
    )
  }

  private registerComponent = (id: string, reference: SnippetComponentReference) => {
    this.componentsReferences.current?.components.set(id, reference)
  }

  run = () => {
    const start = createEvaluationRequest(
      this.props.sheetId,
      this.props.snippet,
      this.listSources('code')
    )
    this.props.evaluate(start)
  }

  serialize = () => {
    const message = new SnippetProtocol.Snippet()
    message.setId(this.props.snippet.id)
    message.setName(this.props.snippet.title)
    message.setOrder(this.props.snippet.order)
    message.setComponentsList(this.serializeComponents())
    return message
  }

  serializeComponents = () => {
    const references = this.componentsReferences.current?.components
    const output: SnippetProtocol.Snippet.Component[] = []
    if (!references) {
      return output
    }
    for (const component of Object.values(this.props.snippet.components)) {
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
    for (const component of Object.values(this.props.snippet.components)) {
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
  isCooldown?: boolean
  evaluate: (request: StartEvaluationRequest) => void
  capture?: (reference: SnippetReference) => void
}

export default function SnippetX(properties: SnippetProperties) {
  const snippetContext = useSnippet(properties.id)
  if (!snippetContext.snippet) {
    return <></>
  }
  return (
    <ExistingSnippet
      {...snippetContext}
      running={properties.running}
      isCooldown={properties.isCooldown}
      evaluate={properties.evaluate}
      sheetId={properties.sheetId}
      capture={properties.capture}
      position={properties.position}
      headProperties={properties.dragHandleProps}
      snippet={snippetContext.snippet}
    />
  )
}
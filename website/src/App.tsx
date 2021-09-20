import React from 'react'
import './antd.css'
import {Button, Layout, Menu, Space} from 'antd'
import {Content, Header} from 'antd/lib/layout/layout'
import * as icons from '@ant-design/icons'
import styled from 'styled-components'
import Sheet from "./sheet/snippet/Sheet";
import Client, {EvaluationListener} from "./client";
import {
  EvaluationError, EvaluationResult,
  MissingSources, StartEvaluationRequest
} from "@jsheets/protocol/src/jsheets/api/snippet_runtime_pb";
import {SheetSnippetComponentOutput} from "./sheet";
import {useDispatch} from "react-redux";
import {reportOutput} from "./sheet/state";

const Logo = styled.span`
  span:first-child {
    color: #00758f;
  }

  span:last-child {
    color: #f29110;
  }

  font-size: 30px;
  font-weight: bolder;
  font-family: 'Titillium Web', Roboto, 'Helvetica Neue', 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol' !important;
`

const StyledHeader = styled(Header)`
  box-shadow: 0 5px 5px 0 rgb(230 230 230 / 90%);

  .ant-menu-horizontal {
    border-bottom: none;
  }
`

export default function App() {
  const evaluator = useEvaluator()
  return (
    <Layout>
      <StyledHeader style={{background: '#fff'}} className="header">
        <Menu theme="light" style={{marginRight: 'auto'}} mode="horizontal" defaultSelectedKeys={['2']}>
          <Logo><span>Java</span><span>Sheets</span></Logo>
          <Space style={{marginLeft: 'auto'}} size="middle">
            <Button type="primary" ghost icon={<icons.ShareAltOutlined/>}>Share</Button>
          </Space>
        </Menu>
      </StyledHeader>
      <Content style={{padding: 50}}>
				<Sheet onRun={evaluator}/>
      </Content>
    </Layout>
  )
}

function useEvaluator() {
  const client = Client.forHost({protocol: 'http:', host: 'localhost:8090'})
  const dispatch = useDispatch()
  return (start: StartEvaluationRequest) => {
    console.log({start: start.toObject()})
    client.evaluate(
      start,
      new Listener((componentId, output) =>
        dispatch(reportOutput({componentId, output})))
    )
  }
}

type ReportOutput = (componentId: string, output: SheetSnippetComponentOutput) => void

class Listener implements EvaluationListener {
  constructor(private readonly reportOutput: ReportOutput) {}

  onEnd(): void {}

  onError = (error: EvaluationError) => {
    console.log({error})
    this.reportOutput(error.getComponentId(), {
      span: {
        start: error.getSpan()?.getStart() || 0,
        end: error.getSpan()?.getEnd() || 0
      },
      code: error.getKind(),
      message: error.getMessage()
    })
  }

  onMissingSources(sources: MissingSources): void {
    console.log({sources: sources.toObject()})
  }

  onResult = (result: EvaluationResult) => {
    console.log({result})
    this.reportOutput(result.getComponentId(), {
      type: result.getKind() === 1 ? 'info' : 'error',
      message: result.getOutput()
    })
  }
}
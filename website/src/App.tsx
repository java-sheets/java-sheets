import React from 'react'
import './antd.css'
import {Layout} from 'antd'
import {Content} from 'antd/lib/layout/layout'
import Sheet from "./sheet/snippet/Sheet";
import useEvaluate from "./sheet/useEvaluate";
import Header from "./header/Header";

export default function App() {
  const [evaluate, evaluating] = useEvaluate()
  return (
    <Layout>
      <Header/>
      <Content style={{padding: 50}}>
        <Sheet running={evaluating} onRun={evaluate}/>
      </Content>
    </Layout>
  )
}
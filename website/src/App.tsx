import React, {useCallback} from 'react'
import {Layout} from 'antd'
import {Content} from 'antd/lib/layout/layout'
import Sheet from './sheet/Sheet'
import useEvaluate from './sheet/useEvaluate'
import Header from './header/Header'
import {
  Route,
  Switch,
  useHistory,
} from 'react-router-dom'
import {useShare} from './sheet/useShare'
import ImportedSheet from './sheet/ImportedSheet'

export default function App() {
  const [evaluate, evaluating] = useEvaluate()
  const history = useHistory()
  const callback = useCallback(created => history.push(`/s/${created.id}`), [history])
  const [share, captureSnippet] = useShare(callback)
  return (
    <Layout>
      <Header onShare={share}/>
      <Content style={{padding: '0 50px 50px 50px'}}>
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


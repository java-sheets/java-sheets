import React, {useCallback, useState} from 'react'
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
import ShareModal from './sheet/ShareModal'
import {createBlankSheet, createWelcomeSheet} from './sheet/defaultSheet'

const welcomeSheet = createWelcomeSheet()
const blankSheet = createBlankSheet()

export default function App() {
  const [evaluate, evaluating, cooldown] = useEvaluate()
  const history = useHistory()
  const [sharedId, setSharedId] = useState('')
  const [shareVisible, setShareVisible] = useState(false)
  const callback = useCallback(created => {
    history.push(`/s/${created.id}`)
    setShareVisible(true)
    setSharedId(created.id)
  }, [history])
  const [share, captureSnippet] = useShare(callback)
  return (
    <Layout>
      <Header onShare={share}/>
      <Content style={{padding: '0 50px 50px 50px'}}>
          <ShareModal
            visible={shareVisible}
            onVisibilityChange={setShareVisible}
            sheetId={sharedId}
          />
          <Switch>
            <Route path="/s/:sheetId">
              <ImportedSheet
                evaluating={evaluating}
                isCooldown={cooldown}
                evaluate={evaluate}
                captureSnippet={captureSnippet}
              />
            </Route>
            <Route path={['/new', '/blank']}>
              <Sheet
                initial={blankSheet}
                isCooldown={cooldown}
                evaluating={evaluating}
                evaluate={evaluate}
                captureSnippet={captureSnippet}
              />
            </Route>
            <Route path="/">
              <Sheet
                initial={welcomeSheet}
                isCooldown={cooldown}
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


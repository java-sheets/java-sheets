import React, {useCallback, useState} from 'react'
import Sheet, {CaptureSnippetReference} from './sheet/Sheet'
import useEvaluate from './sheet/useEvaluate'
import {
  Route,
  Switch,
  useHistory,
} from 'react-router-dom'
import {useShare} from './sheet/useShare'
import ImportedSheet from './sheet/ImportedSheet'
import ShareModal from './sheet/ShareModal'
import {createBlankSheet, createWelcomeSheet} from './sheet/defaultSheet'
import Page from './layout/Page'

const welcomeSheet = createWelcomeSheet()
const blankSheet = createBlankSheet()

interface UseShareModal {
  component: React.ReactNode
  open: () => void
  captureSnippet: CaptureSnippetReference
}

function useShareModal(): UseShareModal {
  const [sharedId, setSharedId] = useState('')
  const [shareVisible, setShareVisible] = useState(false)
  const history = useHistory()
  const callback = useCallback(created => {
    history.push(`/s/${created.id}`)
    setShareVisible(true)
    setSharedId(created.id)
  }, [history])
  const [open, captureSnippet] = useShare(callback)

  const component = <ShareModal
    visible={shareVisible}
    onVisibilityChange={setShareVisible}
    sheetId={sharedId}
  />

  return {component, open, captureSnippet}
}

export default function App() {
  const [evaluate, evaluating, cooldown] = useEvaluate()
  const {open, component, captureSnippet} = useShareModal()
  return (
    <Page onShare={open}>
      {component}
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
    </Page>
  )
}


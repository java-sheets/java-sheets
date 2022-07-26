import React from 'react'
import SnippetContext from './SnippetContext'
import styles from './Snippet.module.css'
import ComponentList from './component/ComponentList'
import TerminalLineIcon from 'remixicon-react/TerminalLineIcon'
import AddLineIcon from 'remixicon-react/AddLineIcon'
import CloseLineIcon from 'remixicon-react/CloseLineIcon'

export default function Snippet() {
  const {snippet} = React.useContext(SnippetContext)
  return (
    <div className={styles['snippet']}>
      <div className={styles['snippet-header']}>
        <div className={styles['snippet-header-title']}>
          {snippet.title}
        </div>
        <div className={styles['snippet-header-actions']}>
          <button className={styles['add-code-button']}>
            <AddLineIcon/>
            <span>Code</span>
          </button>
          <button className={styles['add-docs-button']}>
            <AddLineIcon/>
            <span>Docs</span>
          </button>
          <button className={styles['run-button']}>
            <TerminalLineIcon/>
            <span>Run</span>
          </button>
          <button className={styles['delete-button']}>
            <CloseLineIcon/>
          </button>
        </div>
      </div>
      <div className={styles['snippet-body']}>
        <ComponentList/>
      </div>
    </div>
  )
}
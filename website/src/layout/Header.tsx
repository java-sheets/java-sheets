import React from 'react'
import {useTranslation} from 'react-i18next'
import styles from './Header.module.css'

export interface HeaderProps {
  onShare?: () => void
}

export default function Header(props: HeaderProps) {
  const {t} = useTranslation()
  return (
    <header className={styles['header']}>
      <div className={styles['branding']}>
        <div className={styles['logo']}>
          <strong>Java</strong>
          <span>Sheets</span>
        </div>
        <span className={styles['slogan']}>
          The online JShell editor
        </span>
      </div>
      <div className={styles['actions']}>
        <button
          className={styles['share-button']}
          onClick={props.onShare}
        >{t('menu.share')}</button>
      </div>
    </header>
  )
}
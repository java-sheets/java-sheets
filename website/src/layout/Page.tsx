import React from 'react'
import Header from './Header'
import Footer from './Footer'

import styles from './Page.module.css'

export interface PageProps {
  children?: React.ReactNode
  onShare?: () => void
}

export default function Page(props: PageProps) {
  return (
    <div className={styles['page']}>
      <Header onShare={props.onShare}/>
      <main className={styles['content']}>{props.children}</main>
      <footer className={styles['footer']}><Footer/></footer>
    </div>
  )
}
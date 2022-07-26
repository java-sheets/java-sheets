import styled, {createGlobalStyle} from 'styled-components'
import RichMarkdownEditor from 'rich-markdown-editor'

export const TextComponent = styled.div`
  padding: 0;
  margin: 0;
`

export const GlobalMarkdown = createGlobalStyle`
  #block-menu-container, .heading-actions, .block-menu-trigger {
    display: none !important;
  }
`

export const Editor = styled(RichMarkdownEditor)`
  margin: 0 !important;
  overflow: hidden;
  :first-child {
    padding: 10px !important;
  }
  background: var(--snippet-doc-bg) !important;
  * > div {
    background: var(--snippet-doc-bg) !important;
  }
  & > :first-child h1 {
    margin: 0 0 0.25em;
  }
  & > h1 {
    margin: 0.5em 0 0.25em;
  }
`
import styled from 'styled-components'
import {Input, Modal} from 'antd'

export const ShareModal = styled(Modal)`
  .ant-modal-footer {
    display: none;
  }
`

export const LinkText = styled(Input)`
  font-family: "JetBrains Mono", Menlo, Monaco, source-code-pro, Consolas, monospace;
`
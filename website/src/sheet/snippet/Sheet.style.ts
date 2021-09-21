import styled from 'styled-components'
import {Button} from 'antd'

export const SnippetContainer = styled.div`

`

export const Sheet = styled.div`
  ${SnippetContainer} {
    margin-top: 50px;
  }
`

export const AddButtonContainer = styled.div`
  display: flex;
  justify-content: center;
  margin: 50px auto 0 auto;
  width: 90%;
  max-width: 900px;
  .ant-btn:hover {
    background: #00758f;
    color: white;
  }
`

export const AddButton = styled(Button)`
  width: 100%;
`
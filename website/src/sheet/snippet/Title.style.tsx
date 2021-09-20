import styled from 'styled-components'
import {Input as AntdInput} from 'antd'

export const Title = styled.span`
  display: flex;
  font-size: 18px;
  cursor: default !important;
  & > * {
    margin: auto 0 auto 0;
  }
`

export const IconBox = styled.span`
  padding: 8px;
  display: flex;
  border-radius: 5px;
  margin: auto 5px auto 0;
  background: #f0f2f5;
  .anticon {
    color: #00758f;
    margin: auto 0;
  }
`

export const Input = styled(AntdInput)`
  margin-left: 5px;
  margin-right: 10px;
  font-size: 18px;
  max-width: 300px;
  height: 34px;
  padding: 2px;
  border: none;
  outline: none;
  &:focus {
    outline: none;
  }
`

export const Text = styled.span`
  margin: auto 0 auto 7px;
  cursor: text;
  user-select: text !important;
`
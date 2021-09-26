import styled from 'styled-components'
import {Input as AntdInput} from 'antd'
import themed from '../../theme/themed'

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
	background: ${themed('snippet.card.icon.background')};
	.anticon {
		color: ${themed('base.primary')};
		margin: auto 0;
	}
`

export const Input = styled(AntdInput)`
  margin: auto 10px auto 5px !important;
  max-width: 300px;
  height: 34px !important;
  padding: 2px !important;
  font-size: 18px !important;
  border: none !important;
  outline: none !important;
  &:focus {
    outline: none !important;
  }
`

export const Text = styled.span`
  margin: auto 0 auto 7px;
  cursor: text;
  user-select: text !important;
`
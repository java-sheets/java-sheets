import styled from 'styled-components'
import themed from '../../theme/themed'

export const Card = styled.div`
	max-width: 1000px;
	margin: 50px auto 0 auto;
	overflow: hidden;
	box-shadow: ${themed('snippet.card.shadow')};
	border: none;
	border-radius: 3px;
	background: ${themed('background')};
	&:first-child {
		margin-top: 0;
	}
`

export const CardHead = styled.div`
	padding: 0 15px;
	height: 70px;
	box-shadow: ${themed('snippet.card.head.shadow')} !important;
	display: flex;
`

export const CardBody = styled.div`
  padding: 0;
  overflow: hidden;
`
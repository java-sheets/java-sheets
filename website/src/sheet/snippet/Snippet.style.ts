import styled from 'styled-components'

export const Card = styled.div`
	max-width: 1000px;
	margin: 50px auto 0 auto;
	overflow: hidden;
	box-shadow: 0 3px 5px 0 rgb(200 200 200 / 30%);
	border: none;
	border-radius: 3px;
	background: white;
	&:first-child {
		margin-top: 0;
	}
`

export const CardHead = styled.div`
	padding: 0 15px;
	height: 70px;
	box-shadow: inset 0 -1px 0 0 #eeeeee !important;
	display: flex;
`

export const CardBody = styled.div`
	padding: 0;
	overflow: hidden;
`
import styled from 'styled-components'
import {Button, Space} from 'antd'

export const Card = styled.div`
	max-width: 1000px;
	margin: 50px auto 0 auto;
	overflow: hidden;
	box-shadow: 0 3px 5px 0 rgb(200 200 200 / 30%);
	border: none;
	border-radius: 3px;
	&:first-child {
		margin-top: 0;
	}
`

export const CardExtra = styled(Space)`
	margin: auto 0 auto auto;
	& > .ant-button {
		cursor: pointer !important;
	}
`

export const CardHead = styled.div`
	background: white;
	padding: 0 15px;
	height: 70px;
	box-shadow: inset 0 -1px 0 0 #eeeeee !important;
	display: flex;
`

export const CardBody = styled.div`
	padding: 0;
	overflow: hidden;
`

export const AddComponentButton = styled(Button)`
	width: 100%;
`

export const ComponentContent = styled.div`
	height: 100%;
`

export const ComponentOptions = styled.div`
	z-index: 20;
	position: absolute;
	bottom: 10px;
	right: 10px;
	display: flex;
	flex-direction: row;
	opacity: 100%;
	transition: all .1s ease-in-out;
	& > :not(:first-child) {
		margin-left: 5px;
	}
`

export const DeleteButton = styled(Button)`

`

export const DragHandle = styled.div`
	color: #00758f;
	background: transparent;
	display: flex;
	padding: 3px;
	border: 1px solid #00758f;
	border-radius: 2px;
	font-size: 15px;
	transition: background-color .2s ease-in;
	&:hover {
		background: rgba(250, 250, 250, 0.5);
	}
`

export const Component = styled.div<{isDragging: boolean}>`
	position: relative;
	min-height: 45px; // To include the options
	transition: all .1s ease-in;
	background: rgb(253, 253, 253);
	${props => props.isDragging
		? `
		box-shadow: inset 0 -2px 3px 1px rgba(240, 240, 240, 0.3);
		`
		: `:not(:hover) ${ComponentOptions} {
			opacity: 0;
			right: -10px;
		}
	`}
`

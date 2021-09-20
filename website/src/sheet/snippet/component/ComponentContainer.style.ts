import styled from "styled-components";
import {Button} from "antd";



export const ComponentOptions = styled.div`
	z-index: 20;
	position: absolute;
	bottom: 10px;
	display: flex;
	flex-direction: row;
	transition: all .1s ease-in-out;
	right: -10px;
	opacity: 0;
	& > :not(:first-child) {
		margin-left: 5px;
	}
`

export const ComponentInputArea = styled.div`
  position: relative;
`

export const Component = styled.div`
	min-height: 45px; // To include the options
	transition: opacity .1s ease-in, box-shadow .1s ease-in;
	background: rgb(253, 253, 253);

	&:not(:first-child) {
		border-top: 1px solid #ededed;
	}

	&.dragging-component {
		box-shadow: inset 0 -2px 3px 1px rgba(240, 240, 240, 0.3);
	}

  &.show-options ${ComponentOptions}, &.dragging-component ${ComponentOptions}, ${ComponentInputArea}:hover ${ComponentOptions} {
    opacity: 100%;
    right: 10px;
  }
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

export const DeleteButton = styled(Button)`
`

export const DuplicateButton = styled(Button)`
`

export const AddComponentButton = styled(Button)`
	width: 100%;
`

export const ComponentContent = styled.div`
	height: 100%;
`


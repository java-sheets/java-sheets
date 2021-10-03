import styled from 'styled-components'
import {Button} from "antd";

export const Overlay = styled.div`
  position: absolute;
  display: flex;
  left: 0;
  top: 5px;
  opacity: 0;
  transition: opacity .1s ease-in-out;
  & > * {
    margin: auto 0 auto 0;
  }
`

export const CloseButton = styled(Button)`
  color: white;
  &:hover {
    color: #e1e1e1;
  }
`

const OutputText = styled.pre`
  position: relative;
  font-size: 13px;
  color: #fff;
  overflow-y: scroll;
  background: #2c2c2c;
  transition: max-height 2s ease-in;
  display: flex;
  width: 100%;
  max-height: 200px;
  padding: 10px 35px;
  margin: 0;
  :empty {
    transition: max-height 2s ease-in;
    max-height: 0;
    padding: 0;
  }
  &:hover ${Overlay} {
    opacity: 1;
  }
  div {
    outline: none;
  }
  ::-webkit-scrollbar {
    width: 15px;
  }
  ::-webkit-scrollbar-track {
    background: none;
  }
  ::-webkit-scrollbar-thumb {
    cursor: pointer;
    background-color: #e2e2e2;
    border-radius: 20px;
    border: 4px solid #272727;
  }
`

export const ErrorOutput = styled(OutputText)`
  background-color: #e34e53;
`

export const InfoOutput = styled(OutputText)``

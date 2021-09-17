import styled from 'styled-components'

export const OutputText = styled.code`
  font-size: 13px;
  color: #fff;
  overflow-y: scroll;
  background: #2c2c2c;
  transition: max-height 2s ease-in;
  display: flex;
  width: 100%;
  max-height: 200px;
  padding: 10px 35px;
  :empty {
    transition: max-height 2s ease-in;
    max-height: 0;
    padding: 0;
  }
  div {
    outline: none;
  }
  div::-webkit-scrollbar {
    width: 15px;
  }
  div::-webkit-scrollbar-track {
    background: none;
  }
  div::-webkit-scrollbar-thumb {
    cursor: pointer;
    background-color: #e2e2e2;
    border-radius: 20px;
    border: 4px solid #272727;
  }
`
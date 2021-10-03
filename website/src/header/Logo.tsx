import React from "react";

import * as Styled from './Logo.style'
import {useHistory} from 'react-router-dom'

export function Logo() {
  const history = useHistory()
  return (
    <Styled.Logo onClick={() => history.push('/')}>
      <span>Java</span>
      <span>Sheets</span>
    </Styled.Logo>
  )
}

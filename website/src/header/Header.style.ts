import styled from "styled-components";
import {Header as AntdHeader} from "antd/lib/layout/layout";
import {Menu as AntdMenu} from "antd";
import themed from '../theme/themed'

export const Header = styled(AntdHeader)`
  box-shadow: ${themed('header.shadow')};
  background: ${themed('header.background')} !important;
  ul {
    background: none;
  }
  .ant-menu-horizontal {
    border-bottom: none;
  }
`

export const Menu = styled(AntdMenu)`
  margin-right: auto;
`
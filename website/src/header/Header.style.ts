import styled from "styled-components";
import {Header as AntdHeader} from "antd/lib/layout/layout";
import {Menu as AntdMenu} from "antd";

export const Header = styled(AntdHeader)`
  box-shadow: 0 5px 5px 0 rgb(230 230 230 / 90%);
  background: #fff;
  .ant-menu-horizontal {
    border-bottom: none;
  }
`

export const Menu = styled(AntdMenu)`
  margin-right: auto;
`
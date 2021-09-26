import {Button, Space} from "antd";
import {Logo} from "./Logo";
import * as icons from "@ant-design/icons";
import React from "react";
import * as Styled from './Header.style'
import {useTranslation} from "react-i18next";
import ToggleButton from '../theme/ToggleButton'

export default function Header() {
  const {t} = useTranslation()
  return (
    <Styled.Header className="header">
      <Styled.Menu mode="horizontal" defaultSelectedKeys={['2']}>
        <Logo/>
        <Space style={{marginLeft: 'auto'}} size="middle">
          <Button type="primary" ghost icon={<icons.ShareAltOutlined/>}>{t('menu.share')}</Button>
          <ToggleButton/>
        </Space>
      </Styled.Menu>
    </Styled.Header>
  )
}
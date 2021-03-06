import {Button, Space} from "antd";
import {Logo} from "./Logo";
import * as icons from "@ant-design/icons";
import React from "react";
import * as Styled from './Header.style'
import {useTranslation} from "react-i18next";
import ToggleButton from '../theme/ToggleButton'
import {GithubOutlined} from '@ant-design/icons'

export interface HeaderProperties {
  onShare?: () => void
}

export default function Header(properties: HeaderProperties) {
  const {t} = useTranslation()
  return (
    <Styled.Header className="header">
      <Styled.Menu mode="horizontal" defaultSelectedKeys={['2']}>
        <Logo/>
        <Space style={{marginLeft: 'auto'}} size="middle">
          <Button
            type="primary"
            ghost
            icon={<icons.ShareAltOutlined/>}
            onClick={properties.onShare}
          >{t('menu.share')}</Button>
          <ToggleButton/>
          <Button
            shape="circle"
            icon={<GithubOutlined/>}
            onClick={() => window.open('https://github.com/java-sheets/java-sheets', '_blank')}
          />
        </Space>
      </Styled.Menu>
    </Styled.Header>
  )
}
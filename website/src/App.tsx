import React from 'react'
import {Button, Layout, Menu, Space} from 'antd'
import {Content, Header} from 'antd/lib/layout/layout'
import * as icons from '@ant-design/icons'
import styled from 'styled-components'
import Sheet from "./sheet/Sheet";
import {useTranslation} from 'react-i18next'
import {useThemeToggle} from './theme/ThemeContext'
import themed from './theme/themed'

const Logo = styled.span`
  span:first-child {
    color: #00758f;
  }

  span:last-child {
    color: #f29110;
  }

  font-size: 30px;
  font-weight: bolder;
  font-family: 'Titillium Web', Roboto, 'Helvetica Neue', 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol' !important;
`

const StyledHeader = styled(Header)`
  box-shadow: ${themed('header.shadow')};
	background: ${themed('header.background')} !important;
	ul {
		background: none;
	}
  .ant-menu-horizontal {
    border-bottom: none;
  }
`

export default function App() {
	const {t} = useTranslation()
	const toggleTheme = useThemeToggle()
  return (
    <Layout>
      <StyledHeader className="header">
        <Menu style={{marginRight: 'auto'}} mode="horizontal" defaultSelectedKeys={['2']}>
          <Logo><span>Java</span><span>Sheets</span></Logo>
          <Space style={{marginLeft: 'auto'}} size="middle">
            <Button
							type="primary"
							ghost
							icon={<icons.ShareAltOutlined/>}>
							{t('menu.share')}
						</Button>
						<Button
							type="primary"
							shape="round"
							icon={<icons.BulbOutlined/>}
							onClick={toggleTheme}
						/>
          </Space>
        </Menu>
      </StyledHeader>
      <Content style={{padding: 50}}>
				<Sheet/>
      </Content>
    </Layout>
  )
}
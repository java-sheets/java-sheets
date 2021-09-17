import React from 'react'
import './antd.css'
import {Button, Layout, Menu, Space} from 'antd'
import {Content, Header} from 'antd/lib/layout/layout'
import * as icons from '@ant-design/icons'
import styled from 'styled-components'
import Sheet from './components/snippet/Sheet'

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
  box-shadow: 0 5px 5px 0 rgb(230 230 230 / 90%);

  .ant-menu-horizontal {
    border-bottom: none;
  }
`

function App() {
  return (
    <Layout>
      <StyledHeader style={{background: '#fff'}} className="header">
        <Menu theme="light" style={{marginRight: 'auto'}} mode="horizontal" defaultSelectedKeys={['2']}>
          <Logo><span>Java</span><span>Sheets</span></Logo>
          <Space style={{marginLeft: 'auto'}} size="middle">
            <Button type="primary" ghost icon={<icons.ShareAltOutlined/>}>Share</Button>
          </Space>
        </Menu>
      </StyledHeader>
      <Content style={{padding: 50}}>
				<Sheet/>
      </Content>
    </Layout>
  )
}

interface SheetProperties {
  title: string
  description?: React.ReactNode
  code: string
}


export default App

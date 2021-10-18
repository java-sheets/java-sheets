import {Button} from 'antd'
import {CheckOutlined, CopyOutlined, ShareAltOutlined} from '@ant-design/icons'
import * as Styled from './ShareModal.style'
import * as StyledTitle from './snippet/Title.style'
import useTimedFlag from '../util/useTimedFlag'
import copy from 'copy-to-clipboard'

export interface ShareModalProperties {
  visible?: boolean
  onVisibilityChange?: (state: boolean) => void
  sheetId?: string
}

function createLink(sheetId: string) {
  return window.location.origin + '/s/' + sheetId
}

export default function ShareModal(properties: ShareModalProperties) {
  const [clicked, setClicked] = useTimedFlag(false, 1000)

  const link =createLink(properties.sheetId || 'none')

  const onClick = () => {
    setClicked(true)
    copy(link, {format: 'text/plain'})
  }

  return (
    <Styled.ShareModal
      title={
        <StyledTitle.Title>
          <StyledTitle.IconBox><ShareAltOutlined/></StyledTitle.IconBox>
          <StyledTitle.Text>Share</StyledTitle.Text>
        </StyledTitle.Title>
      }
      visible={properties.visible}
      onCancel={() => properties.onVisibilityChange?.(false)}
      onOk={() => properties.onVisibilityChange?.(false)}
      footer={[]}
    >
      <p>Your Sheet has been saved. You can use the link to share it.</p>
      <Styled.LinkText
        value={link}
        suffix={
          <Button
            type="primary"
            icon={clicked ? <CheckOutlined/> : <CopyOutlined/>}
            onClick={onClick}
          />
        }
      />
    </Styled.ShareModal>
  )
}
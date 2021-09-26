import {Button} from 'antd'
import {BulbFilled, BulbOutlined} from '@ant-design/icons'
import {useTheme, useThemeToggle} from './ThemeContext'

export default function ToggleButton() {
  const [theme] = useTheme()
  const toggle = useThemeToggle()
  return (
    <Button
      type="primary"
      shape="circle"
      icon={theme === 'light' ? <BulbFilled /> : <BulbOutlined />}
      onClick={toggle}
    />
  )
}
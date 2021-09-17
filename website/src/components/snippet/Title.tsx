import React, {Ref, useEffect} from 'react'
import * as Styled from './Title.style'
import {Input} from 'antd'

interface TitleProperties {
  icon: React.ReactNode
  text: string
	editing: boolean
	onEditingChange?: (target: boolean) => void
  onChange?: (text: string) => void
}

export default function Title(properties: TitleProperties) {
	const ref = React.createRef<Input>()
	useEffect(() => {
		if (properties.editing) {
			ref.current?.focus()
		}
	}, [properties.editing])
  return (
    <Styled.Title>
			<Styled.IconBox>{properties.icon}</Styled.IconBox>
			{properties.editing
				? <Styled.Input
						ref={ref}
						value={properties.text}
						onPressEnter={() => properties.onEditingChange?.(false)}
						onChange={event => properties.onChange?.(event.target.value)}
					/>
				: <Styled.Text onDoubleClick={() => properties.onEditingChange?.(true)}>{properties.text}</Styled.Text>
			}

    </Styled.Title>
  )
}

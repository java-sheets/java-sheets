import {Button, Dropdown, Menu} from "antd";
import {
	CodeOutlined,
	CommentOutlined, DeleteOutlined, EditOutlined,
	FireOutlined, MoreOutlined,
	PlusOutlined
} from "@ant-design/icons";
import * as Styled from "./SnippetExtras.style";
import React from "react";
import {SheetSnippetComponent} from "../index";
import {TFunction} from 'i18next'
import {useTranslation, WithTranslationProps} from 'react-i18next'

type AddComponent = (component: Partial<SheetSnippetComponent>) => void

export interface SnippetExtrasProperties {
	editingTitle: boolean
	setEditingTitle: (state: boolean) => void
	delete: () => void
	addComponent: AddComponent
  onRun: () => void
  running?: boolean
}

const AddMenu: React.FC<{addComponent: AddComponent, t: TFunction}> = ({addComponent, t}) =>  (
	<Dropdown
		overlay={
			<Menu>
				<Menu.Item
					key="add-code"
					icon={<CodeOutlined />}
					onClick={() => addComponent({type: 'code'})}
				>{t('snippet.menu.add.code')}</Menu.Item>
				<Menu.Item
					key="add-text"
					icon={<CommentOutlined />}
					onClick={() => addComponent({type: 'text'})}
				>{t('snippet.menu.add.comment')}</Menu.Item>
			</Menu>
		}>
		<Button type="primary" ghost icon={<PlusOutlined/>}>
			{t('snippet.menu.add.title')}
		</Button>
	</Dropdown>
)

const EditMenu: React.FC<SnippetExtrasProperties & {t: TFunction}> = properties => (
	<Dropdown overlay={
		<Menu>
			<Menu.Item
				key="rename"
				icon={<EditOutlined/>}
				onClick={() => properties.setEditingTitle(!properties.editingTitle)}
			>{properties.t('snippet.menu.edit.rename')}</Menu.Item>
			<Menu.Item
				key="delete"
				danger
				icon={<DeleteOutlined/>}
				onClick={properties.delete}
			>{properties.t('snippet.menu.edit.delete')}</Menu.Item>
		</Menu>
	}>
		<Button
			type="primary"
			ghost
			icon={<MoreOutlined/>}
			title={properties.t('snippet.menu.edit.title')}
		/>
	</Dropdown>
)

export default function SnippetExtras(properties: SnippetExtrasProperties) {
	const {t} = useTranslation()
	return (
		<Styled.SnippetExtras>
			<AddMenu t={t} key="add" addComponent={properties.addComponent}/>
			<Button
        type="primary"
        icon={<FireOutlined/>}
        loading={properties.running}
        onClick={properties.onRun}
      >{t('snippet.menu.run.button')}</Button>
			<EditMenu t={t} key="edit" {...properties}/>
		</Styled.SnippetExtras>
	)
}
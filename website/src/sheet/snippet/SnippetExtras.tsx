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

type AddComponent = (component: Partial<SheetSnippetComponent>) => void

export interface SnippetExtrasProperties {
	editingTitle: boolean
	setEditingTitle: (state: boolean) => void
	delete: () => void
	addComponent: AddComponent
  onRun: () => void
}

const AddMenu: React.FC<{addComponent: AddComponent}> = ({addComponent}) =>  (
	<Dropdown
		overlay={
			<Menu>
				<Menu.Item
					key="add-code"
					icon={<CodeOutlined />}
					onClick={() => addComponent({type: 'code'})}
				>Code</Menu.Item>
				<Menu.Item
					key="add-text"
					icon={<CommentOutlined />}
					onClick={() => addComponent({type: 'text'})}
				>Comment</Menu.Item>
			</Menu>
		}>
		<Button type="primary" ghost icon={<PlusOutlined/>}>Add</Button>
	</Dropdown>
)

const EditMenu: React.FC<SnippetExtrasProperties> = properties => (
	<Dropdown overlay={
		<Menu>
			<Menu.Item
				key="rename"
				icon={<EditOutlined/>}
				onClick={() => properties.setEditingTitle(!properties.editingTitle)}
			>Rename</Menu.Item>
			<Menu.Item
				key="delete"
				danger
				icon={<DeleteOutlined/>}
				onClick={properties.delete}
			>Delete</Menu.Item>
		</Menu>
	}>
		<Button type="primary" ghost icon={<MoreOutlined/>}/>
	</Dropdown>
)

export default function SnippetExtras(properties: SnippetExtrasProperties) {
	return (
		<Styled.SnippetExtras>
			<AddMenu key="add" addComponent={properties.addComponent}/>
			<Button
        type="primary"
        icon={<FireOutlined/>}
        onClick={properties.onRun}
      >Run</Button>
			<EditMenu key="edit" {...properties}/>
		</Styled.SnippetExtras>
	)
}
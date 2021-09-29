import * as Styled from './OutputText.style'
import {ErrorOutput, SheetSnippetComponentOutput} from "../../index";
import {CloseCircleFilled} from '@ant-design/icons'

export interface OutputTextProperties {
  output?: SheetSnippetComponentOutput
  onClose?: () => void
}

export default function OutputText(properties: OutputTextProperties) {
  if (!properties.output) {
    return <></>
  }
  const errorOutput = properties.output as ErrorOutput
  const isError = errorOutput.code !== undefined
  const Type = isError ? Styled.ErrorOutput : Styled.InfoOutput
  const content = isError
    ? errorOutput.message
    : properties.output.message
  return (
    <Type>
      {content}&nbsp;
      <Styled.Overlay>
        <Styled.CloseButton
          type="text"
          icon={<CloseCircleFilled/>}
          onClick={properties.onClose}
        />
      </Styled.Overlay>
    </Type>
  )
}
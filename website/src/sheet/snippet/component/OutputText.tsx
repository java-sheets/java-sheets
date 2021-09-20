import * as Styled from './OutputText.style'
import {ErrorOutput, SheetSnippetComponentOutput} from "../../index";

export interface OutputTextProperties {
	output?: SheetSnippetComponentOutput
}

export default function OutputText(properties: OutputTextProperties) {
  if (!properties.output) {
    return <></>
  }
  const errorOutput = properties.output as ErrorOutput
  if (errorOutput.span !== undefined) {
    return (
      <Styled.OutputText>
        {errorOutput.code}
        {errorOutput.message}
      </Styled.OutputText>
    )
  }
	return (
		<Styled.OutputText>
			{properties.output.message}
		</Styled.OutputText>
	)
}
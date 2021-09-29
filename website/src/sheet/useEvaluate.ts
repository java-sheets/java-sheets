import {
  EvaluationError, EvaluationResult, MissingSources,
  StartEvaluationRequest
} from "@jsheets/protocol/src/jsheets/api/snippet_runtime_pb";
import Client, {EvaluationListener} from "../client";
import {useDispatch} from "react-redux";
import {useState} from "react";
import {removeOutput, reportOutput} from "./state";
import {SheetSnippetComponentOutput} from "./index";

type UseEvaluate = [(start: StartEvaluationRequest) => void, boolean]

export default function useEvaluate(): UseEvaluate {
  const dispatch = useDispatch()
  const [evaluating, setEvaluating] = useState(false)
  const evaluate = (start: StartEvaluationRequest) => {
    const client = Client.create()
    setEvaluating(true)
    dispatch(removeOutput({}))
    client.evaluate(
      start,
      new Listener({
          reportOutput(componentId, output) {
            dispatch(reportOutput({componentId, output}))
          },
          close() {
            setEvaluating(false)
          }
        }
      ))
  }
  return [evaluate, evaluating]
}

type ReportOutput = (componentId: string, output: SheetSnippetComponentOutput) => void

class Listener implements EvaluationListener {
  constructor(
    private readonly callback: { reportOutput: ReportOutput, close: () => void }
  ) {
  }

  onEnd(): void {
    this.callback.close()
  }

  onError = (error: EvaluationError) => {
    this.callback.reportOutput(error.getComponentId(), {
      span: {
        start: error.getSpan()?.getStart() || 0,
        end: error.getSpan()?.getEnd() || 0
      },
      code: error.getKind(),
      message: error.getMessage()
    })
  }

  onMissingSources(sources: MissingSources): void {
    console.log({sources: sources.toObject()})
  }

  onResult = (result: EvaluationResult) => {
    this.callback.reportOutput(result.getComponentId(), {
      type: result.getKind() === 1 ? 'info' : 'error',
      message: result.getOutput()
    })
  }
}
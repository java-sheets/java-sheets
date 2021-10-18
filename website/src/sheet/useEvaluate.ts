import {
  EvaluationError, EvaluationResult, MissingSources,
  StartEvaluationRequest
} from "@jsheets/protocol/src/jsheets/api/snippet_runtime_pb";
import Client, {EvaluationListener} from "../client";
import {useDispatch} from "react-redux";
import {useState} from "react";
import {removeOutput, reportOutput} from "./state";
import {SnippetComponentOutput} from "./index";
import modal from "antd/lib/modal";
import useTimedFlag from "../util/useTimedFlag";

type UseEvaluate = [(start: StartEvaluationRequest) => void, boolean, boolean]

const clientSideCooldown = 1500

export default function useEvaluate(): UseEvaluate {
  const dispatch = useDispatch()
  const [evaluating, setEvaluating] = useState(false)
  const [isCooldown, setCooldown] = useTimedFlag(false, clientSideCooldown)

  const evaluate = (start: StartEvaluationRequest) => {
    if (evaluating || isCooldown) {
      return
    }
    const client = Client.create()
    setEvaluating(true)
    dispatch(removeOutput({}))
    setCooldown(true)
    client.evaluate(
      start,
      new Listener({
        reportOutput(componentId, output) {
          dispatch(reportOutput({componentId, output}))
        },
        close() {
          setEvaluating(false)
        }
      })
    )
  }
  return [evaluate, evaluating, isCooldown]
}

type ReportOutput = (componentId: string, output: SnippetComponentOutput) => void

const tooManyRequestsCode = 429

class Listener implements EvaluationListener {
  constructor(
    private readonly callback: { reportOutput: ReportOutput, close: () => void }
  ) {
  }

  onEnd(): void {
    this.callback.close()
  }

  onServiceError = (code: number) =>{
    if (code == tooManyRequestsCode) {
      modal.error({
        title: 'Too many requests',
        content: 'The server is receiving too many evaluation requests right' +
          'now, please try again in a few seconds.'
      })
    }
  }

  onEvaluationError = (error: EvaluationError) => {
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
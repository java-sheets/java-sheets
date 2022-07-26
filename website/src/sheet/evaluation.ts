import {atom, useAtom} from 'jotai'

const isEvaluatingAtom = atom(false)

export function useIsEvaluating(): boolean {
  const [state] = useAtom(isEvaluatingAtom)
  return state
}
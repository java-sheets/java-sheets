import {useCallback, useRef, useState} from "react";

type TimeoutId = ReturnType<typeof setTimeout>

export type UseTimedFlag = [boolean, (target: boolean) => void]

export default function useTimedFlag(initial: boolean, timeout: number): UseTimedFlag {
  const active = useRef<{ active?: TimeoutId }>({active: undefined})
  const [state, setState] = useState(initial)

  const toggle = useCallback((target) => {
    if (target) {
      setState(true)
      active.current.active = setTimeout(() => {
        setState(false)
      }, timeout)
      return true
    } else {
      setState(false)
      const currentTimeout = active.current.active
      if (currentTimeout !== undefined) {
        clearTimeout(currentTimeout)
      }
      return false
    }
  }, [active, timeout])

  return [state, toggle]
}
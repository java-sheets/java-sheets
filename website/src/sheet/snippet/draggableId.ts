import {useMemo} from 'react'

export function useDraggableId(id: string): number {
  const [associate] = useDraggableIds()
  return useMemo(() => associate(id), [id])
}

const table = {ids: new Map<string, number>(), count: 1}

export function resetDraggableIds() {
  table.ids.clear()
  table.count = 1
}

export function useDraggableIds(): [(id: string) => number, () => void] {
  return [
    id => {
      const existing = table.ids.get(id)
      if (existing) {
        return existing
      }
      const numericId = table.count++
      table.ids.set(id, numericId)
      return numericId
    },
    resetDraggableIds
  ]
}
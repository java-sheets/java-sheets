import {DefaultTheme} from 'styled-components'

export default function themed(name: string) {
  return ({theme}: {theme: DefaultTheme}) => {
    return accessPath(undefined, theme   , name.split('.'))
  }
}

function accessPath(
  parent: any,
  object: any,
  path: string[],
  index: number = 0): any {
  if (object === undefined) {
    if (parent !== undefined) {
      const possibleFields = Object.keys(parent)
      throw new Error(
        `could not find ${path[index]} in ${path[index - 1]} (only has: ${possibleFields})`
      )
    }
    throw new Error(`could not find ${path[index]}`)
  }
  if (path.length === index) {
    return object
  }
  const child = object[path[index]]
  return accessPath(object, child, path, index + 1)
}
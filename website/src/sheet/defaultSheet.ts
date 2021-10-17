import {SheetState} from "./index";
import {v4 as uuid} from "uuid";

export function createWelcomeSheet(): SheetState {
  const id = uuid()
  const firstComponentId = uuid()
  const secondComponentId = uuid()
  return {
    id: uuid(),
    title: 'Welcome!',
    description: '',
    snippets: {
      [id]: {
        id: id,
        title: 'Welcome!',
        order: 0,
        components: {
          [firstComponentId]: {
            id: firstComponentId,
            order: 0,
            content: `Welcome to the JSheets Demo`,
            type: 'text'
          },
          [secondComponentId]: {
            id: secondComponentId,
            order: 1,
            content: `Math.min(420, 1337)`,
            type: 'code'
          }
        }
      }
    }
  }
}

export function createBlankSheet(): SheetState {
  const id = uuid()
  const componentId = uuid()
  return {
    id: uuid(),
    title: 'Welcome!',
    description: '',
    snippets: {
      [id]: {
        id: id,
        title: 'Welcome!',
        order: 0,
        components: {
          [componentId]: {
            id: componentId,
            order: 0,
            content: ``,
            type: 'code'
          },
        }
      }
    }
  }
}


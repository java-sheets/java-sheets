const commentNodeType = 8

export function findCommentNode(comment: string) {
  for (const node of document.head.childNodes) {
    if (node.nodeType === commentNodeType && node?.nodeValue?.trim() === comment) {
      return node;
    }
  }
  return null;
}

export function createLinkElement(attributes: Partial<HTMLLinkElement>) {
  const elements = document.createElement('link')
  for (const [attribute, value] of Object.entries(attributes)) {
    // @ts-ignore
    elements[attribute] = value;
  }
  return elements
}

function insertStyle(linkElement: HTMLElement, insertionPoint?: string) {
  const insert = insertionPoint
    ? selectInsertionMethod(insertionPoint)
    : insertToHead
  insert(linkElement)
}

function insertToHead(element: Node) {
  return document.head.appendChild(element)
}

function insertBefore(parent: Node, point: Node) {
  return (element: Node) => parent.insertBefore(element, point?.nextSibling)
}

function selectInsertionMethod(name: string): (element: Node) => void {
  const point = findCommentNode(name)
  const parent = point?.parentNode
  return parent && point
    ? insertBefore(parent, point)
    : insertToHead
}

function createStyleLink(id: string, link: string, callback?: Callback) {
  const temporaryId = id + '_temp'
  return createLinkElement({
    type: 'text/css',
    rel: 'stylesheet',
    id: temporaryId,
    href: link,
    onload: () => {
      document.getElementById(id)?.remove()
      const nextStyle = document.getElementById(temporaryId)
      nextStyle?.setAttribute('id', id)
      callback?.()
    }
  })
}

type ThemeSources = Record<string, string>

function *listPrefetches(themeSources: ThemeSources) {
  for (const [theme, link] of Object.entries(themeSources)) {
    const id = `theme-prefetch-${theme}`;
    if (document.getElementById(id)) {
      continue
    }
    yield createPrefetch(theme, id, link)
  }
}

function createPrefetch(theme: string, id: string, link: string) {
  const prefetch = document.createElement('link');
  prefetch.rel = 'prefetch'
  prefetch.type = 'text/css'
  prefetch.id = id;
  prefetch.href = link
  return prefetch
}

const activeThemeId = 'active-theme'
const insertionPoint = 'active-theme'

type Callback = () => void

export function updateTheme(theme: string, link: string, callback?: Callback) {
  const element = createStyleLink(activeThemeId, link, callback)
  insertStyle(element, insertionPoint)
  document.body.setAttribute('data-theme', theme)
}

export function insertPrefetches(themes: ThemeSources) {
  const insert = selectInsertionMethod(insertionPoint)
  for (const prefetch of listPrefetches(themes)) {
    insert(prefetch)
  }
}
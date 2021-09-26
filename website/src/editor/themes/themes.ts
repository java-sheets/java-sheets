import * as darkTheme from './dark'
import * as lightTheme from './light'
import {ThemeTable} from '../../theme/ThemeContext'
import {Extension} from '@codemirror/state'

export const editorThemes: ThemeTable<Extension> = {
  light: lightTheme.theme,
  dark: darkTheme.theme
}

export const highlightingThemes: ThemeTable<Extension> = {
  light: lightTheme.highlighting,
  dark: darkTheme.highlighting
}

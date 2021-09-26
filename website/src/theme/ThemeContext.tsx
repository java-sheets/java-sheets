import React, {useEffect, useState} from 'react'
import {insertPrefetches, updateTheme} from './updateTheme'
import {light as lightTheme, dark as darkTheme} from './theme'
import {
	DefaultTheme,
	ThemeProvider as StyledThemeProvider
} from 'styled-components'

export type ThemeKey = 'light' | 'dark'

type UpdateTheme = React.Dispatch<React.SetStateAction<ThemeKey>>

export interface ThemeContextProperties {
	theme: ThemeKey
	update: UpdateTheme
}

export const ThemeContext = React.createContext<ThemeContextProperties>(
	{
		theme: 'light',
		update: () => 'light'
	}
)

export function useTheme(): [ThemeKey, UpdateTheme] {
	const {theme, update} = React.useContext(ThemeContext)
	return [theme, update]
}

type ToggleTheme = () => void

export function useThemeToggle(): ToggleTheme {
	const [, update] = useTheme()
	return () => update((current: ThemeKey) => toggle(current))
}

function toggle(current: ThemeKey): ThemeKey {
	return current === 'light' ? 'dark' : 'light'
}

export interface ThemeProviderProperties {
	initialTheme?: ThemeKey
	children: React.ReactNode
}

export type ThemeTable<T> = Record<ThemeKey, T>

const themeSources: ThemeTable<string> = {
	light: '/theme/light.css',
	dark: '/theme/dark.css'
}

const themeVariables: ThemeTable<DefaultTheme> = {
	light: lightTheme,
	dark: darkTheme
}

export async function installThemes(): Promise<string> {
	return new Promise(resolve => {
		insertPrefetches(themeSources)
		const theme = 'light'
		updateTheme(theme, themeSources[theme], () => resolve(theme))
	})
}

export default function ThemeProvider(properties: ThemeProviderProperties) {
	const [theme, update] = useState<ThemeKey>(properties.initialTheme || 'light')
	const variables = themeVariables[theme]

	useEffect(() => {
		const link = themeSources[theme]
		updateTheme(theme, link)
	}, [theme])

	return (
		<ThemeContext.Provider value={{theme, update}}>
			<StyledThemeProvider theme={variables}>
				{properties.children}
			</StyledThemeProvider>
		</ThemeContext.Provider>
	)
}
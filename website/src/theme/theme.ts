export const light = {
	background: '#fff',
	base: {
		primary: '#00758f'
	},
	header: {
		background: '#fff',
		shadow: '0 5px 5px 0 rgb(230 230 230 / 90%)',
	},
	snippet: {
		card: {
			background: '#ffffff',
			shadow: '0 3px 5px 0 rgb(200 200 200 / 30%)',
			head: {
				shadow: 'inset 0 -1px 0 0 #eeeeee'
			},
			icon: {
				background: '#f0f2f5'
			}
		}
	}
}

export const dark: typeof light = {
	background: '#252525',
	base: {
		primary: '#00758f'
	},
	header: {
		background: '#252525',
		shadow: '0 5px 5px 0 rgb(23 23 23 / 90%)'
	},
	snippet: {
		card: {
			background: '#313131',
			shadow: '0 3px 5px 0 rgb(35 35 35 / 30%)',
			head: {
				shadow: 'inset 0 -1px 0 0 #eeeeee'
			},
			icon: {
				background: '#313131'
			}
		}
	}
}

type ThemeType = typeof light

declare module 'styled-components' {
	export interface DefaultTheme extends ThemeType {}
}
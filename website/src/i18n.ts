import i18n from 'i18next'
import {initReactI18next} from 'react-i18next'

const resources = {
  en: {
    translation: {
      menu: {
        share: 'Share'
      },
      snippet: {
        menu: {
          run: {
            button: 'Run'
          },
          add: {
            title: 'New',
            code: 'Code',
            comment: 'Comment'
          },
          edit: {
            rename: 'Rename',
            delete: 'Delete',
            title: 'Edit'
          },
        },
        component: {
          option: {
            delete: 'Delete'
          }
        },
      }
    },
  },
  de: {
    translations: {
      menu: {
        share: 'Teilen'
      },
      snippet: {
        menu: {
          run: {
            button: 'Ausführen'
          },
          add: {
            title: 'Neu',
            code: 'Code',
            comment: 'Kommentar'
          },
          edit: {
            rename: 'Umbenennen',
            delete: 'Löschen',
            title: 'Anpassen'
          },
        },
        component: {
          option: {
            delete: 'Löschen'
          }
        },
      }
    }
  }
}

i18n
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: 'en',
    supportedLngs: ['en', 'de'],
    interpolation: {escapeValue: false}
  })

export default i18n
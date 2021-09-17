import i18n from "i18next";
import {initReactI18next} from 'react-i18next'

const resources = {
  en: {
    translation: {
      snippet: {
        format: {
          title: 'Format',
          description: 'Formats the snippets code'
        },
        run: {
          title: 'Run',
          description: 'Evaluates the snippets code'
        }
      }
    }
  },
  de: {
    translation: {
      snippet: {
        format: {
          title: 'Formatieren',
          description: 'Formatiert den Quelltext des Snippets'
        },
        run: {
          title: 'Starten',
          description: 'Evaluiert den Quelltext des Snippets'
        }
      }
    }
  }
};

i18n.use(initReactI18next).init({
  resources,
  lng: "en",
  interpolation: {
    escapeValue: false
  }
});

export default i18n
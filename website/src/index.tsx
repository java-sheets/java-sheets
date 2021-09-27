import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import {Provider} from 'react-redux'
import store from './store'
import {I18nextProvider} from 'react-i18next'
import i18n from './i18n'
import ThemeProvider, {detectTheme, installThemes} from './theme/ThemeContext'
import {BrowserRouter} from 'react-router-dom'

installThemes().then(() => {
  ReactDOM.render(
    <React.StrictMode>
      <Provider store={store}>
        <React.Suspense fallback={<></>}>
          <I18nextProvider i18n={i18n}>
            <ThemeProvider initialTheme={detectTheme()}>
              <BrowserRouter>
                <App/>
              </BrowserRouter>
            </ThemeProvider>
          </I18nextProvider>
        </React.Suspense>
      </Provider>
    </React.StrictMode>,
    document.getElementById('root')
  );
})

reportWebVitals().catch(details => {
  console.log({error: 'failed to report web vitals', details})
})

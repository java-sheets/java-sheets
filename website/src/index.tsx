import React from 'react'
import ReactDOM from 'react-dom'
import './index.css'
import App from './App'
import reportWebVitals from './reportWebVitals'
import {Provider} from 'react-redux'
import store from './store'
import {I18nextProvider} from 'react-i18next'
import i18n from './i18n'
import {BrowserRouter} from 'react-router-dom'

ReactDOM.render(
  <React.StrictMode>
    <Provider store={store}>
      <React.Suspense fallback={<></>}>
        <I18nextProvider i18n={i18n}>
          <BrowserRouter>
            <App/>
          </BrowserRouter>
        </I18nextProvider>
      </React.Suspense>
    </Provider>
  </React.StrictMode>,
  document.getElementById('root')
)

reportWebVitals().catch(details => {
  console.log({error: 'failed to report web vitals', details})
})

import {
  combineReducers,
  configureStore,
} from '@reduxjs/toolkit'
import sheetReducer from './sheet/state'

const rootReducer = combineReducers({sheet: sheetReducer})

export type RootState = ReturnType<typeof rootReducer>

const store = configureStore({reducer: rootReducer})

export type AppDispatch = typeof store.dispatch

export default store
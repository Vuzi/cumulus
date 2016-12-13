import * as React from "react"
import { connect } from "react-redux"

import { hashHistory } from "react-router"

const App = () => {
  return (
    <div>
      <h1>Hello World !</h1>
      <button onClick={() => {
        hashHistory.push("/login")
      }}>login</button>
    </div>
  )
}

export default connect()(App)

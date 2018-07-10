import React, { Component } from 'react';
import './App.css';
import axios from 'axios';

class App extends Component {
  state = {
    data: []
  }

  componentDidMount() {
    this.loadData();
  }

  loadData() {
    axios.get('http://localhost:2048/report')
      .then(response => {
        this.setState({ data: response.data });
      });
  }

  render() {
    return (
      <div className="datagrid">
        <button type="button" onClick={() => this.loadData()}>refresh</button>
        <table>
          <thead><tr><th>name</th><th>invocations</th><th>totalDuration</th><th>average</th></tr></thead>
          <tbody>
            {this.state.data.map(datum =>
              <tr key={datum.name}><td>{datum.name}</td><td>{datum.invocations}</td><td>{datum.totalDuration}</td><td>{datum.average}</td></tr>
            )}
          </tbody>
        </table>
      </div>
    );
  }
}

export default App;

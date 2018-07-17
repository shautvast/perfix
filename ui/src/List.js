import React, { Component } from 'react';
import './App.css';
import axios from 'axios';

class List extends Component {
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
        <h1>List view</h1>
        <table>
          <thead><tr><th>name</th><th>invocations</th><th>totalDuration</th><th>average</th></tr></thead>
          <tbody>
            {this.state.data.map(datum =>
              <tr key={datum.name}><td>{datum.name}</td><td>{datum.invocations}</td><td>{Math.floor(datum.totalDuration/1000)/1000}</td><td>{Math.floor(datum.average/1000)/1000}</td></tr>
            )}
          </tbody>
        </table>
      </div>
    );
  }
}

export default List;

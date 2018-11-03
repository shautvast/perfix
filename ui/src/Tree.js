import React, { Component } from 'react';
import axios from 'axios';
import List from './List';

class Tree extends Component {
    state = { data: [] };

    componentDidMount() {
        this.loadData();
    }

    loadData() {
        console.log(this);
        axios.get("http://localhost:2048/callstack")
            .then(response => this.setState({ data: response.data }));
    }

    clearData() {
        axios.get("http://localhost:2048/clear")
            .then(response => this.setState({ data: response.data }));
    }

    renderChildren(children) {
        return (<ul className="tree">
            {children.map(
                r =>
                    <li key={r.name}><input type="checkbox" className="tree"/>
                        {Math.floor(r.invocation.duration / 1000) / 1000} ms &nbsp;
                        {r.name}
                        {this.renderChildren(r.children)}
                    </li>
            )}
        </ul>)
    }

    render() {
        return (
            <div>
                <button type="button" onClick={() => this.clearData()}>clear</button>
                <button type="button" onClick={() => this.loadData()}>refresh</button>
                <div className="view"><h1>Callstack view</h1>
                    <div className="treeView">
                        {this.renderChildren(this.state.data)}
                    </div>
                </div>
                <p/>
                <List />
            </div>
        )
    }
}

export default Tree;
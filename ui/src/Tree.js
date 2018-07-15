import React, { Component } from 'react';
import axios from 'axios';
import List from './List';

class Tree extends Component {
    state = { data: [] };

    componentDidMount() {
        this.loadData();
    }

    loadData() {
        axios.get("http://localhost:2048/callstack")
            .then(response => this.setState({ data: response.data }));
    }

    renderChildren(children) {
        return (<ul className="tree">
            {children.map(
                r =>
                    <li>
                        {Math.floor(r.report.average / 1000) / 1000} ms &nbsp;
                        - {r.report.invocations} inv. &nbsp;
                        {r.report.name}
                        {this.renderChildren(r.children)}
                    </li>
            )}
        </ul>)
    }

    render() {
        return (
            <div>
                <div className="view"><h1>Callstack view</h1>
                    {/* <button type="button" onClick={this.loadData()}> refresh </button> */}
                    {this.renderChildren(this.state.data)}
                </div>
                <p/>
                <List/>
            </div>
        )
    }
}

export default Tree;
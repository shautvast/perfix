import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import Tree from './Tree';
import registerServiceWorker from './registerServiceWorker';

ReactDOM.render(<Tree />, document.getElementById('root'));
registerServiceWorker();

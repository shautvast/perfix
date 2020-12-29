let callstack_data;
const datatree = document.getElementById("datatree");

function appendChildren(parent, nodes) {
    let node;

    for (let i = 0; i < nodes.length; i++) {
        node = nodes[i];
        let li = document.createElement("li");

        let branch = document.createElement("i");
        if (node.children.length > 0) {
            branch.setAttribute("class", "indicator glyphicon glyphicon-plus-sign");
            branch.onclick = function (event) {
                let icon = event.currentTarget.parentElement.firstChild;
                let ul_to_toggle = icon.nextSibling.nextSibling;

                if (ul_to_toggle.getAttribute("class") === "visible") {
                    icon.setAttribute("class", "indicator glyphicon glyphicon-plus-sign");
                    ul_to_toggle.setAttribute("class", "hidden");
                } else {
                    icon.setAttribute("class", "indicator glyphicon glyphicon-min-sign");
                    ul_to_toggle.setAttribute("class", "visible");
                }
            };
        }
        let label = document.createElement("a");
        label.innerText = Math.floor(node["invocation"].duration / 1000) / 1000 + " ms " + node.name;
        li.appendChild(branch);
        li.appendChild(label);

        let ul = document.createElement("ul");
        ul.setAttribute("class", "hidden");
        li.appendChild(ul);

        appendChildren(ul, node.children);

        parent.appendChild(li);
    }
}

function refresh_data_tree() {
    let new_div = document.createElement("div");
    new_div.setAttribute("class", "callstack-tree");
    datatree.appendChild(new_div);
    appendChildren(new_div, callstack_data);
}

(function main() {
    axios.get('http://localhost:2048/callstack')
        .then(response => {
            callstack_data = response.data;
            refresh_data_tree();
        });
}());


let name_filter = "";
let datatable, callstack_data;
const datagrid = document.getElementById("datagrid");
const datatree = document.getElementById("datatree");

function refresh_data_grid() {
    if (datagrid.firstChild) {
        datagrid.removeChild(datagrid.firstChild);
    }

    const table = append_table(datagrid);
    let row, name;
    table.thead().tr()
        .th("name")
        .th("invocations")
        .th("total duration")
        .th("average");
    const tbody = table.tbody();
    for (let i = 0; i < datatable.data.length; i++) {
        row = datatable.data[i];
        name = row.name;
        if (name_filter.length === 0 || name.includes(name_filter)) {
            tbody.tr()
                .td(name)
                .td(row["invocations"])
                .td(row["totalDuration"] / 1000000 + " ms")
                .td(row["average"] / 1000000 + " ms");
        }
    }
}

function update_filter() {
    name_filter = document.getElementById("name-filter").value;
    if (datatable) {
        refresh_data_grid();
    }
}

function append_table(parent) {
    const table = document.createElement("table");
    parent.appendChild(table);

    return {
        thead: function () {
            let thead = document.createElement("thead");
            table.appendChild(thead);
            return {
                tr: function () {
                    let new_tr = document.createElement("tr");
                    thead.appendChild(new_tr);

                    let th = function (text) {
                        let new_td = document.createElement("th");
                        new_td.innerText = text;
                        new_tr.appendChild(new_td);
                        return {
                            th
                        }
                    }
                    return {
                        th
                    }
                }
            }
        },
        tbody: function () {
            let tbody = document.createElement("tbody");
            table.appendChild(tbody);
            return {
                tr: function () {
                    let new_tr = document.createElement("tr");
                    tbody.appendChild(new_tr);

                    let td = function (text) {
                        let new_td = document.createElement("td");
                        new_td.innerText = text;
                        new_tr.appendChild(new_td);
                        return {
                            td
                        }
                    }
                    return {
                        td
                    }
                }
            }
        },
        tr: function () {
            let new_tr = document.createElement("tr");
            table.appendChild(new_tr);

            let td = function (text) {
                let new_td = document.createElement("td");
                new_td.innerText = text;
                new_tr.appendChild(new_td);
                return {
                    td
                }
            }
            return {
                td
            }
        }
    }
}

// (function tabular_view() {

// }());

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
    let datatree = document.getElementById("datatree");
    let new_div = document.createElement("div");
    new_div.setAttribute("class", "callstack-tree");
    datatree.appendChild(new_div);
    appendChildren(new_div, callstack_data);
}

(function main() {
    update_filter();

    axios.get('http://localhost:2048/report')
        .then(response => {
            datatable = response;
            refresh_data_grid();
        });

    axios.get('http://localhost:2048/callstack')
        .then(response => {
            callstack_data = response.data;
            refresh_data_tree();
        });
}());


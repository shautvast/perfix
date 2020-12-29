let name_filter = "";
let datatable;
const datagrid = document.getElementById("datagrid");

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

(function main() {
    update_filter();

    axios.get('http://localhost:2048/report')
        .then(response => {
            datatable = response;
            refresh_data_grid();
        });
}());


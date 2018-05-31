function tabulate(data, columns) {
    d3.select('table').remove();
    var table = d3.select('body').append('table')
    var thead = table.append('thead')
    var	tbody = table.append('tbody');

    thead.append('tr')
        .selectAll('th')
        .data(columns).enter()
        .append('th')
        .text(function (column) { return column; });

    var rows = tbody.selectAll('tr')
        .data(data)
        .enter()
        .append('tr');

    rows.selectAll('td')
        .data(function (row) {
            return columns.map(function (column) {
                return {column: column, value: row[column]};
            });
        })
        .enter()
        .append('td')
        .text(function (d) { return d.value; });

    return table;
}
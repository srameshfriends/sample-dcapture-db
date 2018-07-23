function LookupGrid(base, columns) {
    this.columns = columns;
    this.clearAll();
    this.listener = null;
}

LookupGrid.prototype.setTitle = function (titleArray) {

};

LookupGrid.prototype.clearAll = function () {
    $("#tableBody").empty();
    this.dataMap = new Map();
};
SimpleGrid.prototype.addCell = function (row, data, name) {
    let cell = $("<td></td>"), txt = data[name];
    if (typeof txt === "undefined" || txt === "null") {
        txt = "";
    }
    cell.text(txt);
    cell.click(function () {
        self.listener("actionClicked", data, name);
    });
    row.append(cell);
    return cell;
};
SimpleGrid.prototype.addRow = function (data) {
    let name, idx, row = $("<tr></tr>"), body = $("#tableBody");
    const rowId = "RID" + body.children().length;
    if(this.hasEditCell) {
        this.addEditCell(row, data);
    }
    for (idx = 0; idx < this.columns.length; idx++) {
        name = this.columns[idx];
        this.addCell(row, data, name);
    }
    if(this.hasDeleteCell) {
        this.addDeleteCell(row, data);
    }
    row.data("rowId", rowId);
    this.dataMap.set(rowId, data);
    body.append(row);
    return row;
};
SimpleGrid.prototype.setData = function (dataArray) {
    let idx, data;
    this.clearAll();
    if(!Array.isArray(dataArray)) {
        dataArray = [];
    }
    for (idx = 0; idx < dataArray.length; idx++) {
        data = dataArray[idx];
        this.addRow(data);
    }
};
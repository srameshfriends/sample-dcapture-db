function SimpleGrid(model, listener) {
    let self = this;
    const listId = model.name;
    self.columns = [];
    self.build = function () {
        let replacer, idx, id, type, name, txt, node, nodeArray;
        replacer = listId + ".";
        nodeArray = $("thead[data-id]");
        if (1 > nodeArray.length) {
            console.log(listId + " > html thead not found");
        }
        node = $(nodeArray[0]);
        if (1 > node.children().length) {
            console.log(listId + " > html thead first row not found");
        }
        self.header = node.children().eq(0);
        for (idx = 0; idx < self.header.children().length; idx++) {
            node = self.header.children().eq(idx);
            id = node.data("id");
            if (0 > id.indexOf(replacer)) {
                continue;
            }
            name = id.replace(replacer, "");
            if (0 < name.length) {
                self.columns.push(name);
            }
        }
        nodeArray = $("tbody[data-id]");
        if (1 > nodeArray.length) {
            throw listId + " > html tbody not found";
        }
        self.body = $(nodeArray[0]);

    };
    self.addEditCell = function (row, data) {
        let cell = $("<td><i class='fas fa-edit icon-blue'></i></td>");
        cell.click(function () {
            if ($.type(listener) === "function") {
                listener("actionEdit", model.name, data);
            }
        });
        row.append(cell);
        return cell;
    };
    self.addDeleteCell = function (row, data) {
        let cell = $("<td><i class='fas fa-trash icon-blue'></i></td>");
        cell.click(function () {
            if ($.type(listener) === "function") {
                listener("actionDelete", model.name, data);
            }
        });
        row.append(cell);
        return cell;
    };
    self.addCell = function (row, name, data) {
        let cell = $("<td></td>"), txt = data[name];
        if ($.type(txt) === "undefined") {
            txt = "";
        }
        cell.text(txt);
        cell.click(function () {
            if ($.type(listener) === "function") {
                listener("click", name, data);
            }
        });
        row.append(cell);
        return cell;
    };
    self.addRow = function (data) {
        let name, idx, row = $("<tr></tr>");
        const rowId = "RID" + self.body.children().length;
        for (idx = 0; idx < self.columns.length; idx++) {
            name = self.columns[idx];
            if ("actionEdit" === name) {
                self.addEditCell(row, data);
            } else if ("actionDelete" === name) {
                self.addDeleteCell(row, data);
            } else {
                self.addCell(row, name, data);
            }
        }
        row.data("rowId", rowId);
        self.dataMap.set(rowId, data);
        self.body.append(row);
        return row;
    };
    self.clearAll = function () {
        self.body.empty();
        self.dataMap = new Map();
    };
    self.setData = function (dataArray) {
        let idx, data;
        self.clearAll();
        if (!Array.isArray(dataArray)) {
            dataArray = [];
        }
        for (idx = 0; idx < dataArray.length; idx++) {
            data = dataArray[idx];
            self.addRow(data);
        }
    };
    self.build();
    self.clearAll();
}


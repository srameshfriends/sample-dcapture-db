function DataTable(model) {
    let self = this;
    self.listener = false;
    const listId = model.name;
    self.columns = [];
    self.onChange = function (callback) {
        self.listener = callback;
    };
    self.build = function () {
        let replacer, idx, id, dType, name, txt, node, nodeArray;
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
            dType = node.data("type");
            if (typeof  id !== "string") {
                id = "";
            }
            name = id.replace(replacer, "");
            let colModel = {name: name, type: dType};
            if(dType === "reference") {
                let columns = node.data("columns");
                colModel["columns"] = columns.split(" ");
            }
            self.columns.push(colModel);
        }
        nodeArray = $("tbody[data-id]");
        if (1 > nodeArray.length) {
            throw listId + " > html tbody not found";
        }
        self.body = $(nodeArray[0]);
    };
    self.getColumnByName = function (name) {
        for (let idx = 0; idx < self.columns.length; idx++) {
            let col = self.columns[idx];
            if (col["name"] === name) {
                return col;
            }
        }
        return false;
    };
    self.addEditCell = function (row, data) {
        let cell = $("<td><i class='fas fa-edit icon-blue'></i></td>");
        cell.on("click", function () {
            if (typeof self.listener === "function") {
                self.listener("actionEdit", model.name, data);
            }
        });
        row.append(cell);
        return cell;
    };
    self.addSelectableCell = function (row, data) {
        let rowId, cell = $("<td></td>");
        rowId = row.data("rowId");
        rowId = rowId.replace("RID", "");
        rowId = parseInt(rowId) + 1;
        cell.text(rowId);
        cell.addClass("pointer");
        cell.data("selected", "false");
        cell.on("click", function (evt) {
            evt.preventDefault();
            let selected = cell.data("selected");
            selected = "true" === selected ? "false" : "true";
            cell.data("selected", selected);
            row.data("selected", selected);
            if ("true" === selected) {
                row.addClass("row-selected");
            } else {
                row.removeClass("row-selected");
            }
            if (typeof self.listener === "function") {
                self.listener("changed", model.name, data);
            }
        });
        row.append(cell);
        return cell;
    };
    self.addDeleteCell = function (row, data) {
        let cell = $("<td><i class='fas fa-trash icon-blue'></i></td>");
        cell.on("click", function () {
            if (typeof self.listener === "function") {
                self.listener("actionDelete", model.name, data);
            }
        });
        row.append(cell);
        return cell;
    };
    self.addLinkCell = function (row, name, data) {
        let cell = $("<td></td>"), txt = data[name];
        if (typeof txt === "undefined") {
            txt = "";
        }
        cell.text(txt);
        cell.addClass("cell-link");
        cell.on("click", function () {
            if (typeof self.listener === "function") {
                self.listener("click", name, data);
            }
        });
        row.append(cell);
        return cell;
    };
    self.addReferenceCell = function (row, name, data) {
        let cell = $("<td></td>"), obj = data[name];
        row.append(cell);
        if(typeof obj === "undefined" || obj === null || typeof obj !== "object") {
            return cell;
        }
        let idx, txt, col, colName, values = [];
        col = self.getColumnByName(name);
        for (idx = 0; idx < col.columns.length; idx++) {
            colName = col.columns[idx];
            if(obj.hasOwnProperty(colName)) {
                txt = obj[colName];
                values.push(txt);
            }
        }
        cell.text(values.join(" - "));
        return cell;
    };
    self.addCell = function (row, name, data) {
        let cell = $("<td></td>"), txt = data[name];
        if ($.type(txt) === "undefined") {
            txt = "";
        }
        cell.text(txt);
        row.append(cell);
        return cell;
    };
    self.addIntCell = function (row, name, data) {
        let cell = $("<td></td>"), intValue, txt = data[name];
        cell.addClass("text-right");
        try {
            intValue = parseInt(txt);
        } catch (e) {
            intValue = 0;
        }
        if (0 === intValue) {
            cell.text("");
        } else {
            cell.text(intValue);
        }
        row.append(cell);
        return cell;
    };
    self.addDoubleCell = function (row, name, data) {
        let cell = $("<td></td>"), txt = data[name];
        cell.addClass("text-right");
        cell.text((typeof txt === "undefined") ? "" : txt);
        row.append(cell);
        return cell;
    };
    self.addEmptyCell = function (row) {
        let cell = $("<td></td>");
        row.append(cell);
        return cell;
    };
    self.addRow = function (data) {
        let model, name, dType, idx, row = $("<tr></tr>");
        const rowId = "RID" + self.body.children().length;
        row.data("rowId", rowId);
        for (idx = 0; idx < self.columns.length; idx++) {
            model = self.columns[idx];
            name = model['name'];
            dType = model['type'];
            if (typeof name !== 'string') {
                name = "";
            }
            if (typeof dType !== 'string') {
                dType = "text";
            }
            if ("selectable" === dType) {
                self.addSelectableCell(row, data);
            } else if ("text" === dType) {
                self.addCell(row, name, data);
            } else if ("reference" === dType) {
                self.addReferenceCell(row, name, data);
            } else if ("integer" === dType) {
                self.addIntCell(row, name, data);
            } else if ("double" === dType || "decimal" === dType) {
                self.addDoubleCell(row, name, data);
            } else if ("link" === dType) {
                self.addLinkCell(row, name, data);
            } else if ("edit" === dType) {
                self.addEditCell(row, data);
            } else if ("delete" === dType) {
                self.addDeleteCell(row, data);
            } else {
                self.addEmptyCell(row);
            }
        }
        self.dataMap.set(rowId, data);
        self.body.append(row);
        return row;
    };
    self.getSelected = function (dataArray) {
        let result, resultArray = [], idx, row, count = self.body.children().length;
        for (idx = 0; idx < count; idx++) {
            row = self.body.children().eq(idx);
            if ("true" === row.data("selected")) {
                result = self.dataMap.get(row.data("rowId"));
                if (typeof result === "object") {
                    resultArray.push(result);
                }
            }
        }
        return resultArray;
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


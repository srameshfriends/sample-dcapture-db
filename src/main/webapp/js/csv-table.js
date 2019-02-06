function CsvTable() {
    let self = this;
    self.listener = false;
    self.model = {};
    self.onChange = function (callback) {
        self.listener = callback;
    };
    self.build = function (model) {
        let idx, col, title, thEle;
        self.csvHeader = [];
        self.model = model;
        self.header = $("<thead></thead>");
        self.body = $("<tbody></tbody>");
        self.table = $("<table></table>");
        self.headerRow = $("<tr></tr>");
        self.table.prop("class", "table table-bordered");
        self.table.append(self.header);
        self.table.append(self.body);
        self.header.append(self.headerRow);
        model.base.append(self.table);
        thEle = $("<th></th>");
        thEle.css("width", "32px");
        self.headerRow.append(thEle);
        for (idx = 0; idx < model.columns.length; idx++) {
            col = model.columns[idx];
            thEle = $("<th></th>");
            thEle.prop("class", "pl-2");
            thEle.data("id", "");
            thEle.data("type", "text");
            thEle.data("locale", "");
            thEle.text("");
            if (typeof col.name === "string" || typeof col.id === "string") {
                thEle.data("id", col.name);
            }
            if (typeof col.type === "string") {
                thEle.data("type", col.type);
            }
            if (typeof col.locale === "string") {
                thEle.data("locale", col.locale);
                thEle.text(SessionDB.getLocale(col.locale));
            }
            if (typeof col.width === "string") {
                thEle.css("width", col.width);
            }
            self.headerRow.append(thEle);
        }
    };
    self.length = function () {
        return self.body.children().length;
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
    self.addSelectableCell = function (row, data) {
        let cell = $("<th class='text-secondary' scope='row'>&#9679;</th>");
        cell.prop("class", "pointer");
        cell.data("rowId", row.data("rowId"));
        cell.data("selected", "false");
        cell.on("click", function () {
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
                self.listener("changed", self.model.name, data);
            }
        });
        row.append(cell);
        return cell;
    };
    self.setFlag = function (type) {
        type = (typeof type !== "string") ? "text-secondary" : type.trim();
        if (typeof type === "string") {
            if ("primary" === type || "text-primary" === type) {
                type = "text-primary";
            } else if ("warning" === type || "text-warning" === type) {
                type = "text-warning";
            } else if ("success" === type || "text-success" === type) {
                type = "text-success";
            }
        }
        let row, node, idx;
        for (idx = 0; idx < self.length(); idx++) {
            row = self.body.children().eq(idx);
            node = row.children().eq(0);
            node.addClass(type);
        }
    };
    self.addDeleteCell = function (row, data) {
        let cell = $("<td><i class='fas fa-trash icon-blue'></i></td>");
        cell.on("click", function () {
            if (typeof self.listener === "function") {
                self.listener("actionDelete", self.model.name, data);
            }
        });
        row.append(cell);
        return cell;
    };
    self.addCell = function (row, text, hasRightAlign) {
        let cell = $("<td></td>"), fld = $("<input>");
        if ($.type(text) === "undefined") {
            text = "";
        }
        row.append(cell);
        cell.append(fld);
        fld.val(text);
        fld.prop("class", "border-0 pt-1 pb-1 pl-2 pr-2 w-100 bg-transparent");
        cell.prop("class", "p-0 pt-2");
        if (hasRightAlign) {
            fld.addClass("text-right");
            cell.addClass("text-right");
        }
        return cell;
    };
    self.addCsv = function (data, csvIndex) {
        let value, col, name, dType, idx, row = $("<tr></tr>");
        row.data("rowId", csvIndex);
        self.addSelectableCell(row, data);
        for (idx = 0; idx < self.model.columns.length; idx++) {
            value = "";
            if (data.length > idx) {
                self.addCell(row, data[idx], false);
            } else {
                self.addCell(row, "", false);
            }
        }
        self.body.append(row);
        return row;
    };
    self.isValidType = function (text, type, required) {
        if (typeof required === "boolean" && required) {
            if (type === "text") {
                return 0 < text.trim().length;
            } else if (type === "int" || type === "integer") {
                try {
                    let intVal = parseInt(text);
                    return 0 < intVal;
                } catch (e) {
                }
                return false;
            } else if (type === "decimal" || type === "double") {
                try {
                    let decimalVal = parseDecimal(text);
                    return 0 < decimalVal;
                } catch (e) {
                }
                return false;
            } else if (type === "boolean") {
                text = text.trim().toLowerCase();
                return text === "true" || text === "false";
            }
            console.log("CSV TABLE : type not implemented [" + type + "] \t " + text);
            return false;
        }
        if (type === "decimal" || type === "double") {
            try {
                parseDecimal(text);
                return true;
            } catch (e) {
            }
            return false;
        } else if (type === "int" || type === "integer") {
            try {
                parseInt(text);
                return true;
            } catch (e) {
            }
            return false;
        } else if (type === "boolean") {
            text = text.trim().toLowerCase();
            return text === "true" || text === "false";
        }
        return true;
    };
    self.validate = function () {
        if (0 === self.length()) {
            return false;
        }
        let colId, rIdx, col, type, row, ele, text, status = true;
        for (rIdx = 0; rIdx < self.length(); rIdx++) {
            row = self.body.children().eq(rIdx);
            for (colId = 0; colId < self.model.columns.length; colId++) {
                col = self.model.columns[colId];
                type = col.type;
                if (typeof type !== "string") {
                    type = "text";
                }
                ele = row.children().eq(colId + 1);
                text = ele.children().eq(0).val();
                console.log(colId + " - Type : " + type + " Value : " + text);
                let result = self.isValidType(text, type, col.required);
                if (result) {
                    ele.removeClass("bg-e3f5bb");
                } else {
                    ele.addClass("bg-e3f5bb");
                    status = false;
                }
            }
        }
        return status;
    };
    self.getRowValue = function (row) {
        let colId, ele, text, values = [];
        for (colId = 0; colId < self.model.columns.length; colId++) {
            ele = row.children().eq(colId + 1);
            text = ele.children().eq(0).val();
            values.push(text.trim());
        }
        return values;
    };
    self.getSelected = function () {
        let array = [], idx, row, count = self.length();
        for (idx = 0; idx < count; idx++) {
            row = self.body.children().eq(idx);
            if ("true" === row.data("selected")) {
                array.push(self.getRowValue(row));
            }
        }
        return array;
    };
    self.getRowId = function (row) {
        let rowId = row.data("rowId");
        try {
            return parseInt(rowId);
        } catch (e) {
        }
        return -1;
    };
    self.removeSelected = function () {
        let idx, row, isNextRecord = true, isVerifiedAll, result = [];
        if (0 < self.length()) {
            while (isNextRecord) {
                isVerifiedAll = true;
                for (idx = 0; idx < self.length(); idx++) {
                    row = self.body.children().eq(idx);
                    if ("true" === row.data("selected")) {
                        result.push(self.getRowId(row));
                        row.remove();
                        isVerifiedAll = false;
                        break;
                    }
                }
                if (isVerifiedAll || 0 === self.length()) {
                    isNextRecord = false;
                }
            }
        }
        return result;
    };
    self.getCsv = function (withHeader) {
        let array = [], idx, row, count = self.length();
        if (withHeader) {
            array.push(self.csvHeader);
        }
        for (idx = 0; idx < count; idx++) {
            row = self.body.children().eq(idx);
            array.push(self.getRowValue(row));
        }
        return array;
    };
    self.clear = function () {
        self.body.empty();
        self.csvHeader = [];
        return this;
    };
    self.setCsv = function (csvHeader, csvArray, start) {
        let idx, rowCount;
        self.clear();
        if (Array.isArray(csvHeader)) {
            self.csvHeader = csvHeader;
        }
        if (!Array.isArray(csvArray)) {
            csvArray = [];
        }
        if (typeof start !== "number") {
            start = 0;
        }
        for (idx = 0; idx < csvArray.length; idx++) {
            self.addCsv(csvArray[idx], start);
            start += 1;
        }
    };
}

function DataGrid(base, callback) {
    this.listener = callback;
    base.empty();
    this.table = $("<table></table>");
    this.table.prop("class", "table table-bordered");
    base.append(this.table);
    this.sortingOrderList = [];
}

DataGrid.prototype.pvtEmToPx = function (size) {
    let value, em = parseFloat($("body").css("font-size"));
    value = size * em;
    return parseInt(value);
};
DataGrid.prototype.pvtUniqueId = function () {
    return 'u' + Math.random().toString(36).substr(2, 16);
};
DataGrid.prototype.pvtRemoveSortingIcon = function (columnIndex) {
    let icon, node = this.headerRow.children().eq(columnIndex);
    icon = node.children().eq(1);
    icon.removeClass("la la-angle-up");
    icon.removeClass("la la-angle-down");
    node.data("sortOrder", "");
};
DataGrid.prototype.pvtOnColumnEvt = function (thNode) {
    let idx, item, sortOrder, icon = thNode.children().eq(1);
    const nodeIndex = thNode.index();
    const name = thNode.data("name");
    const orderBy = thNode.data("sortOrder");
    for (idx = 0; idx < this.sortingOrderList.length; idx++) {
        item = this.sortingOrderList[idx];
        if (name === item.name) {
            this.pvtRemoveSortingIcon(item.index);
            this.sortingOrderList.splice(idx, 1);
            break;
        }
    }
    // Sorting limit is two columns.
    if (1 < this.sortingOrderList.length) {
        item = this.sortingOrderList.pop();
        this.pvtRemoveSortingIcon(item.index);
    }
    icon.removeClass("la la-angle-down").removeClass("la la-angle-up");
    if (orderBy === "") {
        sortOrder = "asc";
        icon.addClass("la la-angle-down");
    } else if (orderBy === "asc") {
        sortOrder = "desc";
        icon.addClass("la la-angle-up");
    } else {
        sortOrder = "";
    }
    thNode.data("sortOrder", sortOrder);
    if (sortOrder !== "") {
        this.sortingOrderList.unshift({index: nodeIndex, name: name, order: sortOrder});
    }
    this.publishEvent("SortingOrder", name);
};
DataGrid.prototype.pvtAddHeadCell = function (model) {
    const self = this;
    let ele1 = $("<span></span>"), ele2 = $("<i></i>"), thNode = $("<th></th>");
    ele1.text(model.title);
    ele2.prop("class", "float-right");
    thNode.append(ele1);
    thNode.append(ele2);
    thNode.data("name", model.name);
    thNode.data("type", model.type);
    thNode.data("sortOrder", "");
    thNode.prop("name", model.name);
    thNode.prop("class", "pointer");
    thNode.click(function () {
        self.pvtOnColumnEvt(thNode);
    });
    this.headerRow.append(thNode);
    return thNode;
};
DataGrid.prototype.build = function (cfg) {
    this.columns = cfg.columns;
    this.required = cfg.required;
    let head = $("<thead></thead>");
    this.headerRow = $("<tr></tr>");
    this.body = $("<tbody></tbody>");
    head.append(this.headerRow);
    this.table.empty();
    this.table.append(head);
    this.table.append(this.body);
    this.table.data("name", cfg.name);
    this.table.data("type", cfg.type);
    this.columnsMap = new Map();
    let idx, model, colWidth, firstCol, headModel = {name: "ACTION_TO_ALL", title: "", type: "indexing"};
    firstCol = this.pvtAddHeadCell(headModel);
    colWidth = this.pvtEmToPx(4);
    firstCol.prop("width", colWidth + "px");
    let isValidFetchGroup = false;
    if (typeof cfg["fetchGroup"] !== "undefined") {
        const fgv = cfg.fetchGroup["auto"];
        if (typeof fgv !== "undefined" && Array.isArray(fgv)) {
            isValidFetchGroup = true;
        }
    }
    for (idx = 0; idx < this.columns.length; idx++) {
        model = this.columns[idx];
        this.columnsMap.set(model.name, model);
    }
    if (isValidFetchGroup) {
        let colIdxAry = cfg.fetchGroup.auto, colNameMap = new Map();
        for (idx = 0; idx < this.columns.length; idx++) {
            model = this.columns[idx];
            colNameMap.set(model.name, model);
        }
        for (idx = 0; idx < colIdxAry.length; idx++) {
            const colMdl = colNameMap.get(colIdxAry[idx]);
            if (typeof colMdl === "object") {
                this.pvtAddHeadCell(colMdl);
            }
        }
    } else {
        for (idx = 0; idx < this.columns.length; idx++) {
            this.pvtAddHeadCell(this.columns[idx]);
        }
    }
    this.clearAll();
    this.addEmptyRow();
};
DataGrid.prototype.publishEvent = function (name, obj) {
    if (typeof this.listener === "function") {
        this.listener(name, obj);
    }
};
DataGrid.prototype.pvtSetRowIndicator = function (actionId, row) {
    row.removeClass("text-primary", "text-danger");
    if ("Modified" === actionId || "Created" === actionId) {
        row.addClass("text-primary");
    } else if ("Invalid" === actionId) {
        row.addClass("text-danger");
    }
};
DataGrid.prototype.pvtOnCellBlurEvt = function (row) {
    const uid = row.data("uid");
    const oldObj = this.dataMap.get(uid), newObj = this.pvtRowData(row);
    const isEqualValue = this.isEqualRowObject(oldObj, newObj);
    const isNewRecord = this.newRecordSet.has(uid);
    if (isNewRecord === false) {
        if (isEqualValue) {
            this.modifiedSet.delete(uid);
            this.pvtSetRowIndicator("Reverted", row);
            this.publishEvent("Reverted", uid);
        } else {
            this.modifiedSet.add(uid);
            this.pvtSetRowIndicator("Modified", row);
            this.publishEvent("Modified", uid);
        }
    }
};
DataGrid.prototype.pvtOnKeyUpEvt = function (keyCode, row, tdNode) {
    if (13 === keyCode || 40 === keyCode) {
        this.pvtOn13o40KeyUpEvt(row, tdNode);
    } else if (37 === keyCode || 39 === keyCode) {
        this.pvtOn37o39KeyUpEvt(keyCode, row, tdNode);
    } else if (38 === keyCode) {
        this.pvtOn38KeyUpEvt(row, tdNode);
    }
};
DataGrid.prototype.pvtOn13o40KeyUpEvt = function (row, tdNode) {
    let temp, tdNext, rowCount = this.body.children().length, rowIdx = row.index();
    temp = rowCount - 2;
    if (rowIdx > temp) {
        this.addEmptyRow();
    }
    temp = this.body.children().eq(rowIdx + 1);
    tdNext = temp.children().eq(tdNode.index());
    tdNext.children().eq(0).focus();
};
DataGrid.prototype.pvtOn37o39KeyUpEvt = function (keyCode, row, tdNode) {
    const ele = tdNode.children().eq(0);
    if (0 < ele.val().length) {
        return true;
    }
    let tdIndex = tdNode.index();
    tdIndex = 37 === keyCode ? tdIndex - 1 : tdIndex + 1;
    if (0 < tdIndex && tdIndex < row.children().length) {
        let tdNode = row.children().eq(tdIndex);
        tdNode.children().eq(0).focus();
    }
    return true;
};
DataGrid.prototype.pvtOn38KeyUpEvt = function (row, tdNode) {
    const rowIdx = row.index() - 1;
    if (-1 < rowIdx) {
        let tdCelNode, tdIdx = tdNode.index(), trn = this.body.children().eq(rowIdx);
        tdCelNode = trn.children().eq(tdIdx);
        tdCelNode.children().eq(0).focus();
    }
};
DataGrid.prototype.pvtAddIndexCell = function (self, row, idx) {
    let cell = $("<td></td>");
    row.append(cell);
    row.data("selected", "false");
    cell.data("uid", row.data("uid"));
    cell.prop("class", "pointer text-bold");
    cell.text(idx);
    cell.click(function () {
        let evtId, selected = row.data("selected");
        if ("true" === selected) {
            selected = "false";
            row.removeClass("bg-eefdf3");
            evtId = "Deselected";
        } else {
            selected = "true";
            row.addClass("bg-eefdf3");
            evtId = "Selected";
        }
        row.data("selected", selected);
        self.publishEvent(evtId, row.data("uid"));
    });
};
DataGrid.prototype.pvtAddBoolCell = function (row, model) {
    const self = this;
    let tdNode = $("<td></td>"), cell = $("<input/>");
    row.append(tdNode);
    row.data("selected", "false");
    tdNode.append(cell);
    tdNode.data("type", model.type);
    tdNode.data("name", model.name);
    cell.prop("class", "border rounded-circle");
    cell.prop("type", "button");
    cell.data("value", "false");
    cell.css("width", "28px");
    cell.css("background-color", "transparent");
    cell.click(function (evt) {
        evt.preventDefault();
        if ("false" === cell.data("value")) {
            cell.data("value", "true");
            cell.css("background-color", "#A2FB9E");
        } else {
            cell.data("value", "false");
            cell.css("background-color", "transparent");
        }
        self.pvtOnCellBlurEvt(row);
    });
};
DataGrid.prototype.pvtAddObjCell = function (row, model) {
    const self = this;
    let tdNode = $("<td></td>"), cell = $("<input/>"), cell2;
    row.append(tdNode);
    row.data("selected", "false");
    tdNode.append(cell);
    tdNode.data("type", model.type);
    tdNode.data("name", model.name);
    tdNode.data("reference", model.reference);
    if(!Array.isArray(model.attributes)) {
        model.attributes = [];
    }
    if (1 < model.attributes.length) {
        cell2 = $("<input/>");
        tdNode.append(cell2);
        cell.prop("class", "bg-transparent border-0");
        cell2.prop("class", "bg-transparent border-0");
        cell2.blur(function () {
            cell2.attr('size', cell2.val().length);
            self.pvtOnCellBlurEvt(row);
        });
        cell2.keyup(function (evt) {
            self.pvtOnKeyUpEvt(evt.keyCode, row, tdNode);
        });
    } else {
        cell.prop("class", "bg-transparent border-0 w-100");
    }
    cell.blur(function () {
        cell.attr('size', cell.val().length);
        self.pvtOnCellBlurEvt(row);
    });
    cell.keyup(function (evt) {
        self.pvtOnKeyUpEvt(evt.keyCode, row, tdNode);
    });
};
DataGrid.prototype.pvtAddCell = function (row, model) {
    const self = this;
    let tdNode = $("<td></td>"), cell = $("<input/>");
    row.append(tdNode);
    row.data("selected", "false");
    tdNode.append(cell);
    tdNode.data("type", model.type);
    tdNode.data("name", model.name);
    if ("password" === model.type) {
        cell.prop("type", "password");
    }
    cell.prop("class", "bg-transparent border-0 w-100");
    cell.blur(function () {
        self.pvtOnCellBlurEvt(row);
    });
    cell.keyup(function (evt) {
        self.pvtOnKeyUpEvt(evt.keyCode, row, tdNode);
    });
};
DataGrid.prototype.pvtAddRow = function () {
    let idx, model, prop, row = $("<tr></tr>");
    this.body.append(row);
    row.data("uid", this.pvtUniqueId());
    this.pvtAddIndexCell(this, row, this.body.children().length);
    for (idx = 0; idx < this.columns.length; idx++) {
        model = this.columns[idx];
        if ("object" === model.type) {
            this.pvtAddObjCell(row, model);
        } else if ("boolean" === model.type) {
            this.pvtAddBoolCell(row, model);
        } else {
            this.pvtAddCell(row, model);
        }
    }
    return row;
};
DataGrid.prototype.addEmptyRow = function () {
    let row = this.pvtAddRow();
    this.newRecordSet.add(row.data("uid"));
};
DataGrid.prototype.pvtSetRowData = function (row, obj) {
    let uid, model, name, value, idx, cell, tdNode, count = row.children().length;
    for (idx = 1; idx < count; idx++) {
        tdNode = row.children().eq(idx);
        name = tdNode.data("name");
        model = this.columnsMap.get(name);
        value = obj[name];
        cell = tdNode.children().eq(0);
        if (value === undefined || value === null) {
            value = "";
        }
        if ("object" === model.type) {
            let cdx, values = obj[name], itemName, itemValue, max;
            if (!Array.isArray(model.attributes)) {
                model.attributes = [];
            }
            if(values === "undefined" || values === null) {
                values = {};
            }
            if (1 < model.attributes.length) {
                for (cdx = 0; cdx < 2; cdx++) {
                    itemValue = "";
                    if (cdx < model.attributes.length) {
                        itemName = model.attributes[cdx];
                        itemValue = values[itemName];
                        if (itemValue === undefined || itemName === null) {
                            itemValue = "";
                        }
                    }
                    cell = tdNode.children().eq(cdx);
                    cell.val(itemValue);
                    cell.attr('size', 3 > itemValue.length ? 3 : itemValue.length);
                }
            } else {
                cell = tdNode.children().eq(0);
                cell.val("");
                cell.attr('size', 3);
            }
        } else if ("boolean" === model.type) {
            if (value === true || "true" === value) {
                cell.data("value", "true");
                cell.css("background-color", "#A2FB9E");
            } else {
                cell.data("value", "false");
                cell.css("background-color", "transparent");
            }
        } else {
            cell.val(value);
        }
    }
    uid = row.data("uid");
    this.dataMap.set(uid, obj);
};
DataGrid.prototype.isEqualRowObject = function (oldObj, newObj) {
    if (typeof oldObj === "undefined" || typeof newObj === "undefined") {
        return false;
    }
    let value1, value2, model;
    for (let idx = 0; idx < this.columns.length; idx++) {
        model = this.columns[idx];
        value1 = oldObj[model.name];
        value2 = newObj[model.name];
        if (value1 !== value2) {
            return false;
        }
    }
    return true;
};
DataGrid.prototype.pvtIsValidReqFld = function (data) {
    let result = true;
    for (let idx = 0; idx < this.required.length; idx++) {
        let fldName = this.required[idx];
        if (data.hasOwnProperty(fldName)) {
            let value = data[fldName];
            if (typeof value === "string") {
                if ("" === value) {
                    result = false;
                }
            }
        } else {
            result = false;
        }
    }
    return result;
};
DataGrid.prototype.getSortingOrder = function () {
    if (0 === this.sortingOrderList.length) {
        return [];
    }
    let idx, item, result = [];
    for (idx = 0; idx < this.sortingOrderList.length; idx++) {
        item = this.sortingOrderList[idx];
        result[idx] = item.name + " " + item.order;
    }
    return result;
};
DataGrid.prototype.getNewRecords = function () {
    if (1 > this.newRecordSet.size) {
        return [];
    }
    let idx, resultList = [];
    for (idx = 0; idx < this.body.children().length; idx++) {
        let uid, row = this.body.children().eq(idx);
        uid = row.data("uid");
        if (this.newRecordSet.has(uid)) {
            let result = this.pvtRowData(row);
            if (this.pvtIsValidReqFld(result)) {
                resultList.push(result);
            }
        }
    }
    return resultList;
};
DataGrid.prototype.getModified = function () {
    let idx, resultList = [];
    for (idx = 0; idx < this.body.children().length; idx++) {
        let uid, row = this.body.children().eq(idx);
        uid = row.data("uid");
        if (this.modifiedSet.has(uid)) {
            let oldObj, mfd = this.pvtRowData(row);
            oldObj = this.dataMap.get(uid);
            if (!this.isEqualRowObject(oldObj, mfd)) {
                resultList.push(this.pvtRowData(row));
            }
        }
    }
    return resultList;
};
DataGrid.prototype.getSelected = function () {
    let idx, resultArray = [];
    for (idx = 0; idx < this.body.children().length; idx++) {
        let selected, row = this.body.children().eq(idx);
        selected = row.data("selected");
        if ("true" === selected) {
            resultArray.push(this.pvtRowData(row));
        }
    }
    return resultArray;
};
DataGrid.prototype.getSelectedIds = function () {
    let idx, rowIds = [];
    for (idx = 0; idx < this.body.children().length; idx++) {
        let selected, ele = this.body.children().eq(idx);
        selected = ele.data("selected");
        if ("true" === selected) {
            rowIds.push(ele.data("uid"));
        }
    }
    return rowIds;
};
DataGrid.prototype.pvtUpdateRowIndex = function () {
    for (let idx = 0; idx < this.body.children().length; idx++) {
        let tdFirst, row = this.body.children().eq(idx);
        tdFirst = row.children().eq(0);
        tdFirst.text(idx + 1);
    }
};
DataGrid.prototype.removeSelected = function () {
    let resultList = this.getSelected();
    for (let idx = 0; idx < this.body.children().length; idx++) {
        let selected, row = this.body.children().eq(idx);
        selected = row.data("selected");
        if ("true" === selected) {
            row.remove();
        }
    }
    if (0 === this.body.children().length) {
        this.addEmptyRow();
    } else {
        this.pvtUpdateRowIndex();
    }
    return resultList;
};
DataGrid.prototype.clearAll = function () {
    this.body.empty();
    this.dataMap = new Map();
    this.modifiedSet = new Set();
    this.newRecordSet = new Set();
};
DataGrid.prototype.pvtToValue = function (model, tdNode, oldValue) {
    let type = model.type, cell = tdNode.children().eq(0);
    if ("string" === type) {
        return cell.val();
    } else if ("double" === type) {
        try {
            return parseFloat(cell.val().trim());
        } catch (ex) {
        }
        return 0.0;
    } else if ("integer" === type) {
        try {
            return parseInt(cell.val().trim());
        } catch (ex) {
        }
        return 0;
    } else if ("object" === type) {
        if (!Array.isArray(model.attributes)) {
            model.attributes = [];
        }
        let refName, objValue = oldValue, idx;
        if (objValue === undefined || objValue === null) {
            objValue = {};
        }
        for (idx = 0; idx < model.attributes.length; idx++) {
            if (idx < tdNode.children().length) {
                refName = model.attributes[idx];
                cell = tdNode.children().eq(idx);
                if (typeof refName === "string") {
                    objValue[refName] = cell.val();
                }
            }
        }
        return objValue;
    } else if ("boolean" === type) {
        return cell.data("value") === "true";
    }
    return cell.val().trim();
};
DataGrid.prototype.pvtRowData = function (row) {
    let idx, model, name, tdNode, old, result = {};
    old = this.dataMap.get(row.data("uid"));
    if (typeof old === "object") {
        for (let key in old) {
            if (old.hasOwnProperty(key)) {
                result[key] = old[key];
            }
        }
    } else {
        old = {};
    }
    for (idx = 1; idx < row.children().length; idx++) {
        tdNode = row.children().eq(idx);
        name = tdNode.data("name");
        model = this.columnsMap.get(name);
        result[name] = this.pvtToValue(model, tdNode, old[name]);
    }
    return result;
};
DataGrid.prototype.setData = function (data) {
    this.clearAll();
    if (!Array.isArray(data) || 0 === data.length) {
        this.addEmptyRow();
        return;
    }
    for (let idx = 0; idx < data.length; idx++) {
        this.pvtSetRowData(this.pvtAddRow(), data[idx]);
    }
};

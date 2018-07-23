function TextField(name, node, listener) {
    let self = this;
    self.getValue = function () {
        return node.val();
    };
    self.setValue = function (fValue) {
        if (fValue === undefined || fValue === null) {
            fValue = "";
        }
        node.val(fValue);
        self.oldValue = fValue.trim();
    };
    self.revert = function () {
        node.val(self.oldValue);
    };
    node.keyup(function (evt) {
        if ($.type(listener) === "function") {
            if (self.oldValue !== node.val().trim()) {
                listener(evt, "changed", name);
            }
        }
    });
    node.focus(function () {
        node.select();
    });
    self.isValid = function () {
        return 0 < node.val().trim().length;
    };
    self.oldValue = node.val().trim();
}

function HiddenField(name, node) {
    let self = this;
    self.data = node.val();
    self.getValue = function () {
        return self.data;
    };
    self.setValue = function (data) {
        if (data === undefined || data === null) {
            data = "";
        }
        self.data = data;
        if ($.type(data) === "object" || $.type(data) === "array") {
            node.val(JSON.stringify(data));
        } else {
            node.val(data);
        }
    };
}

function NumberField(name, node, listener) {
    let self = this;
    self.parseNumber = function (text) {
        if (text === undefined || text === null) {
            return 0;
        }
        let obj = parseInt(text);
        if (isNaN(obj)) {
            self.validText = false;
            obj = 0;
        } else {
            self.validText = true;
        }
        return obj;
    };
    self.getValue = function () {
        return node.val();
    };
    self.setValue = function (fValue) {
        let obj = self.parseNumber(fValue);
        node.val(obj.toString());
        self.oldValue = obj;
    };
    self.revert = function () {
        node.val(self.oldValue.toString());
    };
    node.keyup(function (evt) {
        let newValue = self.parseNumber(node.val());
        if ($.type(listener) === "function") {
            if (self.validText && self.oldValue !== newValue) {
                listener(evt, "changed", name);
            }
        }
    });
    node.focus(function () {
        node.select();
    });
    node.blur(function () {
        let newValue = self.parseNumber(node.val());
        if (!self.validText) {
            node.val(newValue.toString());
            self.validText = true;
        }
    });
    self.isValid = function () {
        return self.validText;
    };
    self.oldValue = self.parseNumber(node.val());
}

function DecimalField(name, node, listener) {
    let self = this;
    self.parseDecimal = function (text) {
        if (text === undefined || text === null) {
            return 0;
        }
        let obj = parseFloat(text);
        if (isNaN(obj)) {
            self.validText = false;
            obj = 0;
        } else {
            self.validText = true;
        }
        return obj;
    };
    self.getValue = function () {
        return node.val();
    };
    self.setValue = function (fValue) {
        let obj = self.parseDecimal(fValue);
        node.val(obj.toString());
        self.oldValue = obj;
    };
    self.revert = function () {
        node.val(self.oldValue.toString());
    };
    node.keyup(function (evt) {
        let newValue = self.parseDecimal(node.val());
        if ($.type(listener) === "function") {
            if (self.validText && self.oldValue !== newValue) {
                listener(evt, "changed", name);
            }
        }
    });
    node.focus(function () {
        node.select();
    });
    node.blur(function () {
        let newValue = self.parseDecimal(node.val());
        if (!self.validText) {
            node.val(newValue.toString());
            self.validText = true;
        }
    });
    self.isValid = function () {
        return self.validText;
    };
    self.oldValue = self.parseDecimal(node.val());
}

function DateField(name, node, listener) {
    let self = this;
    self.getValue = function () {
        return node.val();
    };
    self.setValue = function (fValue) {
        if (fValue === undefined || fValue === null) {
            fValue = "";
        }
        node.val(fValue);
        self.oldValue = fValue.trim();
    };
    self.revert = function () {
        node.val(self.oldValue);
    };
    node.keyup(function (evt) {
        if ($.type(listener) === "function") {
            if (self.oldValue !== node.val().trim()) {
                listener(evt, "changed", name);
            }
        }
    });
    node.focus(function () {
        node.select();
    });
    self.isValid = function () {
        return 0 < node.val().trim().length;
    };
    self.oldValue = node.val().trim();
}

function LookupField(cfg, listener) {
    let self = this, div = [], fields = [], table = [], searchDelay = new CallFuture(1200);
    div[0] = $("<div></div>");
    div[1] = $("<div></div>");
    div[2] = $("<div></div>");
    div[3] = $("<div></div>");
    div[4] = $("<div></div>");
    div[5] = $("<div></div>");
    div[6] = $("<div></div>");
    div[7] = $("<div></div>");
    div[0].append(div[1]);
    div[1].append(div[2]);
    div[2].append(div[3]);
    div[2].append(div[4]);
    div[3].append(div[5]);
    div[3].append(div[6]);
    div[3].append(div[7]);
    $("body").append(div[0]);
    div[0].prop("class", "modal fade");
    div[1].prop("class", "modal-dialog modal-sm");
    div[2].prop("class", "modal-content");
    div[3].prop("class", "modal-body");
    div[4].prop("class", "modal-body");
    div[4].css("overflow-y", "scroll");
    div[4].css("max-height", "400px");
    div[5].prop("class", "w-100");
    div[6].prop("class", "w-100 mt-3");
    div[7].prop("class", "w-100 mt-3");
    div[0].prop("role", "dialog");
    div[0].prop("aria-hidden", "true");
    div[1].prop("role", "document");
    div[0].prop("id", "lookup-dialog");
    fields[0] = $("<input>");
    fields[1] = $("<button></button>");
    fields[2] = $("<i></i>");
    fields[3] = $("<button></button>");
    fields[4] = $("<i></i>");
    fields[5] = $("<button></button>");
    fields[6] = $("<i></i>");
    fields[7] = $("<label></label>");
    fields[8] = $("<button></button>");
    fields[9] = $("<i></i>");
    fields[10] = $("<button></button>");
    table[0] = $("<table></table>");
    table[1] = $("<thead></thead>");
    table[2] = $("<tr></tr>");
    table[3] = $("<tbody></tbody>");
    fields[0].prop("class", "form-control d-inline col-8 mr-sm-2");
    fields[0].prop("placeholder", "Search");
    fields[1].prop("class", "btn btn-outline-info my-2 my-sm-0");
    fields[2].prop("class", "fas fa-search icon-blue");
    fields[3].prop("class", "btn btn-outline-info my-2 my-sm-0");
    fields[4].prop("class", "fas fa-chevron-left icon-blue");
    fields[5].prop("class", "btn btn-outline-info my-2 my-sm-0 ml-3");
    fields[6].prop("class", "fas fa-chevron-right icon-blue");
    fields[7].prop("class", "btn  ml-2");
    fields[8].prop("class", "close float-right");
    fields[8].data("dismiss", "model");
    fields[8].prop("aria-label", "Close");
    fields[9].prop("class", "fas fa-times icon-blue");
    fields[10].prop("class", "btn btn-link pointer text-left");
    if ($.type(cfg.deselect) === "undefined") {
        cfg.deselect = "Clear Selected Value";
    }
    fields[10].text(cfg.deselect);
    div[5].append(fields[0]);
    div[5].append(fields[1]);
    div[5].append(fields[8]);
    fields[1].append(fields[2]);
    div[6].append(fields[3]);
    fields[3].append(fields[4]);
    div[6].append(fields[5]);
    fields[5].append(fields[6]);
    div[6].append(fields[7]);
    fields[8].append(fields[9]);
    div[7].append(fields[10]);
    table[0].prop("class", "table table-borderless");
    div[4].append(table[0]);
    table[0].append(table[1]);
    table[1].append(table[2]);
    table[0].append(table[3]);
    self.paging = {};
    self.paging.totalRecords = 0;
    self.paging.start = 0;
    self.paging.limit = 10;
    self.paging.isPrevious = false;
    self.paging.isNext = false;
    self.selected = {};
    self.addColumns = function () {
        let colObj, ci;
        for (ci = 0; ci < cfg.columns.length; ci++) {
            colObj = cfg.columns[ci];
            let item = $("<th></th>");
            item.data("name", colObj.name);
            item.text(colObj.title);
            table[2].append(item);
        }
    };
    self.setValue = function (data) {
        if ($.type(data) !== "object") {
            data = {};
        }
        let cdx, mdl, text, display = "";
        for (cdx = 0; cdx < cfg.columns.length; cdx++) {
            mdl = cfg.columns[cdx];
            text = data[mdl.name];
            if ($.type(text) === "string") {
                display = display + " - " + text;
            }
        }
        self.selected = data;
        if (3 < display.length) {
            display = display.substring(3, display.length);
        }
        cfg.field.val(display);
    };
    self.getValue = function () {
        return self.selected;
    };
    self.change = function (cell, data) {
        cell.click(function (evt) {
            div[0].modal('hide');
            self.setValue(data);
            if ($.type(listener) === "function") {
                listener(evt, "changed", cfg.name);
            }
        });
    };
    self.setPaging = function (obj) {
        let startIdx = obj.start + 1, endIdx = obj.start + obj.length, currentCount;
        self.paging.isPrevious = true;
        self.paging.isNext = true;
        if (0 === obj.length && 0 === obj.totalRecords) {
            fields[7].text(" - ");
        } else {
            fields[7].text(startIdx + " - " + endIdx + " of " + obj.totalRecords);
        }
        if (0 === obj.start) {
            self.paging.isPrevious = false;
        }
        currentCount = obj.start + obj.length;
        if (obj.totalRecords <= currentCount) {
            self.paging.isNext = false;
        }
    };
    self.setData = function (dataArray) {
        if (!Array.isArray(dataArray)) {
            dataArray = [];
        }
        table[3].empty();
        let mdl, ix;
        for (ix = 0; ix < dataArray.length; ix++) {
            let col, cdx, text, row = $("<tr></tr>"), data = dataArray[ix];
            table[3].append(row);
            for (cdx = 0; cdx < cfg.columns.length; cdx++) {
                mdl = cfg.columns[cdx];
                text = data[mdl.name];
                if (text === null || text === undefined) {
                    text = "";
                }
                col = $("<td></td>").text(text);
                col.prop("class", "btn-link pointer");
                row.append(col);
                self.change(col, data);
            }
        }
    };
    self.getRequest = function (start) {
        self.paging.start = 0 > start ? 0 : start;
        let req = {};
        req.name = cfg.name;
        req.start = start;
        req.limit = self.paging.limit;
        req.searchText = fields[0].val().trim();
        return req;
    };
    self.search = function (req) {
        $.ajax({
            type: "POST",
            dataType: "json",
            url: cfg.url,
            data: JSON.stringify(req),
            error: function (err) {
                new ShowMessageDialog(err.responseText);
            },
            success: function (data) {
                const model = cfg.name;
                self.setData(data[model]);
                self.setPaging(data);
            }
        });
    };

    fields[1].click(function () {
        self.search(self.getRequest(0));
    });
    fields[3].click(function () {
        if (self.paging.isPrevious) {
            let req = self.getRequest(self.paging.start - self.paging.limit);
            self.search(req);
        }
    });
    fields[5].click(function () {
        if (self.paging.isNext) {
            let req = self.getRequest(self.paging.start + self.paging.limit);
            self.search(req);
        }
    });
    fields[0].keyup(function (evt) {
        searchDelay.cancelAll();
        if (13 === evt.keyCode) {
            self.search(self.getRequest(0));
        } else {
            searchDelay.call(function () {
                self.search(self.getRequest(0));
            });
        }
    });
    fields[10].click(function (evt) {
        div[0].modal('hide');
        self.setValue({});
        if ($.type(listener) === "function") {
            listener(evt, "changed", cfg.name);
        }
    });
    fields[8].click(function () {
        div[0].modal('hide');
    });
    cfg.lookupField.click(function () {
        div[0].modal('show');
    });
    self.addColumns();
    self.search(self.getRequest(0));
}

function FormBinder(model, listener) {
    let self = this;
    const formId = model.name;
    self.fieldMap = new Map();
    self.loadElements = function () {
        let frmReplace, idx, id, type, name, txt, node, nodeArray;
        frmReplace = formId + ".";
        nodeArray = $("form[data-id]");
        for (idx = 0; idx < nodeArray.length; idx++) {
            node = nodeArray[idx];
            id = node.getAttribute("data-id");
            if (id === formId) {
                self.form = $(node);
                break;
            }
        }
        if(self.form === undefined) {
            console.log(formId + " > not found valid form element");
        }
        let lookupMap = new Map();
        nodeArray = $("[data-lookup]");
        for (idx = 0; idx < nodeArray.length; idx++) {
            node = nodeArray[idx];
            id = node.getAttribute("data-lookup");
            name = id.replace(frmReplace, "");
            if (id.indexOf(frmReplace) !== -1) {
                lookupMap.set(name, $(node));
            }
        }
        nodeArray = $("[data-id]");
        for (idx = 0; idx < nodeArray.length; idx++) {
            node = nodeArray[idx];
            id = node.getAttribute("data-id");
            if (0 > id.indexOf(frmReplace)) {
                continue;
            }
            type = node.getAttribute("data-type");
            if(type === undefined || type === null) {
                console.log("data type not valid for " + id);
                continue;
            }
            name = id.replace(frmReplace, "");
            if (type === "text") {
                self.fieldMap.set(name, new TextField(name, $(node), listener));
            } else if (type === "reference") {
                let cfg = model[name];
                cfg.field = $(node);
                cfg.lookupField = lookupMap.get(name);
                self.fieldMap.set(name, new LookupField(cfg, listener));
            } else if (type === "date") {
                self.fieldMap.set(name, new DateField(name, $(node)));
            } else if (type === "hidden") {
                self.fieldMap.set(name, new HiddenField(name, $(node)));
            } else if (type === "number") {
                self.fieldMap.set(name, new NumberField(name, $(node)));
            } else if (type === "double") {
                self.fieldMap.set(name, new DecimalField(name, $(node)));
            }
        }
    };
    self.setValue = function (data) {
        if (data === undefined || data === null || typeof data !== "object") {
            data = {};
        }
        let obj, entry;
        for (entry of self.fieldMap.entries()) {
            obj = data[entry[0]];
            entry[1].setValue(obj);
        }
    };
    self.getValue = function () {
        let entry, data = {};
        for (entry of self.fieldMap.entries()) {
            data[entry[0]] = entry[1].getValue();
        }
        return data;
    };
    self.addEvents = function () {
        self.form.on("submit", function (evt) {
            evt.preventDefault();
            listener(evt, "submit", formId);
        });
    };
    self.loadElements();
    self.addEvents();
}
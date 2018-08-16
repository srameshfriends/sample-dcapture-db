function TextField(name, node, listener) {
    let self = this;
    self.field = node;
    self.listener = listener;
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
    self.field = node;
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
    self.field = node;
    self.listener = listener;
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
    self.field = node;
    self.listener = listener;
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
    self.field = node;
    self.listener = listener;
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
    let self = this;
    self.attached = function () {
        let dlg = $("<div class='modal'  tabindex='-1' role='dialog' " +
            " style='overflow: hidden;min-width: 24rem;width: 30rem;max-width: 60rem;'>" +
            "<div class='card' style='margin: auto;float: none;'>" +
            "<div class='card-header' style='padding: 0.5rem 1.0rem;'>" +
            "<button class='btn btn-outline-light float-right pr-0'>" +
            "<i class='float-right fas fa-times icon-blue'></i></button>" +
            "<div class='form-inline'><input placeholder='Search' class='form-control' type='text'>" +
            "<div class='btn-group'><button class='btn btn-outline-secondary' type='button'>" +
            "<i class='fas fa-chevron-left icon-blue'></i></button>" +
            "<button class='btn btn-outline-secondary' type='button'>" +
            "<i class='fas  fa-chevron-right icon-blue'></i></button></div>" +
            "<button class='btn btn-outline-secondary ml-4'>" +
            "<i class='fas  fa-minus icon-blue'></i></button></div></div>" +
            "<div class='card-body' style='overflow: scroll; height:24rem;min-height: 24rem;max-height: 60rem;'>" +
            "<table class='table'><thead><tr></tr></thead><tbody></tbody></table></div></div></div>");
        cfg.field.parent().append(dlg);
        return dlg;
    };
    self.paging = {totalRecords: 0, start: 0, limit: 10, isPrevious: false, isNext: false, selected: {}};
    self.searchDelay = new CallFuture(1200);
    self.field = cfg.field;
    self.listener = listener;
    self.base = self.attached();
    self.card = self.base.children().eq(0);
    self.cardHead = self.card.children().eq(0);
    self.cardBody = self.card.children().eq(1);
    self.table = self.cardBody.children().eq(0);
    self.tHead = self.table.children().eq(0);
    self.tBody = self.table.children().eq(1);
    self.closeBtn = self.cardHead.children().eq(0);
    self.formDiv = self.cardHead.children().eq(1);
    self.searchFld = self.formDiv.children().eq(0);
    self.groupDiv = self.formDiv.children().eq(1);
    self.clearBtn = self.formDiv.children().eq(2);
    self.previousBtn = self.groupDiv.children().eq(0);
    self.nextBtn = self.groupDiv.children().eq(1);
    self.clearBtn.on("click", function (evt) {
        evt.preventDefault();
        self.base.modal('hide');
        self.setValue({});
        if (typeof listener === "function") {
            listener(evt, "changed", cfg.name);
        }
    });
    self.addColumns = function () {
        let colObj, ci, tHeadRow = self.tHead.children().eq(0);
        for (ci = 0; ci < cfg.columns.length; ci++) {
            colObj = cfg.columns[ci];
            let item = $("<th></th>");
            item.data("name", colObj.name);
            item.text(colObj.title);
            tHeadRow.append(item);
        }
    };
    self.setValue = function (data) {
        if (typeof  data !== "object") {
            data = {};
        }
        let cdx, mdl, text, display = "";
        for (cdx = 0; cdx < cfg.columns.length; cdx++) {
            mdl = cfg.columns[cdx];
            text = data[mdl.name];
            if (typeof text === "string") {
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
        cell.on("click", function (evt) {
            evt.preventDefault();
            self.base.modal("hide");
            self.setValue(data);
            if (typeof listener === "function") {
                listener(evt, "changed", cfg.name);
            }
        });
    };
    self.setPaging = function (obj) {
        let startIdx = obj.start + 1, endIdx = obj.start + obj.length, currentCount;
        self.paging.isPrevious = true;
        self.paging.isNext = true;
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
        let mdl, ix;
        self.tBody.empty();
        for (ix = 0; ix < dataArray.length; ix++) {
            let cdx, text, row = $("<tr></tr>"), data = dataArray[ix];
            self.tBody.append(row);
            for (cdx = 0; cdx < cfg.columns.length; cdx++) {
                mdl = cfg.columns[cdx];
                text = data[mdl.name];
                if (text === null || text === undefined) {
                    text = "";
                }
                let col = $("<td></td>").text(text);
                col.prop("class", "pointer overlay");
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
        req.searchText = self.searchFld.val().trim();
        return req;
    };
    self.search = function (req) {
        $.ajax({
            type: "POST",
            dataType: "json",
            url: cfg.url,
            data: JSON.stringify(req),
            error: function (err) {
                MessageDialog.show(err.responseText);
            },
            success: function (data) {
                const model = cfg.name;
                self.setData(data[model]);
                self.setPaging(data);
            }
        });
    };
    self.previousBtn.on("click", function (evt) {
        evt.preventDefault();
        if (self.paging.isPrevious) {
            let req = self.getRequest(self.paging.start - self.paging.limit);
            self.search(req);
        }
    });
    self.nextBtn.on("click", function (evt) {
        evt.preventDefault();
        if (self.paging.isNext) {
            let req = self.getRequest(self.paging.start + self.paging.limit);
            self.search(req);
        }
    });
    self.searchFld.on("click", function (evt) {
        self.searchDelay.cancelAll();
        if (13 === evt.keyCode) {
            self.search(self.getRequest(0));
        } else {
            self.searchDelay.call(function () {
                self.search(self.getRequest(0));
            });
        }
    });
    self.clearBtn.on("click", function (evt) {
        evt.preventDefault();
        self.setValue({});
        if (typeof listener === "function") {
            listener(evt, "changed", cfg.name);
        }
        self.base.modal('hide');
    });
    self.closeBtn.on("click", function (evt) {
        evt.preventDefault();
        self.base.modal('hide');
    });
    cfg.lookupField.on("click", function () {
        let pos = self.field.offset();
        self.base.css("top", 60);
        self.base.css("left", pos.left);
        self.base.modal('show');
        $(".modal-backdrop").on("click", function (evt) {
            evt.preventDefault();
            $(".modal-backdrop").off();
            self.base.modal('hide');
        });
    });
    self.addColumns();
    self.search(self.getRequest(0));
    self.base.modal('hide');
}

function FormBinder(model) {
    let self = this;
    self.listener = false;
    const formId = model.name;
    self.fieldMap = new Map();
    self.submit = function(callback) {
        self.listener = callback;
    };
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
        if (self.form === undefined) {
            throw formId + " > not found valid form element";
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
            if (type === undefined || type === null) {
                console.log("data type not valid for " + id);
                continue;
            }
            name = id.replace(frmReplace, "");
            if (type === "text") {
                self.fieldMap.set(name, new TextField(name, $(node), self.listener));
            } else if (type === "reference") {
                let cfg = model[name];
                cfg.field = $(node);
                cfg.lookupField = lookupMap.get(name);
                self.fieldMap.set(name, new LookupField(cfg, self.listener));
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
    self.focus = function (name) {
        self.fieldMap.get(name).field.focus();
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
    self.loadElements();
}
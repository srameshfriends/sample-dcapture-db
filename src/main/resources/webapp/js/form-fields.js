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
                self.fieldMap.set(name, new LookupField(model[name], self.listener));
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
function LookupField(model, listener) {
    let self = this;
    self.searchDelay = new CallFuture(1200);
    self.limit = 10;
    self.isLoaded = false;
    if(typeof model === "undefined" || (typeof model.name === "undefined")) {
        throw "Lookup field model name should not be null";
    }
    self.init = function () {
        let idx, tagName, name, txt, node, nodeArray = $("[data-lookup]");
        let modelName = model.name;
        for (idx = 0; idx < nodeArray.length; idx++) {
            node = nodeArray[idx];
            name = node.getAttribute("data-lookup");
            if (typeof name === "string" && name.startsWith(modelName)) {
                if (name.endsWith("input")) {
                    self.inputEle = $(node);
                } else if (name.endsWith("button")) {
                    self.buttonEle = $(node);
                } else if (name.endsWith("table")) {
                    self.tableEle = $(node);
                } else if (name.endsWith("search")) {
                    self.searchEle = $(node);
                } else if (name.endsWith("dialog")) {
                    self.dialogEle = $(node);
                } else if (name.endsWith("clear")) {
                    self.clearEle = $(node);
                } else if (name.endsWith("previous")) {
                    self.previousEle = $(node);
                } else if (name.endsWith("info")) {
                    self.infoEle = $(node);
                } else if (name.endsWith("next")) {
                    self.nextEle = $(node);
                }
            }
        }
        self.inputEle.on("focus", function () {
            if(!self.isLoaded) {
                self.isLoaded = true;
                self.search(self.getRequest(0));
            }
            self.showDialog();
        });
        if (typeof self.buttonEle !== "undefined") {
            self.buttonEle.on("click", function (evt) {
                evt.stopPropagation();
                self.showDialog();
                self.searchEle.focus();
            });
        }
        if (typeof self.searchEle !== "undefined") {
            self.searchEle.keydown(function (evt) {
                if (13 === evt.keyCode) {
                    evt.preventDefault();
                    evt.stopPropagation();
                    self.search(self.getRequest(0));
                    return false;
                }
            });
            self.searchEle.keyup(function (evt) {
                evt.preventDefault();
                self.searchDelay.cancelAll();
                if (13 !== evt.keyCode) {
                    self.searchDelay.call(function () {
                        self.search(self.getRequest(0));
                    });
                }
                return false;
            });
        }
        if (typeof self.clearEle !== "undefined") {
            self.clearEle.on("click", function (evt) {
                evt.preventDefault();
                self.setValue({});
                self.onEvent();
                self.hideDialog();
            });
        }
        if (typeof self.previousEle !== "undefined") {
            self.previousEle.on("click", function (evt) {
                evt.preventDefault();
                evt.stopPropagation();
                if (self.isPrevious) {
                    let req = self.getRequest(self.start - self.limit);
                    self.search(req);
                }
                return false;
            });
        }
        if (typeof self.nextEle !== "undefined") {
            self.nextEle.on("click", function (evt) {
                evt.preventDefault();
                evt.stopPropagation();
                if (self.isNext) {
                    let req = self.getRequest(self.start + self.limit);
                    self.search(req);
                }
                return false;
            });
            /*let inputId = "#" + self.inputEle.prop("id");
            $(inputId).on("show.bs.dropdown", function () {
                if (typeof self.searchEle !== "undefined") {
                    self.searchEle.focus();
                } else if (typeof self.tableEle !== "undefined") {
                    self.tableEle.focus();
                }
            });*/
        }
        self.setPaging({});
    };
    self.showDialog = function(){
        self.infoEle.trigger('click'); // toggle
    };
    self.hideDialog = function(){
        self.infoEle.trigger('toggle');
    };
    self.onEvent = function (evt) {
        if (typeof listener === "function") {
            listener(evt, "changed", model.name);
        }
    };
    self.setValue = function (data) {
        if (typeof data === "undefined" || data === null || typeof data !== "object") {
            data = {};
        }
        let cdx, mdl, text, display = [];
        for (cdx = 0; cdx < model.columns.length; cdx++) {
            mdl = model.columns[cdx];
            if (data.hasOwnProperty(mdl.name)) {
                text = data[mdl.name];
                display.push(text);
            }
        }
        self.selected = data;
        text = display.join(" - ");
        self.inputEle.val(text);
    };
    self.getValue = function () {
        return self.selected;
    };
    self.change = function (cell, data) {
        cell.on("click", function (evt) {
            evt.preventDefault();
            self.inputEle.trigger('click.bs.dropdown');
            self.setValue(data);
            self.onEvent(evt);
        });
    };
    self.setPaging = function (obj) {
        if (typeof obj !== "object") {
            obj = {};
        }
        if (typeof obj.start !== "number") {
            obj.start = 0;
        }
        if (typeof obj.length !== "number") {
            obj.length = 0;
        }
        if (typeof obj.totalRecords !== "number") {
            obj.totalRecords = 0;
        }
        self.start = obj.start;
        self.totalRecords = obj.totalRecords;
        self.length = obj.length;
        let startIdx = obj.start + 1, endIdx = obj.start + obj.length, currentCount;
        self.isPrevious = true;
        self.isNext = true;
        if (0 === obj.length && 0 === obj.totalRecords) {
            if (typeof self.infoEle !== "undefined") {
                self.infoEle.text(" - ");
            }
        } else {
            if (typeof self.infoEle !== "undefined") {
                if (0 === obj.length) {
                    if (0 === obj.totalRecords) {
                        self.infoEle.text(" - ");
                    } else {
                        self.infoEle.text("0 of " + obj.totalRecords);
                    }
                } else {
                    self.infoEle.text(startIdx + " - " + endIdx + " of " + obj.totalRecords);
                }
            }
        }
        if (0 === obj.start) {
            self.isPrevious = false;
        }
        currentCount = obj.start + obj.length;
        if (obj.totalRecords <= currentCount) {
            self.isNext = false;
        }
        if (typeof self.previousEle !== "undefined") {
            if (self.previousEle) {
                self.previousEle.parent().removeClass("disabled");
            } else {
                self.previousEle.parent().addClass("disabled");
            }
        }
        if (typeof self.nextEle !== "undefined") {
            if (self.nextEle) {
                self.nextEle.parent().removeClass("disabled");
            } else {
                self.nextEle.parent().addClass("disabled");
            }
        }
        return {name: self.name, start: self.start, limit: self.limit, searchText: ""};
    };
    self.setData = function (dataArray) {
        if (!Array.isArray(dataArray)) {
            dataArray = [];
        }
        let mdl, ix;
        self.tableEle.empty();
        for (ix = 0; ix < dataArray.length; ix++) {
            let cdx, text, row = $("<tr></tr>"), data = dataArray[ix];
            self.tableEle.append(row);
            for (cdx = 0; cdx < model.columns.length; cdx++) {
                mdl = model.columns[cdx];
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
        self.start = 0 > start ? 0 : start;
        let req = {};
        req.name = model.name;
        req.start = start;
        req.limit = self.limit;
        req.searchText = "";
        if (typeof self.searchEle !== "undefined") {
            req.searchText = self.searchEle.val().trim();
        }
        return req;
    };
    self.search = function (req) {
        $.ajax({
            type: "POST",
            dataType: "json",
            url: model.url,
            data: JSON.stringify(req),
            error: function (err) {
                MessageDialog.show(err.responseText);
            },
            success: function (data) {
                const dataId = model.name;
                self.setData(data[dataId]);
                self.setPaging(data);
            }
        });
    };
    self.init();
}
function LookupField() {
}
LookupField.prototype.init = function (config) {
    if (jQuery.type(config.modelCode) !== 'string') {
        config.modelCode = 'code';
    }
    if (jQuery.type(config.modelName) !== 'string') {
        config.modelName = 'name';
    }
    if (jQuery.type(config.showModelCode) !== 'boolean') {
        config.showModelCode = false;
    }
    this.modelCode = config.modelCode;
    this.modelName = config.modelName;
    this.showModelCode = config.showModelCode;
    this.url = config.url;
    this.statusCode = -1;
    this.delayCounting = 0;
    this.delayCounter = false;
    this.delayName = false;
    this.masterForm = false;
    this.pagination = false;
    this.isVisible = true;
    this.isLoadAll = false;
    this.selectedCode = '';
    this.selectedName = '';
    this.dataArray = [];
    this.changeHandlers = [];
};
LookupField.prototype.setMasterForm = function (mform) {
    this.masterForm = mform;
};
LookupField.prototype.pvtOnRemoteRequest = function (obj, pageReq) {
    if (1 === obj.statusCode || this.isLoadAll === true) {
        return;
    }
    obj.statusCode = 1;
    var remoteObj = {};
    remoteObj.pageRequest = pageReq;
    remoteObj.searchText = obj.searchInput.val().trim();
    if (this.masterForm !== false) {
        remoteObj.formObject = this.masterForm.getFormObject()
    } else {
        remoteObj.formObject = {};
    }
    remoteObj = dpc.buildRemoteJson(this.url, remoteObj);
    jQuery.ajax(remoteObj).done(function (json) {
        pageControl.showResponse(json['head']);
        obj.setDataList(json['dataList']);
        obj.setPagingInfo(json['pageRequest']);
        if (json['loadAll'] === true) {
            obj.isLoadAll = true;
        }
    }).fail(function (xhr, status, errorThrown) {
        pageControl.error(errorThrown);
    }).always(function () {
        obj.statusCode = 0;
    });
};
LookupField.prototype.pvtHandler = function (obj) {
    obj.pagination.setCallback(function (req) {
        obj.pvtOnRemoteRequest(obj, req);
    });
    obj.source.on('click focus', function () {
        obj.show(true);
    });
    obj.source.keydown(function (evt) {
        if (9 === evt.keyCode) {
            obj.show(false);
        } else if (obj.isVisible === false) {
            obj.show(true);
        }
    });
    obj.searchInput.keyup(function () {
        obj.pvtCountingStop();
        obj.pvtCountingStart();
    });
    obj.pane.clickOutSide(function (evt) {
        obj.show(false);
    });
    obj.delayCounter = function () {
        obj.delayCounting += 1;
        if (5 === obj.delayCounting) {
            obj.pvtCountingStop();
            obj.pvtOnRemoteRequest(obj, obj.pagination.newPageRequest());
        }
    };
    obj.clearLink.click(function () {
        obj.setValue(null, true);
        obj.show(false);
    });
};
LookupField.prototype.pvtCountingStart = function () {
    this.delayCounting = 0;
    this.delayName = setInterval(this.delayCounter, 240);
};
LookupField.prototype.pvtCountingStop = function () {
    this.delayCounting = 0;
    clearInterval(this.delayName);
};

LookupField.prototype.pvtUpdateTableWidth = function () {
    console.log('look up field column width is to be impl');
};
LookupField.prototype.pvtAddRow = function (obj, dataObj) {
    var codeCell, nameCell, row, rowIndex, dataCode, dataName;
    dataCode = dataObj[obj.modelCode];
    dataName = dataObj[obj.modelName];
    rowIndex = obj.table.children().length;
    row = dpc.addDiv(obj.table);
    row.data('rowData', dataObj);
    row.prop('class', (rowIndex % 2) === 0 ? 'lookupField-odd' : 'lookupField-even');
    if (obj.showModelCode === true) {
        codeCell = dpc.addDiv(row);
        codeCell.prop('class', 'lookupField-cell');
        codeCell.text(dataCode);
    }
    nameCell = dpc.addDiv(row);
    nameCell.prop('class', 'lookupField-cell');
    nameCell.text(dataName);
    row.click(function () {
        if (obj.selectedCode !== dataCode) {
            obj.setValue(dataObj, true);
        }
        obj.show(false);
    });
    return row;
};
LookupField.prototype.setPagingInfo = function (info) {
    if (jQuery.type(this.pagination) !== 'undefined') {
        if (jQuery.type(info) === 'undefined') {
            info = {};
            info.totalRows = this.table.children().length;
        }
        this.pagination.setPagingInfo(info);
    }
};
LookupField.prototype.setDataList = function (dl) {
    this.table.empty();
    if ((jQuery.type(dl) === 'undefined') || !Array.isArray(dl)) {
        dl = [];
    }
    for (var vi = 0; vi < dl.length; vi++) {
        this.pvtAddRow(this, dl[vi]);
    }
    this.dataArray = dl;
};
LookupField.prototype.setJsonText = function (jsonTxt) {
    try {
        this.setDataList(JSON.parse(jsonTxt));
    } catch (err) {
        console.log('Lookup Field set json text parse error ' + err);
    }
};
LookupField.prototype.clearData = function () {
    this.table.empty();
    this.dataArray = [];
    this.isLoadAll = false;
    this.pagination.setPagingInfo({});
};
LookupField.prototype.addChangeHandler = function (callback) {
    if (jQuery.type(callback) === 'function') {
        this.changeHandlers.push(callback);
    }
};
LookupField.prototype.notifyAll = function () {
    var handler, result = this.getValue();
    for (var iv = 0; iv < this.changeHandlers.length; iv++) {
        handler = this.changeHandlers[iv];
        handler(result);
    }
};
LookupField.prototype.setValue = function (dataObj, notifyAll) {
    if (jQuery.type(dataObj) === 'undefined' || jQuery.type(dataObj) !== 'object') {
        this.selectedCode = '';
        this.selectedName = '';
    } else {
        this.selectedCode = dataObj[this.modelCode];
        this.selectedName = dataObj[this.modelName];
    }
    if (jQuery.type(this.selectedCode) === 'undefined') {
        this.selectedCode = '';
    }
    if (jQuery.type(this.selectedName) === 'undefined') {
        this.selectedName = '';
    }
    if (this.showModelCode === true) {
        this.source.val(this.selectedCode + ' - ' + this.selectedName);
    } else {
        this.source.val(this.selectedName);
    }
    if (notifyAll === true) {
        this.notifyAll();
    }
};
LookupField.prototype.getValue = function () {
    var result = {};
    if (this.selectedCode !== '') {
        result[this.modelCode] = this.selectedCode;
        result[this.modelName] = this.selectedName;
    }
    return result;
};
LookupField.prototype.setReadOnly = function () {
    this.editable = false;
};
LookupField.prototype.setEditable = function () {
    this.editable = true;
};
LookupField.prototype.show = function (isShow) {
    this.pvtCountingStop();
    this.isVisible = isShow;
    if (isShow) {
        var pos, eWidth, mWidth, left, top;
        pos = this.source.offset();
        eWidth = this.source.width();
        left = (pos.left);
        top = pos.top;
        this.pane.css('min-width', eWidth);
        this.pane.css({left: left, top: top});
        this.pane.css('display', 'inline-block');
        this.searchInput.focus();
        if (0 > this.statusCode) {
            this.pvtOnRemoteRequest(this, this.pagination.newPageRequest());
        }
    } else {
        this.pane.hide();
    }
};
LookupField.prototype.getControl = function () {
    this.pane = dpc.addDiv($('body'));
    this.source = dpc.createInput('text');
    this.searchInput = dpc.addInputText(this.pane);
    this.table = dpc.addDiv(this.pane);
    this.naviPane = dpc.addDiv(this.pane);
    this.pagination = new Pagination();
    this.naviPane.append(this.pagination.getControl());
    this.clearLink = dpc.addLink(this.naviPane);
    this.dataInfo = dpc.addDiv(this.pane);
    this.pane.prop('class', 'lookupField');
    this.table.prop('class', 'lookupField-table');
    this.searchInput.prop('class', 'lookupField-search');
    this.clearLink.prop('class', 'lookupField-clear');
    this.dataInfo.prop('class', 'lookupField-dataInfo');
    this.clearLink.text(translate('shared', 'pagination', 'clearAll'));
    this.pvtHandler(this);
    this.show(false);
    return this.source;
};
LookupField.prototype.getField = function () {
    return this.source;
};

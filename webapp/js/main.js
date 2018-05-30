function getLocale(data, name) {
    let txt = data[name];
    return txt === null || 0 === txt.length ? name : txt;
}

function setLocale(data) {
    let idx, tagName, name, txt, node, nodeArray = $("[data-locale]");
    for (idx = 0; idx < nodeArray.length; idx++) {
        node = nodeArray[idx];
        name = node.getAttribute("data-locale");
        tagName = node.tagName.toUpperCase();
        if ($.type(name) !== "string") {
            name = "";
        }
        txt = data[name];
        if ($.type(txt) !== "string") {
            txt = name;
        }
        if (tagName === "INPUT" || tagName === "TEXTAREA" || tagName === "SELECT") {
            $(node).val(txt);
        } else {
            $(node).text(txt);
        }
    }
}

function CallFuture(delay) {
    if (typeof delay !== "number") {
        delay = 1000;
    }
    this.delay = delay;
    if (typeof window.callFutureIdsSet === "undefined") {
        window.callFutureIdsSet = new Set();
    }
}

CallFuture.prototype.call = function (listener) {
    const delayId = setInterval(function () {
        clearInterval(delayId);
        if (typeof listener === "function") {
            listener();
        }
    }, this.delay);
    window.callFutureIdsSet.add(delayId);
    return delayId;
};
CallFuture.prototype.cancel = function (intervalId) {
    if (typeof intervalId !== "undefined" && Number.isInteger(intervalId)) {
        clearInterval(intervalId);
        if (window.callFutureIdsSet.has(intervalId)) {
            window.callFutureIdsSet.delete(intervalId);
        }
    }
};
CallFuture.prototype.cancelAll = function () {
    window.callFutureIdsSet.forEach(function (delayId) {
        clearInterval(delayId);
    });
    window.callFutureIdsSet = new Set();
};

/* deprecated */
function hideMessage() {
    if (typeof window.showMessageDelay === "undefined") {
        window.showMessageDelay = 0;
    } else if (0 < window.showMessageDelay) {
        clearInterval(window.showMessageDelay);
        window.showMessageDelay = 0;
    }
    $("#messageDialog").hide();
}

function showMessage(msg, title, delay) {
    if (typeof window.showMessageDelay === "undefined") {
        window.showMessageDelay = 0;
    }
    if (typeof window.messageDialog !== "function") {
        window.messageDialog = function () {
            $("#messageDialog").hide();
            clearInterval(window.showMessageDelay);
            window.showMessageDelay = 0;
        };
    }
    clearInterval(window.showMessageDelay);
    if (typeof msg !== "string") {
        msg = "-";
    }
    if (typeof title === "undefined") {
        title = "";
    } else {
        title = title + " : ";
    }
    if (typeof delay === "undefined" || typeof delay !== "number") {
        delay = 5000;
    }
    $("#messageTitle").text(title);
    $("#messageContent").text(msg);
    window.showMessageDelay = setInterval(window.messageDialog, delay);
}

function ShowMessageDialog(msg) {
    let panel = $("<div></div>"), cardDiv = $("<div></div>"), headerDiv = $("<div></div>");
    let msgEle = $("<span></span>"), closeEle = $("<button><i></i></button>"), msgDelay = 0;
    panel.append(cardDiv);
    cardDiv.append(headerDiv);
    headerDiv.append(msgEle);
    headerDiv.append(closeEle);
    panel.prop("class", "fixed-top zindex-fixed w-50").css("left", "25%");
    cardDiv.prop("class", "card");
    headerDiv.prop("class", "card-header bg-info text-light");
    closeEle.prop("class", "float-right btn btn-info");
    closeEle.children().eq(0).prop("class", "fas fa-times icon-blue").css('color', '#ffffff');
    msgDelay = setInterval(function () {
        panel.remove();
        clearInterval(msgDelay);
    }, 5000);
    closeEle.first().click(function (evt) {
        evt.preventDefault();
        panel.remove();
        clearInterval(msgDelay);
    });
    if (typeof msg === 'string') {
        msgEle.text(msg);
    }
    $('body').append(panel);
}

function SessionDB() {

}

SessionDB.set = function (name, value) {
    if (sessionStorage) {
        sessionStorage.setItem(name, value);
    }
};
SessionDB.get = function (name) {
    if (sessionStorage) {
        return sessionStorage.getItem(name);
    }
    return null;
};
SessionDB.getValue = function (name) {
    let text = SessionDB.get(name);
    if ($.type(text) === "string" && 0 < text.length) {
        try {
            return JSON.parse(text);
        } catch (e) {
        }
    }
    return false;
};

function PageManager() {
    let self = this;
    self.loadMenu = function (url, node, callback) {
        $.ajax({
            type: "GET",
            url: url,
            dataType: 'html',
            cache: false,
            success: function (html) {
                node.html(html);
                if ($.type(callback) === "function") {
                    callback();
                }
            }
        });
    };
    self.setActiveMenuItem = function (viewId) {
        let idx, navId, node, nodeArray, history;
        nodeArray = $("a[data-nav]");
        for (idx = 0; idx < nodeArray.length; idx++) {
            node = nodeArray[idx];
            navId = $(node).data("nav");
            if (viewId === navId) {
                $(node).removeClass("text-primary");
            } else {
                $(node).addClass("text-primary");
            }
        }
        history = SessionDB.get("activeView");
        if ($.type(history) === "string" && 2 < history.length) {
            SessionDB.set("previousView", history);
        }
        SessionDB.set("activeView", viewId);
    };
    self.loadLocale = function (url, callback) {
        let data, text = SessionDB.get("locale");
        if ($.type(text) === "string") {
            data = JSON.parse(text);
        }
        if ($.type(data) === "object") {
            setLocale(data);
        } else {
            $.ajax({
                type: "POST",
                dataType: "json",
                url: url,
                error: function (err) {
                    new ShowMessageDialog(err.responseText);
                },
                success: function (data) {
                    SessionDB.set("locale", JSON.stringify(data));
                    setLocale(data);
                    if ($.type(callback) === "function") {
                        callback();
                    }
                }
            });
        }
    };
}

/*Deprecated*/
function UndoDialog(actionId, msg, listener) {
    let undoDelay = 0, dialog = $("#undoDialog");
    $("#undoDialogBtn").click(function () {
        dialog.hide();
        $("#undoDialogBtn").off('click');
        $("#undoDialogClose").off('click');
        clearInterval(undoDelay);
        if (typeof listener === "function") {
            listener("");
        }
    });
    $("#undoDialogClose").click(function (evt) {
        evt.preventDefault();
        dialog.hide();
        $("#undoDialogBtn").off('click');
        $("#undoDialogClose").off('click');
        clearInterval(undoDelay);
        if (typeof listener === "function") {
            listener(actionId);
        }
    });
    if (typeof msg === "undefined") {
        msg = "-";
    }
    dialog.show();
    $("#undoDialogMsg").text(msg);
    undoDelay = setInterval(function () {
        dialog.hide();
        $("#undoDialogBtn").off('click');
        $("#undoDialogClose").off('click');
        clearInterval(undoDelay);
        if (typeof listener === "function") {
            listener(actionId);
        }
    }, 3000);
}

/*Deprecated*/
UndoDialog.prototype.setTitle = function (txt) {
    $("#undoDialogBtn").text(txt);
};
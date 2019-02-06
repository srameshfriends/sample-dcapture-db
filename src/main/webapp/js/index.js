function UndoDialog() {
}

UndoDialog.show = function (msg, actionId, listener) {
    UndoDialog.undoDelay = 0;
    $("#undoDialog-btn").on("click", function (evt) {
        evt.preventDefault();
        UndoDialog.hide();
    });
    $("#undoDialog-close").on("click", function (evt) {
        evt.preventDefault();
        UndoDialog.hide();
        if (typeof listener === "function") {
            listener(actionId);
        }
    });
    if (typeof msg === "undefined") {
        msg = "-";
    }
    $("#undoDialog").show();
    $("#undoDialog-msg").text(msg);
    UndoDialog.undoDelay = setInterval(function () {
        UndoDialog.hide();
        if (typeof listener === "function") {
            listener(actionId);
        }
    }, 4000);
};
UndoDialog.showLocale = function (actionId, listener) {
    UndoDialog.show(SessionDB.getLocale(actionId + ".undo"), actionId, listener);
};
UndoDialog.hide = function () {
    clearInterval(UndoDialog.undoDelay);
    $("#undoDialog-btn").off('click');
    $("#undoDialog-close").off('click');
    $("#undoDialog").hide();
};

function MessageDialog() {
}

MessageDialog.show = function (msg) {
    MessageDialog.delay = 0;
    $("#messageDialog-close").on("click", function (evt) {
        evt.preventDefault();
        MessageDialog.hide();
    });
    if (typeof msg === "undefined") {
        msg = "-";
    }
    $("#messageDialog").show();
    $("#messageDialog-msg").text(msg);
    MessageDialog.delay = setInterval(function () {
        MessageDialog.hide();
    }, 8000);
    const hgt = $("header").height() + 12;
    $("main").css("margin-top", hgt + "px");
};
MessageDialog.showLocale = function (msgId) {
    MessageDialog.show(SessionDB.getLocale(msgId));
};
MessageDialog.hide = function () {
    clearInterval(MessageDialog.delay);
    $("#messageDialog-close").off('click');
    $("#messageDialog").hide();
    const hgt = $("header").height() + 12;
    $("main").css("margin-top", hgt + "px");
};

function PageManager() {
}
PageManager.addMenuItem = function (menuId, href) {
    let ele = $("#" + menuId);
    ele.on("click", function (evt) {
        evt.preventDefault();
        PageManager.show(href);
    });
};
PageManager.getOpenViews = function () {
    return ["view/signin1.html", "view/signin2.html", "view/signup1.html", "view/signup2.html"];
};
PageManager.hasSession = function () {
    let cfg = SessionDB.get("aduetthaecnittinceahtteuda");
    if (typeof cfg === "object" && typeof cfg["authenticated"] === "boolean" || cfg["authenticated"] === true) {
        if (typeof cfg["sessionId"] === "string" && 8 < cfg["sessionId"].length) {
            return true;
        }
    }
    return false;
};
PageManager.setSession = function (cfg) {
    let navigation = $("#appsNavigation"), signOutBtn = $("#appsSignOut");
    signOutBtn.off();
    SessionDB.set("aduetthaecnittinceahtteuda", cfg);
    SessionDB.setValue("applicationName", cfg["name"]);
    SessionDB.setValue("applicationId", cfg["id"]);
    if (!PageManager.hasSession()) {
        navigation.hide();
        return false;
    }
    navigation.show();
    $("#appsName").text(cfg["name"]);
    $("#appsUser").text(cfg["userName"]);
    $("#appsUserId").text(cfg["email"]);
    signOutBtn.on("click", function (evt) {
        evt.preventDefault();
        PageManager.clearSession();
    });
    return true;
};
PageManager.start = function () {
    let signOutBtn = $("#appsSignOut"), navigation = $("#appsNavigation");
    MessageDialog.hide();
    UndoDialog.hide();
    SessionDB.loadLocale("api/locale/default", function () {
        $.ajax({
            type: "POST",
            contentType : "application/json",
            dataType: "json",
            data: "{}",
            url: "api/session/validate",
            error: function (err) {
                MessageDialog.show(err.responseText);
            },
            success: function (args) {
                PageManager.setSession(args);
                if (PageManager.hasSession()) {
                    PageManager.show("view/expense-list.html");
                } else {
                    PageManager.show("view/signin1.html");
                }
            }
        });
    });
    signOutBtn.on("click", function (evt) {
        evt.preventDefault();
        PageManager.clearSession();
    });
};
PageManager.clearSession = function () {
    $("#appsNavigation").hide();
    $.ajax({
        type: "PUT",
        contentType : "application/json",
        dataType: "json",
        url: "api/session/clear",
        data: "{}",
        error: function (msg) {
            MessageDialog.show(msg.responseText);
            SessionDB.clear();
            location.reload();
        },
        success: function () {
            SessionDB.clear();
            location.reload();
        }
    });
};
PageManager.show = function (url) {
    if (false === PageManager.hasSession()) {
        if (0 > PageManager.getOpenViews().indexOf(url)) {
            url = "view/signin1.html";
        }
    }
    SessionDB.loadHtml(url, function (html) {
        $("#mainView").empty().html(html);
        let previous = SessionDB.get("activeHtml");
        if (typeof previous === "string") {
            SessionDB.set("previousHtml", previous);
        }
        SessionDB.set("activeHtml", url);
    });
};
PageManager.init = function (view) {
    SessionDB.setDataLocale();
    SessionDB.setParameter("view=" + view.getId());
    const title = SessionDB.getLocale(view.getId() + ".title");
    $("title").text(title);
    $("#page-view-id").text(title);
    if (typeof view.start === "function") {
        view.start();
    }
};
PageManager.showPrevious = function () {
    let previous = SessionDB.get("previousHtml");
    if (typeof previous === "string") {
        PageManager.show(previous);
    }
};
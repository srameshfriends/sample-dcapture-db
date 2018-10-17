function PageManager() {
}

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
        PageManager.end();
    });
    return true;
};
PageManager.start = function () {
    let dialog = $("#page-dialog"), signOutBtn = $("#appsSignOut"), navigation = $("#appsNavigation");
    MessageDialog.hide();
    UndoDialog.hide();
    SessionDB.loadLocale("api/locale/default", function () {
        $.ajax({
            type: "POST",
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
        PageManager.end();
    });
    $("#menuItemExpense").on("click", function (evt) {
        evt.preventDefault();
        PageManager.show("view/expense-list.html");
    });
    $("#menuItemExpenseCategory").on("click", function (evt) {
        evt.preventDefault();
        PageManager.show("view/expense-category.html");
    });
    $("#menuItemProject").on("click", function (evt) {
        evt.preventDefault();
        PageManager.show("view/project.html");
    });
    $("#menuItemCurrency").on("click", function (evt) {
        evt.preventDefault();
        PageManager.show("view/currency.html");
    });
    $("#menuItemUsers").on("click", function (evt) {
        evt.preventDefault();
        PageManager.show("view/user-list.html");
    });
};
PageManager.end = function () {
    SessionDB.remove("aduetthaecnittinceahtteuda");
    $("#appsNavigation").hide();
    $.ajax({
        type: "POST",
        dataType: "json",
        url: "api/session/end",
        data: "{}",
        error: function () {
            MessageDialog.show("Error : Service or network failure, Please try again!");
        },
        success: function (cfg) {
            PageManager.setSession(cfg);
            PageManager.show("view/signin1.html");
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
    $("#view-title").text(title);
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
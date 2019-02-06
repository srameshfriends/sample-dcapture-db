package sample.dcapture.db.service;

import dcapture.db.core.DataSet;
import dcapture.db.core.SelectBuilder;
import dcapture.db.core.SqlDatabase;
import dcapture.db.util.DataSetRequest;
import dcapture.db.util.DataSetResult;
import dcapture.io.AppSettings;
import dcapture.io.HttpMethod;
import dcapture.io.HttpPath;
import dcapture.io.Localization;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.UUID;

@HttpPath(value = "/session", secured = false)
public class SessionService {
    private static final String SESSION_USER = "session_user";
    private static final String USER_TBL = "apps_user", SESSION_BATCH_TBL = "session_batch";
    private SqlDatabase database;
    private Localization locale;
    private AppSettings settings;

    @Inject
    public SessionService(Localization locale, SqlDatabase database, AppSettings settings) {
        this.database = database;
        this.locale = locale;
        this.settings = settings;
    }

    @HttpPath(value = "/validate", secured = false)
    public DataSetResult validate(HttpServletRequest req) {
        if (req.getSession(false) == null ||
                req.getSession(false).getAttribute(USER_TBL) == null) {
            return getJsonResult("", "", "", "", false);
        } else {
            DataSet appsUser = (DataSet) req.getSession(false).getAttribute(USER_TBL);
            String email = appsUser.getString("email", "");
            String name = appsUser.getString("name", "");
            String id = req.getSession(false).getId();
            return getJsonResult(id, name, email, "", true);
        }
    }

    @HttpPath(value = "/clear", secured = false)
    @HttpMethod("PUT")
    public DataSetResult clear(DataSetRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return DataSetResult.success("clearSession.msg");
    }

    @HttpPath(value = "/reset", secured = false)
    public DataSetResult reset() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("status", "success");
        builder.add("title", locale.get("mailToResetPassword"));
        return DataSetResult.asJson(builder.build());
    }

    @HttpPath(value = "/authorise1", secured = false)
    public DataSetResult authorise1(DataSetRequest request) {
        return database.transact(query -> {
            String email = request.getString("email");
            if (notValid(email)) {
                throw new IllegalArgumentException(locale.get("apps_user.email.invalid"));
            }
            email = email.trim().toLowerCase();
            SelectBuilder select = query.selectFrom(USER_TBL, "email").where("email", email);
            String oldEmail = query.getString(select);
            if (oldEmail == null || !email.equals(oldEmail.toLowerCase())) {
                throw new IllegalArgumentException(locale.get("apps_user.email.invalid"));
            }
            query.execute(query.deleteBuilder(SESSION_BATCH_TBL).where("email", email));
            final String code = UUID.randomUUID().toString();
            query.execute(query.insertBuilder(SESSION_BATCH_TBL).set("email", email).set("code", code)
                    .set("created_on", LocalDateTime.now()).set("client", getClientInfo(request)));
            return getJsonResult("", "", email, code, false);
        });
    }

    @HttpPath(value = "/authorise2", secured = false)
    public DataSetResult authorise2(DataSetRequest request) {
        String code = request.getString("code", "");
        String pass = request.getString("pass", "");
        if (notValid(code)) {
            return getJsonResult("", "", "", "", false);
        } else if (notValid(pass)) {
            return DataSetResult.error("userOrPasswordNotValid");
        }
        return database.transact(query -> {
            SelectBuilder select = query.selectFrom(SESSION_BATCH_TBL, "email")
                    .where("code", code).limit(1);
            String email = query.getString(select);
            if (email == null) {
                return getJsonResult("", "", "", "", false);
            }
            select = query.selectAll(USER_TBL).where("email", email).and("password", pass).limit(1);
            DataSet appsUser = query.getDataSet(select);
            if (appsUser == null || !pass.equals(appsUser.getString("password", ""))) {
                return DataSetResult.error("userOrPasswordNotValid");
            }
            query.execute(query.deleteBuilder(SESSION_BATCH_TBL).where("email", email));
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_USER, appsUser);
            String userName = appsUser.getString("name", "");
            return getJsonResult(session.getId(), userName, email, "", true);
        });
    }

    private DataSetResult getJsonResult(String sessionId, String name, String email, String code, boolean authenticated) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("email", email);
        builder.add("userName", name);
        builder.add("code", code);
        builder.add("authenticated", authenticated);
        builder.add("sessionId", sessionId);
        builder.add("id", settings.getId());
        builder.add("name", settings.getName());
        return DataSetResult.asJson(builder.build());
    }

    private boolean notValid(String code) {
        return code == null || code.trim().isEmpty();
    }

    private String getClientInfo(DataSetRequest request) {
        String host, address, os;
        try {
            host = request.getRemoteHost();
            address = request.getRemoteAddr();
            if (address.equals("0:0:0:0:0:0:0:1")) {
                InetAddress local = InetAddress.getLocalHost();
                address = local.getHostAddress();
                host = local.getHostName();
            }
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
            host = "host";
            address = "address";
        }
        String userAgent = request.getHeader("User-Agent");
        userAgent = userAgent == null ? "" : userAgent.trim().toLowerCase();
        if (userAgent.contains("window")) {
            os = "window";
        } else if (userAgent.contains("mac")) {
            os = "mac";
        } else if (userAgent.contains("linux")) {
            os = "linux";
        } else if (userAgent.contains("x11")) {
            os = "unix";
        } else if (userAgent.contains("android")) {
            os = "android";
        } else if (userAgent.contains("iphone")) {
            os = "iphone";
        } else {
            os = "os";
        }
        return host + " " + address + " " + os;
    }
}

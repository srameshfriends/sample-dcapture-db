package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.io.*;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.UUID;

@HttpPath(value = "/session", secured = false)
public class SessionService {
    private static final String SESSION_USER = "session_user";
    private static final String USER_TABLE = "apps_user", SESSION_BATCH_TABLE = "session_batch";
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
    public JsonObject validate(JsonRequest request) {
        if (request.getSession(false) == null ||
                request.getSession(false).getAttribute(USER_TABLE) == null) {
            return toJsonObject("", "", "", "", false);
        }
        DataSet appsUser = (DataSet) request.getSession(false).getAttribute(USER_TABLE);
        String email = appsUser.getString("email", "");
        String name = appsUser.getString("name", "");
        String id = request.getSession(false).getId();
        return toJsonObject(id, name, email, "", true);
    }

    @HttpPath(value = "/clear", secured = false)
    @HttpMethod("PUT")
    public void clearSession(JsonRequest request, JsonResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.success(locale.get("clearSession.msg"));
    }

    @HttpPath(value = "/reset", secured = false)
    public JsonObject validate(JsonObject request) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("status", "success");
        builder.add("title", locale.get("mailToResetPassword"));
        return builder.build();
    }

    @HttpPath(value = "/authorise1", secured = false)
    public JsonObject authorise1(JsonRequest request) {
        String email = request.getString("email");
        if (notValid(email)) {
            throw new IllegalArgumentException(locale.get("apps_user.email.invalid"));
        }
        SqlQuery query = database.getQuery();
        SelectQuery selectQry = query.selectFrom(USER_TABLE, "email").equalTo("email", email);
        String oldEmail = selectQry.getString();
        if (oldEmail == null || !oldEmail.equals(email)) {
            throw new IllegalArgumentException(locale.get("apps_user.email.invalid"));
        }
        query.delete(SESSION_BATCH_TABLE).equalTo("email", email);
        String code = UUID.randomUUID().toString();
        InsertQuery insertQuery = query.insert(SESSION_BATCH_TABLE).set("email", email).set("code", code);
        insertQuery.set("created_on", LocalDateTime.now()).set("client", getClientInfo(request));
        query.commit();
        return toJsonObject("", "", email, code, false);
    }

    @HttpPath(value = "/authorise2", secured = false)
    public void authorise2(JsonRequest request, JsonResponse response) {
        String code = request.getString("code", "");
        String pass = request.getString("pass", "");
        if (notValid(code)) {
            response.sendObject(toJsonObject("", "", "", "", false));
            return;
        }
        if (notValid(pass)) {
            response.error(locale.get("userOrPasswordNotValid"));
            return;
        }
        SqlQuery query = database.getQuery();
        String email = query.selectFrom(SESSION_BATCH_TABLE, "email").equalTo("code", code).limit(1).getString();
        if (email == null) {
            response.sendObject(toJsonObject("", "", "", "", false));
            return;
        }
        SelectQuery userQuery = query.selectAll(USER_TABLE).equalTo("email", email).and("password", pass).limit(1);
        DataSet appsUser = userQuery.getDataSet();
        if (appsUser == null || !pass.equals(appsUser.getString("password", ""))) {
            response.error(locale.get("userOrPasswordNotValid"));
            return;
        }
        query.delete(SESSION_BATCH_TABLE).equalTo("email", email);
        query.commit();
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER, appsUser);
        String userName = appsUser.getString("name", "");
        response.sendObject(toJsonObject(session.getId(), userName, email, "", true));
    }

    private JsonObject toJsonObject(String sessionId, String name, String email, String code, boolean authenticated) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("email", email);
        builder.add("userName", name);
        builder.add("code", code);
        builder.add("authenticated", authenticated);
        builder.add("sessionId", sessionId);
        builder.add("id", settings.getId());
        builder.add("name", settings.getName());
        return builder.build();
    }

    private boolean notValid(String code) {
        return code == null || code.trim().isEmpty();
    }

    private String getClientInfo(JsonRequest request) {
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

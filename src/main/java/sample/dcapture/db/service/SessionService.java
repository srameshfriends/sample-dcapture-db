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
    private SqlDatabase database;
    private Localization locale;
    private BaseSettings settings;

    @Inject
    public SessionService(Localization locale, SqlDatabase database, BaseSettings settings) {
        this.database = database;
        this.locale = locale;
        this.settings = settings;
    }

    @HttpPath(value = "/validate", secured = false)
    @HttpMethod("POST")
    public JsonObject validate(JsonRequest request) {
        if (request.getSession(false) == null ||
                request.getSession(false).getAttribute("apps_user") == null) {
            return toJsonObject("", "", "", "", false);
        }
        DataSet appsUser = (DataSet) request.getSession(false).getAttribute("apps_user");
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
    @HttpMethod("POST")
    public JsonObject validate(JsonObject request) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("status", "success");
        builder.add("title", locale.get("mailToResetPassword"));
        return builder.build();
    }

    @HttpPath(value = "/authorise1", secured = false)
    @HttpMethod("POST")
    public void authorise1(JsonRequest request, JsonResponse response) throws Exception {
        String email = request.getString("email");
        if (notValid(email)) {
            response.error(locale.get("apps_user.email.invalid"));
            return;
        }
        SelectQuery query = database.getSelectQuery().append("SELECT email FROM ").addTable("apps_user");
        query.append(" WHERE email = ?").setParameter(email);
        String oldEmail = query.getString();
        if (oldEmail == null || !oldEmail.equals(email)) {
            response.error(locale.get("apps_user.email.invalid"));
            return;
        }
        SqlTransaction transaction = database.getTransaction();
        DeleteQuery deleteQuery = database.getDeleteQuery().delete("session_batch");
        deleteQuery.append(deleteQuery.whereQuery().equalTo("email", email));
        transaction.executeUpdate(deleteQuery);
        String code = UUID.randomUUID().toString();
        InsertQuery insertQuery = database.getInsertQuery();
        insertQuery.insert("session_batch").set("email", email).set("code", code);
        insertQuery.set("created_on", LocalDateTime.now()).set("client", getClientInfo(request));
        transaction.executeUpdate(insertQuery).commit();
        response.sendObject(toJsonObject("", "", email, code, false));
    }

    @HttpPath(value = "/authorise2", secured = false)
    @HttpMethod("POST")
    public void authorise2(JsonRequest request, JsonResponse response) throws Exception {
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
        SelectQuery query = database.getSelectQuery();
        query.select("session_batch", "email").append(" WHERE code = ?").setParameter(code);
        String email = (String) query.getValue();
        if (email == null) {
            response.sendObject(toJsonObject("", "", "", "", false));
            return;
        }
        SelectQuery userQuery = database.getSelectQuery();
        userQuery.select("apps_user").append(" WHERE email = ?").setParameter(email).limit(1);
        DataSet appsUser = userQuery.getDataSet();
        if (appsUser == null || !pass.equals(appsUser.getString("password", ""))) {
            response.error(locale.get("userOrPasswordNotValid"));
            return;
        }
        DeleteQuery deleteQuery = database.getDeleteQuery().delete("session_batch");
        deleteQuery.append("WHERE email = ?").setParameter(email);
        database.getTransaction().executeUpdate(deleteQuery).commit();
        HttpSession session = request.getSession(true);
        session.setAttribute("session_user", appsUser);
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

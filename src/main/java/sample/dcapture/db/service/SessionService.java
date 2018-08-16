package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.db.postgres.PgQuery;
import dcapture.io.BaseSettings;
import dcapture.io.JsonRequest;
import dcapture.io.JsonResponse;
import dcapture.io.Localization;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Path;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.UUID;

@Path("/session")
public class SessionService extends SqlMapper {
    private static final Logger logger = LogManager.getLogger(SessionService.class);
    private SqlDatabase database;
    private Localization locale;
    private BaseSettings settings;

    @Inject
    public SessionService(Localization locale, SqlDatabase database, BaseSettings settings) {
        this.database = database;
        this.locale = locale;
        this.settings = settings;
    }

    @Path("/validate")
    public JsonObject validate(JsonRequest request) {
        if (request.getSession(false) == null) {
            return toJsonObject("", "", "", "", false);
        }
        DataSet appsUser = (DataSet) request.getSession(false).getAttribute("apps_user");
        String email = appsUser.getString("email", "");
        String name = appsUser.getString("name", "");
        String id = request.getSession(false).getId();
        return toJsonObject(id, name, email, "", true);
    }

    @Path("/end")
    public JsonObject end(JsonRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return toJsonObject("", "", "", "", false);
    }

    @Path("/reset")
    public JsonObject validate(JsonObject request) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("status", "success");
        builder.add("title", locale.get("mailToResetPassword"));
        return builder.build();
    }

    @Path("/authorise1")
    public void authorise1(JsonRequest request, JsonResponse response) throws Exception {
        String email = request.getString("email");
        if (notValid(email)) {
            response.error(locale.get("email.invalid"));
            return;
        }
        PgQuery query = new PgQuery("SELECT id FROM ");
        query.add(database.getSchema()).add(".apps_user").add(" WHERE email = ?").setParameter(email);
        Long id = (Long) database.getReader().getValue(query);
        if (id == null || 1 > id) {
            response.error(locale.get("email.invalid"));
            return;
        }
        SqlTransaction transaction = database.beginTransaction();
        transaction.execute(new PgQuery("DELETE FROM session_batch WHERE email = ?").setParameter(email));
        transaction.commit();
        String code = UUID.randomUUID().toString();
        DataModel model = new DataModel();
        model.setValue("email", email);
        model.setValue("code", code);
        model.setValue("created_on", LocalDateTime.now());
        model.setValue("client", getClientInfo(request));
        transaction = database.beginTransaction();
        transaction.insert("session_batch", "edit", model.as());
        transaction.commit();
        response.sendObject(toJsonObject("", "", email, code, false));
    }

    @Path("/authorise2")
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
        SqlQuery query = new PgQuery("SELECT email FROM ");
        query.add(database.getSchema()).add(".session_batch").add(" WHERE ").add(" code = ?").setParameter(code);
        String email = (String) database.getReader().getValue(query);
        if (email == null) {
            response.sendObject(toJsonObject("", "", "", "", false));
            return;
        }
        query = querySelectAll(database, "apps_user").add(" WHERE ")
                .add(" email = ?").setParameter(email).limit(1);
        DataSet appsUser = database.getReader().first("apps_user", query);
        if (appsUser == null || !pass.equals(appsUser.getString("password", ""))) {
            response.error(locale.get("userOrPasswordNotValid"));
            return;
        }
        query = new PgQuery().add("DELETE FROM ");
        if (database.getSchema() != null) {
            query.add(database.getSchema()).add(".");
        }
        query.add("session_batch WHERE email = ?").setParameter(email);
        SqlTransaction transaction = database.beginTransaction();
        transaction.execute(query);
        transaction.commit();
        HttpSession session = request.getSession(true);
        session.setAttribute("apps_user", appsUser);
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
            if (logger.isTraceEnabled()) {
                ex.printStackTrace();
            }
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

package sample.dcapture.sql.service;

import dcapture.io.*;
import dcapture.sql.core.*;
import dcapture.sql.postgres.PgQuery;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Path;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

@Path("/session")
public class SessionService extends SqlMapper {
    private static final Logger logger = Logger.getLogger(SessionService.class);
    private SqlDatabase database;
    private Localization locale;

    @Inject
    public SessionService(Localization locale, SqlDatabase database) {
        this.database = database;
        this.locale = locale;
    }

    @Path("/validate")
    private void validate(JsonRequest request, JsonResponse response) {
        if (request.getSession(false) == null) {
            response.send(getResponse("", "", "", "", false));
        } else {
            Entity appsUser = (Entity) request.getSession(false).getAttribute("apps_user");
            String email = appsUser.getString("email", "");
            String name = appsUser.getString("name", "");
            String id = request.getSession(false).getId();
            response.send(getResponse(id, name, email, "", true));
        }
    }

    @Path("/invalidate")
    private void invalidate(JsonRequest request, JsonResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.accepted("Success");
    }

    @Path("/reset")
    private JsonObject validate(JsonObject request) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("status", "success");
        builder.add("title", locale.get("resetPasswordMessage"));
        return builder.build();
    }

    @Path("/authorised")
    private void email(JsonRequest request, JsonResponse response) throws SQLException {
        String email = request.getString("email");
        String code = request.getString("code");
        String pass = request.getString("pass");
        if (isValid(email)) {
            SqlTable sessionBatch = database.getTable("session_batch");
            SqlQuery query = querySelectAll(database, sessionBatch);
            query.add(" WHERE email = ?").setParameter(email).limit(1);
            Entity entity = database.getReader().first("session_batch", query);
            SqlTransaction transaction = database.beginTransaction();
            code = UUID.randomUUID().toString();
            if (entity == null) {
                entity = new Entity();
                entity.setValue("email", email);
                entity.setValue("code", code);
                entity.setValue("created_on", LocalDateTime.now());
                entity.setValue("client", getClientInfo(request));
                transaction.insert("session_batch", "edit", entity);
            } else {
                entity.setValue("email", email);
                entity.setValue("code", code);
                entity.setValue("created_on", LocalDateTime.now());
                entity.setValue("client", getClientInfo(request));
                transaction.update("session_batch", "edit", entity);
            }
            transaction.commit();
            response.send(getResponse("", "", email, code, false));
        } else if (isValid(code) && isValid(pass)) {
            SqlQuery query = new PgQuery().add("SELECT ").add(" email FROM ");
            if (database.getSchema() != null) {
                query.add(database.getSchema()).add(".");
            }
            query.add("session_batch").add(" WHERE ").add(" code = ?").setParameter(code);
            email = (String) database.getReader().getValue(query);
            if (email != null) {
                query = querySelectAll(database, "apps_user").add(" WHERE ")
                        .add(" email = ?").setParameter(email).limit(1);
                Entity appsUser = database.getReader().first("apps_user", query);
                if (appsUser == null) {
                    response.error(locale.get("userOrPasswordNotValid"));
                } else {
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
                    response.send(getResponse(session.getId(), userName, email, "", true));
                }
            }
        } else {
            response.error(locale.get("userOrPasswordNotValid"));
        }
    }

    private JsonObject getResponse(String id, String name, String email, String code, boolean authenticated) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("email", email);
        builder.add("name", name);
        builder.add("code", code);
        builder.add("authenticated", authenticated);
        builder.add("id", id);
        builder.add("signOut", locale.get("signOut"));
        return builder.build();
    }

    private boolean isValid(String code) {
        return code != null && !code.trim().isEmpty();
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

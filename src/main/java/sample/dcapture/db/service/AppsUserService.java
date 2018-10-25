package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.db.util.SqlParser;
import dcapture.io.*;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@HttpPath(value = "/user", secured = false)
public class AppsUserService {
    private static final String EMAIL_REGEX = "^[\\w-+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-z]{2,})$";
    private SqlDatabase database;
    private Localization locale;

    @Inject
    public AppsUserService(Localization locale, SqlDatabase database) {
        this.locale = locale;
        this.database = database;
    }

    @HttpPath("/search")
    @HttpMethod("POST")
    public JsonObject search(JsonObject req) throws Exception {
        FormModel model = new FormModel(req);
        final long start = model.getLongSafe("start");
        int limit = model.getIntSafe("limit");
        limit = 0 < limit ? limit : 20;
        SelectQuery dataQuery = database.getSelectQuery().selectColumnSet("apps_user", "search");
        WhereQuery whereQuery = dataQuery.whereQuery().likeColumnSet(
                model.getStringSafe("searchText"), "apps_user", "searchText");
        dataQuery.append(whereQuery).append(" ORDER BY email, name").limit(limit, start);
        List<DataSet> dataList = dataQuery.getDataSetList();
        //
        SelectQuery totalQuery = database.getSelectQuery();
        totalQuery.append("SELECT COUNT(*) ").append(" FROM ").addTable("apps_user").append(whereQuery);
        int totalRecords = totalQuery.getInt();
        JsonObjectBuilder builder = Json.createObjectBuilder();
        SqlParser parser = new SqlParser(database);
        builder.add("apps_user", parser.getArray(dataList, "apps_user", "name", "email", "status"));
        builder.add("start", start);
        builder.add("limit", limit);
        builder.add("totalRecords", totalRecords);
        builder.add("length", dataList.size());
        return builder.build();
    }

    @HttpPath(value = "/create1", secured = false)
    @HttpMethod("POST")
    public JsonObject create1(JsonRequest request) throws Exception {
        String name = request.getString("name");
        String email = request.getString("email");
        if (notValid(name) || !isValidEmail(email)) {
            throw new IllegalArgumentException(locale.get("email.invalid"));
        }
        SelectQuery query = database.getSelectQuery();
        query.select("apps_user").append("WHERE email=?").setParameter(email).limit(1);
        DataSet appsUser = query.getDataSet();
        if (appsUser != null && !"Pending".equals(appsUser.getString("status", ""))) {
            throw new IllegalArgumentException(locale.getMessage("emailAlreadyRegistered", email));
        }
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("name", name);
        final String code = appsUser != null ? appsUser.getString("password", "") : UUID.randomUUID().toString();
        if (appsUser == null) {
            appsUser = new DataSet().set("email", email).set("name", name).set("password", code);
            appsUser.set("status", "Pending");
            database.getTransaction().insert(appsUser, "apps_user").commit();
        }
        result.add("code", code);
        return result.build();
    }

    @HttpPath(value = "/create2", secured = false)
    @HttpMethod("POST")
    public JsonObject create2(JsonRequest request) throws Exception {
        String code = request.getString("code");
        String value = request.getString("value");
        if (notValid(code)) {
            throw new IllegalArgumentException(locale.get("actionPrevious"));
        }
        if (notValid(value)) {
            throw new IllegalArgumentException(locale.get("apps_user.password.invalid"));
        }
        SelectQuery query = database.getSelectQuery().select("apps_user");
        DataSet appsUser = query.append(" WHERE password = ?").setParameter(code)
                .limit(1).getDataSet();
        if (appsUser == null) {
            throw new NullPointerException(locale.get("actionPrevious"));
        }
        appsUser.unlock().set("password", value).set("status", "Active");
        database.getTransaction().update(appsUser, "apps_user", "required").commit();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("user", appsUser.getString("name", ""));
        result.add("email", appsUser.getString("email", ""));
        return result.build();
    }

    @HttpPath("/save")
    @HttpMethod("PUT")
    public void save(JsonArray req, JsonResponse response) throws SQLException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> appsUserList = parser.getDataSetList(req, "apps_user");
        for (DataSet appsUser : appsUserList) {
            setStatus(appsUser);
            parser.hasRequiredValue(appsUser, "apps_user", "save");
        }
        database.getTransaction().update(appsUserList, "apps_user", "save").commit();
        response.success(locale.getMessage("actionSave.msg", appsUserList.size()));
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public void delete(JsonArray req, JsonResponse response) throws SQLException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> userList = parser.getDataSetList(req, "apps_user");
        database.getTransaction().delete(userList, "apps_user").commit();
        response.success(locale.getMessage("actionDelete.msg", userList.size()));
    }

    private void setStatus(DataSet source) {
        String status = (String) source.get("status");
        if ("Active".equals(status) || "Inactive".equals(status)) {
            source.set("status", status);
        } else {
            source.set("status", "Active");
        }
    }

    private boolean notValid(String code) {
        return code == null || code.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            Pattern pattern = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);
            return pattern.matcher(email).matches();
        }
        return false;
    }
}

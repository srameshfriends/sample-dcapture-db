package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.io.*;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@HttpPath(value = "/user", secured = false)
public class AppsUserService {
    private static final String EMAIL_REGEX = "^[\\w-+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-z]{2,})$";
    private static final String TABLE = "apps_user";
    private static final String[] UPDATE = new String[]{"name", "status"};
    private static final String[] CREATE_USER = new String[]{"email", "name", "password", "status"};
    private static final String[] SEARCH = new String[]{"email", "name", "status"};
    private SqlDatabase database;
    private Localization locale;

    @Inject
    public AppsUserService(Localization locale, SqlDatabase database) {
        this.locale = locale;
        this.database = database;
    }

    @HttpPath("/search")
    public JsonObject search(JsonRequest req) {
        SqlQuery query = database.getQuery();
        long start = req.getLong("start");
        int limit = req.getInt("limit", 20);
        SelectQuery selectQuery = query.selectFrom(TABLE, "email", "name", "status");
        selectQuery.like(req.getString("searchText"), "email", "name");
        selectQuery.orderBy(" email, name").limit(limit, start);
        List<DataSet> dataList = selectQuery.getDataSetList();
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(TABLE, database.getParser().getJsonArray(dataList, TABLE, SEARCH));
        builder.add("start", start);
        builder.add("limit", limit);
        builder.add("totalRecords", selectQuery.getRowCount());
        builder.add("length", dataList.size());
        return builder.build();
    }

    @HttpPath(value = "/create1", secured = false)
    public JsonObject create1(JsonRequest request) {
        final String name = request.getString("name");
        final String email = request.getString("email");
        if (notValid(name) || !isValidEmail(email)) {
            throw new IllegalArgumentException(locale.get("email.invalid"));
        }
        final JsonObjectBuilder result = Json.createObjectBuilder();
        SqlQuery query = database.getQuery();
        SelectQuery selectQuery = query.selectAll(TABLE).equalTo("email", email).limit(1);
        DataSet appsUser = selectQuery.getDataSet();
        if (appsUser != null && !"Pending".equals(appsUser.getString("status", ""))) {
            throw new IllegalArgumentException(locale.getMessage("emailAlreadyRegistered", email));
        }
        result.add("name", name);
        final String code = appsUser != null ? appsUser.getString("password", "") : UUID.randomUUID().toString();
        if (appsUser == null) {
            appsUser = new DataSet().set("email", email).set("name", name).set("password", code);
            appsUser.set("status", "Pending");
            query.insert(appsUser, TABLE, CREATE_USER).commit();
        }
        result.add("code", code);
        return result.build();
    }

    @HttpPath(value = "/create2", secured = false)
    public JsonObject create2(JsonRequest request) {
        final String code = request.getString("code");
        final String value = request.getString("value");
        if (notValid(code)) {
            throw new IllegalArgumentException(locale.get("actionPrevious"));
        }
        if (notValid(value)) {
            throw new IllegalArgumentException(locale.get("apps_user.password.invalid"));
        }
        JsonObjectBuilder result = Json.createObjectBuilder();
        SqlQuery query = database.getQuery();
        SelectQuery selectQuery = query.selectAll(TABLE).equalTo("password", code).limit(1);
        DataSet appsUser = selectQuery.getDataSet();
        if (appsUser == null) {
            throw new NullPointerException(locale.get("actionPrevious"));
        }
        appsUser.unlock().set("password", value).set("status", "Active");
        query.update(appsUser, TABLE, CREATE_USER).commit();
        result.add("user", appsUser.getString("name", ""));
        result.add("email", appsUser.getString("email", ""));
        return result.build();
    }

    @HttpPath("/update")
    @HttpMethod("PUT")
    public void update(JsonArray req, JsonResponse response) {
        SqlParser parser = database.getParser();
        List<DataSet> appsUserList = parser.getDataSetList(req, TABLE);
        for (DataSet appsUser : appsUserList) {
            setStatus(appsUser);
            parser.hasRequired(appsUser, TABLE, UPDATE);
        }
        database.getQuery().update(appsUserList, TABLE, UPDATE).commit();
        response.success(locale.getMessage("actionSave.msg", appsUserList.size()));
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public void delete(JsonArray req, JsonResponse response) {
        SqlParser parser = database.getParser();
        List<DataSet> userList = parser.getDataSetList(req, TABLE);
        database.getQuery().delete(userList, TABLE).commit();
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

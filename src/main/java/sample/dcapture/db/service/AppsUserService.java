package sample.dcapture.db.service;

import dcapture.db.core.DataSet;
import dcapture.db.core.SelectBuilder;
import dcapture.db.core.SqlDatabase;
import dcapture.db.core.SqlParser;
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
    private static final String[] CODE_NAME = new String[]{"code", "name"};
    private SqlDatabase database;
    private Localization locale;
    private SqlParser parser;

    @Inject
    public AppsUserService(Localization locale, SqlDatabase database, SqlParser parser) {
        this.locale = locale;
        this.database = database;
        this.parser = parser;
    }

    @HttpPath("/search")
    public JsonObject search(JsonRequest req) {
        return database.transact(query -> {
            long start = req.getLong("start");
            int limit = req.getInt("limit", 20);
            SelectBuilder select = query.selectFrom(TABLE, "email", "name", "status");
            select.like(req.getString("searchText"), "email", "name");
            select.orderBy(" email, name").limit(limit, start);
            List<DataSet> dataList = query.getDataSetList(select);
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add(TABLE, parser.getJsonArray(dataList, TABLE, SEARCH));
            builder.add("start", start);
            builder.add("limit", limit);
            builder.add("totalRecords", query.getCount(select));
            builder.add("length", dataList.size());
            return builder.build();
        });
    }

    @HttpPath(value = "/create1", secured = false)
    public JsonObject create1(JsonRequest request) {
        final String name = request.getString("name");
        final String email = request.getString("email");
        if (notValid(name) || !isValidEmail(email)) {
            throw new IllegalArgumentException(locale.get("email.invalid"));
        }
        return database.transact(query -> {
            DataSet appsUser = query.getDataSet(query.selectAll(TABLE).where("email", email).limit(1));
            if (appsUser != null && !"Pending".equals(appsUser.getString("status", ""))) {
                throw new IllegalArgumentException(locale.getMessage("emailAlreadyRegistered", email));
            } else if (appsUser == null) {
                appsUser = new DataSet().set("email", email).set("name", name)
                        .set("password", UUID.randomUUID().toString()).set("status", "Pending");
                query.insert(appsUser, TABLE, CREATE_USER);
            }
            appsUser.set("name", name);
            appsUser.set("code", appsUser.get("password"));
            return parser.getJsonObject(appsUser, CODE_NAME).build();
        });
    }

    @HttpPath(value = "/create2", secured = false)
    public JsonObject create2(JsonRequest request) {
        final String code = request.getString("code");
        final String value = request.getString("value");
        if (notValid(code)) {
            throw new IllegalArgumentException(locale.get("actionPrevious"));
        } else if (notValid(value)) {
            throw new IllegalArgumentException(locale.get("apps_user.password.invalid"));
        }
        return database.transact(query -> {
            DataSet appsUser = query.getDataSet(query.selectAll(TABLE).where("password", code).limit(1));
            if (appsUser == null) {
                throw new NullPointerException(locale.get("actionPrevious"));
            }
            appsUser.unlock().set("password", value).set("status", "Active");
            query.update(appsUser, TABLE, CREATE_USER);
            return parser.getJsonObject(appsUser, SEARCH).build();
        });
    }

    @HttpPath("/update")
    @HttpMethod("PUT")
    public JsonResult update(JsonArray req) {
        int count = database.transact(query -> {
            List<DataSet> userList = parser.getDataSetList(req, TABLE, null);
            for (DataSet user : userList) {
                setStatus(user);
                parser.hasRequired(user, TABLE, UPDATE);
            }
            query.update(userList, TABLE, UPDATE);
            return userList.size();
        });
        return JsonResult.send(locale.getMessage("actionSave.msg", count));
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public JsonResult delete(JsonArray req) {
        int count = database.transact(query -> {
            List<DataSet> userList = parser.getDataSetList(req, TABLE, null);
            query.delete(userList, TABLE);
            return userList.size();
        });
        return JsonResult.send(locale.getMessage("actionDelete.msg", count));
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

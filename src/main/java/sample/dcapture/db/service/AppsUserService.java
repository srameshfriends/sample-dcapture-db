package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.io.FormModel;
import dcapture.io.JsonRequest;
import dcapture.io.LocaleException;
import dcapture.io.Localization;

import javax.inject.Inject;
import javax.json.*;
import javax.ws.rs.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Path("/user")
public class AppsUserService {
    private static final String EMAIL_REGEX = "^[\\w-+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-z]{2,})$";
    private SqlDatabase database;
    private Localization locale;

    @Inject
    public AppsUserService(Localization locale, SqlDatabase database) {
        this.locale = locale;
        this.database = database;
    }

    @Path("/search")
    public JsonObject search(JsonObject req) throws Exception {
        FormModel model = new FormModel(req);
        DataSetQuery dataQuery = database.instance(DataSetQuery.class);
        dataQuery.selectColumnGroup("apps_user", "search");
        String searchText = model.getStringSafe("searchText");
        if (!searchText.isEmpty()) {
            dataQuery.add(" WHERE ").searchColumnGroup(searchText, "searchText");
        }
        dataQuery.add(" ORDER BY email, name");
        long start = model.getLongSafe("start");
        int limit = model.getIntSafe("limit");
        limit = 0 < limit ? limit : 20;
        dataQuery.limit(limit, start);
        List<DataSet> dataList = dataQuery.loadAll();
        //
        SqlQuery totalQuery = database.instance(SqlQuery.class);
        totalQuery.add("SELECT COUNT(*) ").add(" FROM ").addTable("apps_user");
        if (!searchText.isEmpty()) {
            dataQuery.add(" WHERE ").searchColumnGroup(searchText, "search");
        }
        Number totalRecords = (Number) totalQuery.getValue();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("apps_user", toUserArray(dataList));
        result.add("start", start);
        result.add("limit", limit);
        result.add("totalRecords", totalRecords.intValue());
        result.add("length", dataList.size());
        return result.build();
    }

    private JsonArray toUserArray(List<DataSet> dataSetList) {
        JsonArrayBuilder array = Json.createArrayBuilder();
        if (dataSetList != null) {
            for (DataSet dataSet : dataSetList) {
                JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("id", dataSet.getId());
                builder.add("rev", dataSet.getRev());
                builder.add("name", dataSet.getString("name", ""));
                builder.add("email", dataSet.getString("email", ""));
                builder.add("status", dataSet.getString("status", ""));
                array.add(builder);
            }
        }
        return array.build();
    }

    @Path("/create1")
    public JsonObject create1(JsonRequest request) throws Exception {
        String name = request.getString("name");
        String email = request.getString("email");
        if (notValid(name) || !isValidEmail(email)) {
            throw new IllegalArgumentException(locale.get("email.invalid"));
        }
        DataSetQuery query = database.instance(DataSetQuery.class);
        query.select("apps_user").add("WHERE email=?").setParameter(email).limit(1);
        DataSet appsUser = query.load();
        if (appsUser != null && !"Pending".equals(appsUser.getString("status", ""))) {
            throw new IllegalArgumentException(locale.getMessage("emailAlreadyRegistered", email));
        }
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("name", name);
        final String code = appsUser != null ? appsUser.getString("password", "") : UUID.randomUUID().toString();
        if (appsUser == null) {
            DataModel model = new DataModel();
            model.setValue("email", email);
            model.setValue("name", name);
            model.setValue("password", code);
            model.setValue("status", "Pending");
            SqlTransaction transaction = database.instance(SqlTransaction.class);
            transaction.begin().insert("apps_user", "edit", model).commit();
        }
        result.add("code", code);
        return result.build();
    }

    @Path("/create2")
    public JsonObject create2(JsonRequest request) throws Exception {
        String code = request.getString("code");
        String value = request.getString("value");
        if (notValid(code)) {
            throw new IllegalArgumentException(locale.get("actionPrevious"));
        }
        if (notValid(value)) {
            throw new IllegalArgumentException(locale.get("apps_user.password.invalid"));
        }
        DataSetQuery query = database.instance(DataSetQuery.class);
        DataSet appsUser = query.select("apps_user").add(" WHERE password = ?").setParameter(code).limit(1).load();
        if (appsUser == null) {
            throw new NullPointerException(locale.get("actionPrevious"));
        }
        DataModel model = new DataModel(appsUser);
        model.setValue("password", value);
        model.setValue("status", "Active");
        SqlTransaction transaction = database.instance(SqlTransaction.class);
        transaction.begin().update("apps_user", "edit", model).commit();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("user", appsUser.getString("name", ""));
        result.add("email", appsUser.getString("email", ""));
        return result.build();
    }

    @Path("/save")
    public JsonArray save(JsonArray req) throws SQLException {
        List<DataModel> modelList = SqlMapper.toDataModels(database, "apps_user", req);
        List<DataSet> dataSets = new ArrayList<>();
        for (DataModel model : modelList) {
            setStatus(model);
            String error = SqlMapper.isValid(database, "apps_user", "save", model);
            if (error != null) {
                throw new LocaleException(error);
            }
            dataSets.add(model);
        }
        SqlTransaction transaction = database.instance(SqlTransaction.class);
        transaction.begin().update("apps_user", "save", dataSets).commit();
        return req;
    }

    @Path("/delete")
    public JsonArray delete(JsonArray req) throws SQLException {
        List<DataSet> entityList = SqlMapper.toDataSets(database, "apps_user", req);
        SqlTransaction transaction = database.instance(SqlTransaction.class);
        transaction.begin().delete("apps_user", entityList).commit();
        return req;
    }

    private void setStatus(DataModel source) {
        String status = (String) source.getValue("status");
        if ("Active".equals(status) || "Inactive".equals(status)) {
            source.setValue("status", status);
        } else {
            source.setValue("status", "Active");
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

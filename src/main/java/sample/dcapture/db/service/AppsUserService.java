package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.io.JsonRequest;
import dcapture.io.LocaleException;
import dcapture.io.Localization;
import dcapture.io.Paging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.json.*;
import javax.ws.rs.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Path("/user")
public class AppsUserService extends SqlMapper {
    private static final Logger logger = LogManager.getLogger(AppsUserService.class);
    private static final String EMAIL_REGEX = "^[\\w-\\+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-z]{2,})$";
    private SqlDatabase database;
    private Localization locale;

    @Inject
    public AppsUserService(Localization locale, SqlDatabase database) {
        this.locale = locale;
        this.database = database;
    }

    @Path("/search")
    public JsonObject search(JsonObject req) {
        JsonObjectBuilder result = Json.createObjectBuilder();
        try {
            Paging paging = parsePaging(req);
            SqlTable sqlTable = database.getTable("apps_user");
            SqlQuery[] queries = querySearchCount(database, sqlTable, paging);
            SqlReader reader = database.getReader();
            List<DataSet> dataList = reader.find(sqlTable.getName(), queries[0]);
            Number count = (Number) reader.getValue(queries[1]);
            result.add("apps_user", toUserArray(dataList));
            result.add("start", paging.getStart());
            result.add("limit", paging.getLimit());
            result.add("totalRecords", count.intValue());
            result.add("length", dataList.size());
        } catch (SQLException ex) {
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
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
        SqlQuery query = querySelectAll(database, "apps_user");
        query.add("WHERE email=?").setParameter(email).limit(1);
        DataSet appsUser = database.getReader().first("apps_user", query);
        if (appsUser != null && !"Pending".equals(appsUser.getString("status", ""))) {
            throw new IllegalArgumentException(locale.getMessage("emailAlreadyRegistered", email));
        }
        SqlTransaction transaction = database.beginTransaction();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("name", name);
        final String code = appsUser != null ? appsUser.getString("password", "") : UUID.randomUUID().toString();
        if (appsUser == null) {
            DataModel model = new DataModel();
            model.setValue("email", email);
            model.setValue("name", name);

            model.setValue("password", code);
            model.setValue("status", "Pending");
            transaction.insert("apps_user", "edit", model.as());
            transaction.commit();
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

        SqlQuery query = querySelectAll(database, "apps_user");
        query.add(" WHERE password = ?").setParameter(code).limit(1);
        DataSet appsUser = database.getReader().first("apps_user", query);
        if (appsUser == null) {
            throw new NullPointerException(locale.get("actionPrevious"));
        }
        SqlTransaction transaction = database.beginTransaction();
        DataModel model = new DataModel(appsUser);
        model.setValue("password", value);
        model.setValue("status", "Active");
        transaction.update("apps_user", "edit", model.as());
        transaction.commit();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("user", appsUser.getString("name", ""));
        result.add("email", appsUser.getString("email", ""));
        return result.build();
    }

    @Path("/save")
    public JsonArray save(JsonArray req) throws SQLException {
        SqlTable sqlTable = database.getTable("apps_user");
        List<DataModel> modelList = toDataModels(database, sqlTable, req);
        List<DataSet> dataSets = new ArrayList<>();
        for (DataModel model : modelList) {
            String error = isValidRequired(sqlTable, "save", model);
            if (error != null) {
                throw new LocaleException(error);
            }
            dataSets.add(model.as());
        }
        SqlTransaction transaction = database.beginTransaction();
        transaction.update("apps_user", "save", dataSets);
        transaction.commit();
        return req;
    }

    @Path("/delete")
    public JsonArray delete(JsonArray req) throws SQLException {
        SqlTable sqlTable = database.getTable("apps_user");
        List<DataSet> entityList = toDataSets(database, sqlTable, req);
        SqlTransaction transaction = database.beginTransaction();
        transaction.delete("apps_user", entityList);
        transaction.commit();
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

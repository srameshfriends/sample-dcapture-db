package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.io.FormModel;
import dcapture.io.JsonRequest;
import dcapture.io.LocaleException;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/expense")
public class ExpenseService {
    private SqlDatabase database;

    @Inject
    public ExpenseService(SqlDatabase database) {
        this.database = database;
    }

    @Path("/search")
    public JsonObject search(JsonObject req) throws SQLException {
        FormModel model = new FormModel(req);
        final long start = model.getLongSafe("start");
        int limit = model.getIntSafe("limit");
        limit = 0 < limit ? limit : 20;
        DataSetQuery dataQuery = database.instance(DataSetQuery.class);
        dataQuery.selectColumnGroup("expense", "search");
        SqlCondition condition = database.instance(SqlCondition.class);
        condition.likeIgnoreCase(model.getStringSafe("searchText"), dataQuery.getColumns("searchText"));
        dataQuery.where(condition).add(" ORDER BY expense_date DESC, code DESC").limit(limit, start);
        List<DataSet> dataList = dataQuery.loadAll();
        //
        SqlQuery totalQuery = database.instance(SqlQuery.class);
        totalQuery.add("SELECT COUNT(*) ").add(" FROM ").addTable("expense").where(condition);
        Number totalRecords = (Number) totalQuery.getValue();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("expense", SqlMapper.toJsonArray(database, "expense", dataList));
        result.add("start", start);
        result.add("limit", limit);
        result.add("totalRecords", totalRecords.intValue());
        result.add("length", dataList.size());
        return result.build();
    }

    @Path("/save")
    public JsonArray save(JsonRequest req) throws SQLException {
        List<DataModel> modelList = SqlMapper.toDataModels(database, "expense", req.getJsonArray());
        List<DataSet> dataSets = new ArrayList<>();
        DataSet sessionUser = (DataSet)req.getSessionAttribute("session_user");
        for (DataModel model : modelList) {
            setDefaultValue(model, sessionUser);
            String error = SqlMapper.isValid(database, "expense", "required", model);
            if (error != null) {
                throw new LocaleException(error);
            }
            dataSets.add(model);
        }
        SqlTransaction transaction = database.instance(SqlTransaction.class);
        transaction.begin().save("expense", null, dataSets).commit();
        return Json.createArrayBuilder().build();
    }

    @Path("/delete")
    public JsonArray delete(JsonArray req) throws SQLException {
        List<DataSet> dataSets = SqlMapper.toDataSets(database, "expense", req);
        SqlTransaction transaction = database.instance(SqlTransaction.class);
        transaction.begin().delete("expense", dataSets).commit();
        return req;
    }

    private void setDefaultValue(DataModel source, DataSet sessionUser) {
        if (source.getValue("code") == null) {
            UUID random = UUID.randomUUID();
            String code = random.toString();
            if (4 < code.length()) {
                code = code.substring(0, 4);
            }
            source.setValue("code", code);
        }
        String status = (String) source.getValue("status");
        if ("Active".equals(status) || "Inactive".equals(status)) {
            source.setValue("status", status);
        } else {
            source.setValue("status", "Active");
        }
        if(1 > source.getId()) {
            System.out.println("Created By : " + sessionUser.getId());
            source.setValue("created_by", sessionUser);
        }
    }
}

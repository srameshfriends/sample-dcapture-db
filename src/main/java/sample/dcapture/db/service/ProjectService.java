package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.io.FormModel;
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

@Path("/project")
public class ProjectService {
    private SqlDatabase database;

    @Inject
    public ProjectService(SqlDatabase database) {
        this.database = database;
    }

    @Path("/search")
    public JsonObject search(JsonObject req) throws SQLException {
        FormModel model = new FormModel(req);
        long start = model.getLongSafe("start");
        int limit = model.getIntSafe("limit");
        limit = 0 < limit ? limit : 20;
        DataSetQuery dataQuery = database.instance(DataSetQuery.class);
        dataQuery.selectColumnGroup("project", "search");
        SqlCondition condition = database.instance(SqlCondition.class);
        condition.likeIgnoreCase(model.getStringSafe("searchText"), dataQuery.getColumns("searchText"));
        dataQuery.where(condition).add(" ORDER BY code, name").limit(limit, start);
        List<DataSet> dataList = dataQuery.loadAll();
        //
        SqlQuery totalQuery = database.instance(SqlQuery.class);
        totalQuery.add("SELECT COUNT(*) ").add(" FROM ").addTable("project").where(condition);
        Number totalRecords = (Number) totalQuery.getValue();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("project", SqlMapper.toJsonArray(database, "project", dataList));
        result.add("start", start);
        result.add("limit", limit);
        result.add("totalRecords", totalRecords.intValue());
        result.add("length", dataList.size());
        return result.build();
    }

    @Path("/save")
    public JsonArray save(JsonArray req) throws SQLException {
        List<DataModel> modelList = SqlMapper.toDataModels(database, "project", req);
        List<DataSet> dataSets = new ArrayList<>();
        for (DataModel model : modelList) {
            setStatus(model);
            String error = SqlMapper.isValid(database, "project", "required", model);
            if (error != null) {
                throw new LocaleException(error);
            }
            dataSets.add(model);
        }
        SqlTransaction transaction = database.instance(SqlTransaction.class);
        transaction.begin().save("project", "edit", dataSets).commit();
        return req;
    }

    @Path("/delete")
    public JsonArray delete(JsonArray req) throws SQLException {
        List<DataSet> dataSets = SqlMapper.toDataSets(database, "project", req);
        SqlTransaction transaction = database.instance(SqlTransaction.class);
        transaction.begin().delete("project", dataSets).commit();
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
}

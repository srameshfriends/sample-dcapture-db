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

@Path("/currency")
public class CurrencyService {
    private SqlDatabase database;

    @Inject
    public CurrencyService(SqlDatabase database) {
        this.database = database;
    }

    @Path("/search")
    public JsonObject search(JsonObject req) throws SQLException {
        FormModel model = new FormModel(req);
        DataSetQuery dataQuery = database.instance(DataSetQuery.class);
        dataQuery.selectColumnGroup("currency", "search");
        String searchText = model.getStringSafe("searchText");
        if (!searchText.isEmpty()) {
            dataQuery.add(" WHERE ").searchColumnGroup(searchText, "searchText");
        }
        dataQuery.add(" ORDER BY code, name");
        long start = model.getLongSafe("start");
        int limit = model.getIntSafe("limit");
        limit = 0 < limit ? limit : 20;
        dataQuery.limit(limit, start);
        List<DataSet> dataList = dataQuery.loadAll();
        //
        SqlQuery totalQuery = database.instance(SqlQuery.class);
        totalQuery.add("SELECT COUNT(*) ").add(" FROM ").addTable("currency");
        if (!searchText.isEmpty()) {
            dataQuery.add(" WHERE ").searchColumnGroup(searchText, "search");
        }
        Number totalRecords = (Number) totalQuery.getValue();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("currency", SqlMapper.toJsonArray(database, "currency", dataList));
        result.add("start", start);
        result.add("limit", limit);
        result.add("totalRecords", totalRecords.intValue());
        result.add("length", dataList.size());
        return result.build();
    }

    @Path("/save")
    public JsonArray save(JsonArray req) throws SQLException {
        List<DataModel> modelList = SqlMapper.toDataModels(database, "currency", req);
        List<DataSet> dataSets = new ArrayList<>();
        for (DataModel model : modelList) {
            setStatus(model);
            String error = SqlMapper.isValid(database, "currency", "required", model);
            if (error != null) {
                throw new LocaleException(error);
            }
            dataSets.add(model);
        }
        SqlTransaction transaction = database.instance(SqlTransaction.class);
        transaction.begin().update("currency", "edit", dataSets).commit();
        return req;
    }

    @Path("/delete")
    public JsonArray delete(JsonArray req) throws SQLException {
        List<DataSet> dataSets = SqlMapper.toDataSets(database, "currency", req);
        SqlTransaction transaction = database.instance(SqlTransaction.class);
        transaction.begin().delete("currency", dataSets).commit();
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

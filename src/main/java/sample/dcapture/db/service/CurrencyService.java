package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.db.util.SqlParser;
import dcapture.db.util.SqlServletRequest;
import dcapture.db.util.SqlServletResponse;
import dcapture.io.*;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@HttpPath("/currency")
public class CurrencyService {
    private SqlDatabase database;
    private Localization localization;

    @Inject
    public CurrencyService(SqlDatabase database, Localization localization) {
        this.database = database;
        this.localization = localization;
    }

    @HttpPath("/search")
    @HttpMethod("POST")
    public JsonObject search(JsonObject req) throws SQLException {
        SqlParser parser = new SqlParser(database);
        FormModel model = new FormModel(req);
        long start = model.getLongSafe("start");
        int limit = model.getIntSafe("limit");
        limit = 0 < limit ? limit : 20;
        SelectQuery dataQuery = database.getSelectQuery().select("currency");
        WhereQuery whereQuery = dataQuery.whereQuery().likeColumnSet(
                model.getStringSafe("searchText"), "currency", "searchText");
        dataQuery.append(whereQuery).append(" ORDER BY code, name").limit(limit, start);
        List<DataSet> dataList = dataQuery.getDataSetList();
        //
        SelectQuery totalQuery = database.getSelectQuery();
        totalQuery.append("SELECT COUNT(*) FROM ").addTable("currency").append(whereQuery);
        Number totalRecords = (Number) totalQuery.getValue();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("currency", parser.getArray(dataList, "currency"));
        result.add("start", start);
        result.add("limit", limit);
        result.add("totalRecords", totalRecords.intValue());
        result.add("length", dataList.size());
        return result.build();
    }

    @HttpPath("/save")
    @HttpMethod("PUT")
    public void save(JsonArray req, JsonResponse response) throws SQLException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> modelList = parser.getDataSetList(req, "currency");
        for (DataSet model : modelList) {
            setStatus(model);
            parser.hasRequiredValue(model, "currency");
        }
        SqlTransaction transaction = database.getTransaction();
        transaction.save(modelList, "currency").commit();
        response.success(localization.getMessage("actionSave.msg", modelList.size()));
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public void delete(JsonArray req, JsonResponse response) throws SQLException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> dataSets = parser.getDataSetList(req, "currency");
        SqlTransaction transaction = database.getTransaction();
        transaction.delete(dataSets, "currency").commit();
        response.success(localization.getMessage("actionDelete.msg", dataSets.size()));
    }

    @HttpPath("/import/csv")
    @HttpMethod("PUT")
    public void importCsv(SqlServletRequest request, HtmlResponse response) throws SQLException, IOException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> modelList = request.getDataSetsFromCsv(database, "currency");
        for (DataSet model : modelList) {
            setStatus(model);
            parser.hasRequiredValue(model, "currency");
        }
        database.getTransaction().insert(modelList, "currency").commit();
        response.success(localization.getMessage("actionImport.msg", modelList.size()));
    }

    @HttpPath("/export/csv")
    public void exportCsv(SqlServletResponse response) throws SQLException, IOException {
        SelectQuery query = database.getSelectQuery();
        query.select("currency", "code", "name", "symbol", "precision").append(" ORDER BY code, name");
        response.sendAttachment("text/csv", "currency", query);
    }

    private void setStatus(DataSet source) {
        String status = (String) source.get("status");
        if ("Active".equals(status) || "Inactive".equals(status)) {
            source.set("status", status);
        } else {
            source.set("status", "Active");
        }
    }
}

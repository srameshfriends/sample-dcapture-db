package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.db.util.SqlParser;
import dcapture.db.util.SqlServletRequest;
import dcapture.db.util.SqlServletResponse;
import dcapture.io.FormModel;
import dcapture.io.HtmlResponse;
import dcapture.io.Localization;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Path;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Path("/currency")
public class CurrencyService {
    private SqlDatabase database;
    private Localization localization;

    @Inject
    public CurrencyService(SqlDatabase database, Localization localization) {
        this.database = database;
        this.localization = localization;
    }

    @Path("/search")
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

    @Path("/save")
    public JsonArray save(JsonArray req) throws SQLException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> modelList = parser.getDataSetList(req, "currency");
        for (DataSet model : modelList) {
            setStatus(model);
            parser.hasRequiredValue(model, "currency");
        }
        SqlTransaction transaction = database.getTransaction();
        transaction.begin().save(modelList, "currency").commit();
        return req;
    }

    @Path("/delete")
    public JsonArray delete(JsonArray req) throws SQLException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> dataSets = parser.getDataSetList(req, "currency");
        SqlTransaction transaction = database.getTransaction();
        transaction.begin().delete(dataSets, "currency").commit();
        return req;
    }

    @Path("/import/csv")
    public void importCsv(SqlServletRequest request, HtmlResponse response) throws SQLException, IOException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> modelList = request.getDataSetsFromCsv(database, "currency");
        for (DataSet model : modelList) {
            setStatus(model);
            parser.hasRequiredValue(model, "currency");
        }
        SqlTransaction transaction = database.getTransaction();
        transaction.begin().insert(modelList, "currency").commit();
        response.success(localization.getMessage("recordsImported.msg", modelList.size()));
    }

    @Path("/export/csv")
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

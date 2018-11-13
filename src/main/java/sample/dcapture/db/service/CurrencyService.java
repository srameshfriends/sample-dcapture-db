package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.io.*;
import sample.dcapture.db.shared.SqlServletUtils;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@HttpPath("/currency")
public class CurrencyService {
    private static final String TABLE = "currency";
    private static final String[] INSERT = new String[]{"code", "name", "symbol", "precision"};
    private static final String[] REQUIRED = new String[]{"code", "name", "symbol"};
    private static final String[] UPDATE = new String[]{"name", "symbol", "precision"};
    private SqlDatabase database;
    private Localization localization;

    @Inject
    public CurrencyService(SqlDatabase database, Localization localization) {
        this.database = database;
        this.localization = localization;
    }

    @HttpPath("/search")
    public JsonObject search(JsonRequest req) {
        SqlParser parser = database.getParser();
        SqlQuery query = database.getQuery();
        long start = req.getLong("start");
        int limit = req.getInt("limit", 20);
        SelectQuery selectQry = query.selectAll(TABLE);
        selectQry.like(req.getString("searchText"), "code", "name").orderBy("code, name").limit(limit, start);
        List<DataSet> dataList = selectQry.getDataSetList();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add(TABLE, parser.getJsonArray(dataList, TABLE));
        result.add("start", start);
        result.add("limit", limit);
        result.add("totalRecords", selectQry.getRowCount());
        result.add("length", dataList.size());
        return result.build();
    }

    @HttpPath("/save")
    @HttpMethod("PUT")
    public void save(JsonArray req, JsonResponse response) {
        SqlParser parser = database.getParser();
        List<DataSet> sourceList = parser.getDataSetList(req, TABLE);
        List<DataSet> insertList = new ArrayList<>();
        List<DataSet> updateList = new ArrayList<>();
        for (DataSet source : sourceList) {
            parser.hasRequired(source, TABLE, REQUIRED);
            if (0 == source.getLong("id")) {
                insertList.add(source);
            } else {
                updateList.add(source);
            }
        }
        if (!insertList.isEmpty()) {
            database.getQuery().insert(insertList, TABLE, INSERT);
        }
        if (!updateList.isEmpty()) {
            database.getQuery().update(updateList, TABLE, UPDATE);
        }
        database.getQuery().commit();
        response.success(localization.getMessage("actionSave.msg", sourceList.size()));
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public void delete(JsonArray req, JsonResponse response) {
        SqlParser parser = database.getParser();
        List<DataSet> dataSets = parser.getDataSetList(req, TABLE);
        database.getQuery().delete(dataSets, TABLE).commit();
        response.success(localization.getMessage("actionDelete.msg", dataSets.size()));
    }

    @HttpPath("/import/csv")
    @HttpMethod("PUT")
    public void importCsv(HtmlRequest request, HtmlResponse response) throws IOException {
        SqlParser parser = database.getParser();
        List<DataSet> modelList = SqlServletUtils.getDataSetsFromCsv(request, database, TABLE);
        for (DataSet model : modelList) {
            parser.hasRequired(model, TABLE, REQUIRED);
        }
        database.getQuery().insert(modelList, TABLE, INSERT).commit();
        response.success(localization.getMessage("actionImport.msg", modelList.size()));
    }

    @HttpPath("/export/csv")
    public void exportCsv(HttpServletResponse response) {
        SelectQuery query = database.getQuery().selectFrom(TABLE, "code, name, symbol, precision ")
                .orderBy("code, name");
        SqlServletUtils.sendCsvAttachment(response, query, TABLE);
    }
}

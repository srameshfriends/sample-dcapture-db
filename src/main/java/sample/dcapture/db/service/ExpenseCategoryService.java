package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.io.*;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.List;

@HttpPath("/expense_category")
public class ExpenseCategoryService {
    private static final String TABLE = "expense_category";
    private static final String[] REQUIRED = new String[]{"code", "name"};
    private static final String[] INSERT = new String[]{"code", "name"};
    private static final String[] UPDATE = new String[]{"name"};
    private SqlDatabase database;
    private Localization localization;

    @Inject
    public ExpenseCategoryService(SqlDatabase database, Localization localization) {
        this.database = database;
        this.localization = localization;
    }

    @HttpPath("/search")
    public JsonObject search(JsonRequest req) {
        SqlParser parser = database.getParser();
        final long start = req.getLong("start");
        final int limit = req.getInt("limit", 20);
        JsonObjectBuilder result = Json.createObjectBuilder();
        SqlQuery query = database.getQuery();
        SelectQuery dataQuery = query.selectAll(TABLE);
        dataQuery.like(req.getString("searchText"), REQUIRED);
        dataQuery.orderBy("code, name").limit(limit, start);
        List<DataSet> dataList = dataQuery.getDataSetList();
        result.add(TABLE, parser.getJsonArray(dataList, TABLE));
        result.add("start", start);
        result.add("limit", limit);
        result.add("totalRecords", dataQuery.getRowCount());
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
        List<DataSet> dataSets = parser.getDataSetList(req, "expense_category");
        database.getQuery().delete(dataSets, "expense_category").commit();
        response.success(localization.getMessage("actionDelete.msg", dataSets.size()));
    }
}

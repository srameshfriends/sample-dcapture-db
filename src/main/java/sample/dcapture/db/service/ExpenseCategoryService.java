package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.db.util.SqlParser;
import dcapture.io.*;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.sql.SQLException;
import java.util.List;

@HttpPath("/expense_category")
public class ExpenseCategoryService {
    private SqlDatabase database;
    private Localization localization;

    @Inject
    public ExpenseCategoryService(SqlDatabase database, Localization localization) {
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
        SelectQuery dataQuery = database.getSelectQuery().select("expense_category");
        WhereQuery whereQuery = dataQuery.whereQuery();
        whereQuery.likeColumnSet(model.getStringSafe("searchText"), "expense_category", "searchText");
        dataQuery.append(whereQuery).append(" ORDER BY code, name").limit(limit, start);
        List<DataSet> dataList = dataQuery.getDataSetList();
        //
        SelectQuery totalQuery = database.getSelectQuery();
        totalQuery.append("SELECT COUNT(*) FROM ").addTable("expense_category").append(whereQuery);
        int totalRecords = totalQuery.getInt();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("expense_category", parser.getArray(dataList, "expense_category"));
        result.add("start", start);
        result.add("limit", limit);
        result.add("totalRecords", totalRecords);
        result.add("length", dataList.size());
        return result.build();
    }

    @HttpPath("/save")
    @HttpMethod("PUT")
    public void save(JsonArray req, JsonResponse response) throws SQLException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> categoryList = parser.getDataSetList(req, "expense_category");
        for (DataSet category : categoryList) {
            setStatus(category);
            parser.hasRequiredValue(category, "expense_category");
        }
        database.getTransaction().save(categoryList, "expense_category").commit();
        response.success(localization.getMessage("actionSave.msg", categoryList.size()));
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public void delete(JsonArray req, JsonResponse response) throws SQLException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> dataSets = parser.getDataSetList(req, "expense_category");
        database.getTransaction().delete(dataSets, "expense_category").commit();
        response.success(localization.getMessage("actionDelete.msg", dataSets.size()));
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

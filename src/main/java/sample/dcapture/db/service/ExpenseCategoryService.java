package sample.dcapture.db.service;

import dcapture.db.core.DataSet;
import dcapture.db.core.SelectBuilder;
import dcapture.db.core.SqlDatabase;
import dcapture.db.core.SqlParser;
import dcapture.io.*;
import sample.dcapture.db.shared.KeySequence;
import sample.dcapture.db.shared.SearchResult;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

@HttpPath("/expense_category")
public class ExpenseCategoryService {
    private static final String TABLE = "expense_category";
    private static final String[] REQUIRED = new String[]{"code", "name"};
    private static final String[] INSERT = REQUIRED, UPDATE = REQUIRED;
    private final SqlDatabase database;
    private final SqlParser parser;
    private final Localization locale;

    @Inject
    public ExpenseCategoryService(SqlDatabase database, SqlParser parser, Localization locale) {
        this.database = database;
        this.parser = parser;
        this.locale = locale;
    }

    @HttpPath("/search")
    public JsonObject search(JsonRequest req) {
        return database.transact(query -> {
            final long start = req.getLong("start");
            final int limit = req.getInt("limit", SearchResult.PAGE_LIMIT);
            SelectBuilder select = query.selectAll(TABLE).like(req.getString("searchText"), REQUIRED)
                    .orderBy("code, name").limit(limit, start);
            List<DataSet> sourceList = query.getDataSetList(select);
            SearchResult result = new SearchResult(start, limit).setTotalRecords(query.getCount(select));
            result.setData(TABLE, parser.getJsonArray(sourceList, TABLE, null));
            return result.build();
        });
    }

    @HttpPath("/reload")
    public JsonObject load(JsonRequest req) {
        return database.transact(query -> {
            DataSet source = parser.getDataSet(req.getJsonObject(), TABLE, null);
            SelectBuilder select = query.selectAll(TABLE);
            if (source.isPrimaryId("id")) {
                select.where("id", source.getLong("id")).limit(1);
                source = query.getDataSet(select);
            } else {
                source.set("code", "AUTO");
            }
            return parser.getJsonObject(source, TABLE, null).build();
        });
    }

    @HttpPath("/save")
    @HttpMethod("PUT")
    public JsonResult save(JsonArray req) {
        int count = database.transact(query -> {
            List<DataSet> sourceList = parser.getDataSetList(req, TABLE, null);
            List<DataSet> insertList = new ArrayList<>();
            List<DataSet> updateList = new ArrayList<>();
            KeySequence keySequence = KeySequence.create(query);
            for (DataSet source : sourceList) {
                String code = source.getString("code", "");
                if (code.isEmpty() || "AUTO".equals(code.toUpperCase())) {
                    keySequence.generate(TABLE, source, "code");
                }
                parser.hasRequired(source, TABLE, REQUIRED);
                if (0 == source.getLong("id")) {
                    insertList.add(source);
                } else {
                    updateList.add(source);
                }
            }
            if (!insertList.isEmpty()) {
                query.insert(insertList, TABLE, INSERT);
            }
            if (!updateList.isEmpty()) {
                query.update(updateList, TABLE, UPDATE);
            }
            return insertList.size() + updateList.size();
        });
        return JsonResult.send(locale.getMessage("actionSave.msg", count));
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public JsonResult delete(JsonArray req) {
        int count = database.transact(query -> {
            List<DataSet> sourceList = parser.getDataSetList(req, "expense_category", null);
            query.delete(sourceList, "expense_category");
            return sourceList.size();
        });
        return JsonResult.send(locale.getMessage("actionDelete.msg", count));
    }
}

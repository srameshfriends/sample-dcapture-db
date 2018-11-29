package sample.dcapture.db.service;

import dcapture.db.core.DataSet;
import dcapture.db.core.SelectBuilder;
import dcapture.db.core.SqlDatabase;
import dcapture.db.core.SqlParser;
import dcapture.io.*;
import sample.dcapture.db.shared.SearchResult;
import sample.dcapture.db.shared.SqlServletUtils;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@HttpPath("/currency")
public class CurrencyService {
    private static final String TABLE = "currency";
    private static final String[] REQUIRED = new String[]{"code", "name", "symbol"};
    private static final String[] INS_UPD_EXT = new String[]{"code", "name", "symbol", "precision"};
    private SqlDatabase database;
    private Localization locale;
    private SqlParser parser;

    @Inject
    public CurrencyService(SqlDatabase database, SqlParser parser, Localization locale) {
        this.database = database;
        this.parser = parser;
        this.locale = locale;
    }

    @HttpPath("/search")
    public JsonObject search(JsonRequest req) {
        return database.transact(query -> {
            long start = req.getLong("start");
            int limit = req.getInt("limit", 20);
            SelectBuilder select = query.selectAll(TABLE).like(req.getString("searchText"), "code, name")
                    .orderBy("code, name").limit(limit, start);
            List<DataSet> dataList = query.getDataSetList(select);
            SearchResult result = new SearchResult(start, limit).setTotalRecords(query.getCount(select));
            result.setData(TABLE, parser.getJsonArray(dataList, TABLE, null));
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
                source.set("precision", 4);
            }
            return parser.getJsonObject(source, TABLE, null).build();
        });
    }

    @HttpPath("/save")
    @HttpMethod("PUT")
    public JsonResult save(JsonArray req) {
        List<DataSet> sourceList = parser.getDataSetList(req, TABLE, null);
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
        int count = database.transact(query -> {
            if (!insertList.isEmpty()) {
                query.insert(insertList, TABLE, INS_UPD_EXT);
            }
            if (!updateList.isEmpty()) {
                query.update(updateList, TABLE, INS_UPD_EXT);
            }
            return insertList.size() + updateList.size();
        });
        return JsonResult.send(locale.getMessage("actionSave.msg", count));
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public JsonResult delete(JsonArray req) {
        int count = database.transact(query -> {
            List<DataSet> sourceList = parser.getDataSetList(req, TABLE, null);
            query.delete(sourceList, TABLE);
            return sourceList.size();
        });
        return JsonResult.send(locale.getMessage("actionDelete.msg", count));
    }

    @HttpPath("/import/csv")
    @HttpMethod("PUT")
    public void importCsv(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<DataSet> sourceList = SqlServletUtils.getDataSetsFromCsv(request, TABLE);
        for (DataSet model : sourceList) {
            parser.hasRequired(model, TABLE, REQUIRED);
        }
        int count = database.transact(query -> {
            query.insert(sourceList, TABLE, INS_UPD_EXT);
            return sourceList.size();
        });
        TextMessage.onSuccess(response, locale.getMessage("actionImport.msg", count));
    }

    @HttpPath("/export/csv")
    public void exportCsv(HttpServletResponse response) {
        database.transact(query -> {
            SelectBuilder select = query.selectFrom(TABLE, INS_UPD_EXT).orderBy("code, name");
            return SqlServletUtils.sendCsvAttachment(response, TABLE, query.getResult(select));
        });
    }
}

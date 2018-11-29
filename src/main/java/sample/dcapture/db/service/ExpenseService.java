package sample.dcapture.db.service;

import dcapture.db.core.DataSet;
import dcapture.db.core.SelectBuilder;
import dcapture.db.core.SqlDatabase;
import dcapture.db.core.SqlParser;
import dcapture.io.*;
import sample.dcapture.db.shared.KeySequence;
import sample.dcapture.db.shared.SearchResult;
import sample.dcapture.db.shared.SqlServletUtils;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@HttpPath("/expense")
public class ExpenseService {
    private static final String TABLE = "expense", SESSION_USER = "session_user";
    private static final String[] REQUIRED = new String[]{"code", "description", "expense_category", "currency", "amount"};
    private static final String SELECT_QRY = "ex.id, ex.expense_date, ex.code, ex.description, ec.id, ec.code, ec.name, cu.id, cu.symbol, cu.code, ex.amount";
    private static final String[] INSERT = new String[]{"expense_date", "code", "description", "expense_category", "currency", "amount", "created_by", "created_on"};
    private static final String[] UPDATE = new String[]{"expense_date", "code", "description", "expense_category", "currency", "amount"};
    private static final String[] CSV_EXPORT = new String[]{"expense_date", "code", "description", "category.code", "currency.code", "amount"};
    private final SqlDatabase database;
    private final SqlParser parser;
    private final Localization locale;

    @Inject
    public ExpenseService(SqlDatabase database, SqlParser parser, Localization locale) {
        this.database = database;
        this.parser = parser;
        this.locale = locale;
    }

    @HttpPath("/search")
    public JsonObject search(JsonRequest req) {
        final long start = req.getLong("start");
        final int limit = req.getInt("limit", SearchResult.PAGE_LIMIT);
        return database.transact(query -> {
            SelectBuilder select = query.selectFrom("expense ex", SELECT_QRY);
            select.leftJoin("expense_category ec ON ec.id=ex.expense_category").leftJoin("currency cu ON cu.id=ex.currency");
            select.like(req.getString("searchText"), "ex.code", "ex.description", "ec.name");
            select.orderBy("ex.expense_date DESC, ex.code DESC").limit(limit, start);
            List<DataSet> sourceList = query.getDataSetList(select);
            SearchResult result = new SearchResult(start, limit).setTotalRecords(query.getCount(select));
            result.setData(TABLE, parser.getJsonArray(sourceList, TABLE, null));
            return result.build();
        });
    }

    @HttpPath("/save")
    @HttpMethod("PUT")
    public JsonResult save(JsonRequest req) {
        int count = database.transact(query -> {
            List<DataSet> sourceList = parser.getDataSetList(req.getJsonArray(), TABLE, null);
            List<DataSet> insertList = new ArrayList<>();
            List<DataSet> updateList = new ArrayList<>();
            KeySequence sequence = KeySequence.create(query);
            DataSet sessionUser = (DataSet) req.getSessionAttribute(SESSION_USER);
            for (DataSet source : sourceList) {
                setDefault(source, sessionUser, sequence);
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

    @HttpPath("/update")
    @HttpMethod("PUT")
    public JsonResult update(JsonRequest req) {
        int count = database.transact(query -> {
            List<DataSet> sourceList = parser.getDataSetList(req.getJsonArray(), TABLE, null);
            DataSet sessionUser = (DataSet) req.getSessionAttribute(SESSION_USER);
            KeySequence sequence = KeySequence.create(query);
            for (DataSet source : sourceList) {
                setDefault(source, sessionUser, sequence);
                parser.hasRequired(source, TABLE, REQUIRED);
            }
            query.update(sourceList, TABLE, UPDATE);
            return sourceList.size();
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

    @HttpPath("/reload")
    public JsonObject load(JsonRequest req) {
        return database.transact(query -> {
            DataSet expense = parser.getDataSet(req.getJsonObject(), TABLE, null);
            SelectBuilder select = query.selectAll(TABLE);
            if (0 < expense.getLong("id")) {
                select.where("id", expense.getLong("id")).limit(1);
                expense = query.getDataSet(select);
            } else if (!expense.getString("code", "").isEmpty()) {
                select.where("code", expense.getString("code")).limit(1);
                expense = query.getDataSet(select);
            } else {
                expense.set("code", "AUTO");
                expense.set("expense_date", LocalDate.now());
            }
            return parser.getJsonObject(expense, TABLE, null).build();
        });
    }

    @HttpPath("/export/csv")
    public void exportCsv(HttpServletResponse res) {
        database.transact(query -> {
            SelectBuilder select = query.selectFrom("expense ex",
                    "ex.expense_date, ex.code, ex.description, ec.code, cu.code, ex.amount")
                    .leftJoin("currency cu ON cu.id = ex.currency")
                    .leftJoin("  expense_category ec ON ec.id = ex.expense_category")
                    .orderBy(" ex.expense_date, ex.code");
            return SqlServletUtils.sendCsvAttachment(res, CSV_EXPORT, "expense", query.getObjectsList(select));
        });
    }

    private void setDefault(DataSet source, DataSet sessionUser, KeySequence sequence) {
        LocalDate localDate = source.getLocalDate("expense_date");
        if (localDate == null) {
            localDate = LocalDate.now();
            source.set("expense_date", localDate);
        }
        String code = source.getString("code", "");
        if (code.isEmpty() || "AUTO".equals(code.toUpperCase())) {
            sequence.generate(TABLE, source, "code", "expense_date");
        }
        if (0 == source.getLong("id")) {
            source.set("created_by", sessionUser);
            source.set("created_on", LocalDateTime.now());
        }
    }
}

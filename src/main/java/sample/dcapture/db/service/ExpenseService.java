package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.io.*;
import sample.dcapture.db.shared.KeySequence;
import sample.dcapture.db.shared.SqlServletUtils;

import javax.inject.Inject;
import javax.json.*;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@HttpPath("/expense")
public class ExpenseService {
    private static final String TABLE = "expense", SESSION_USER = "session_user";
    private static final String[] REQUIRED = new String[]{"code", "description", "expense_category", "currency", "amount"};
    private static final String[] INSERT = new String[]{"expense_date", "code", "description", "expense_category", "currency", "amount", "created_by", "created_on"};
    private static final String[] UPDATE = new String[]{"expense_date", "code", "description", "expense_category", "currency", "amount"};
    private static final String[] CSV_EXPORT = new String[]{"expense_date", "code", "description", "category.code", "currency.code", "amount"};

    private SqlDatabase database;
    private Localization localization;

    @Inject
    public ExpenseService(SqlDatabase database, Localization localization) {
        this.database = database;
        this.localization = localization;
    }

    @HttpPath("/search")
    public JsonObject search(JsonRequest req) {
        final long start = req.getLong("start");
        final int limit = req.getInt("limit", 20);
        SqlQuery query = database.getQuery();
        SelectQuery select = query.selectFrom("expense ex",
                "ex.id, ex.expense_date, ex.code, ex.description, ec.code, ec.name, cu.code, ex.amount");
        select.join("expense_category ec ON ec.id=ex.expense_category").join("currency cu ON cu.id=ex.currency");
        select.like(req.getString("searchText"), "expense", "ex.code", "ex.description", "ec.name");
        select.orderBy("ex.expense_date DESC, ex.code DESC").limit(limit, start);
        List<DataSet> dataList = select.getDataSetList();
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add(TABLE, database.getParser().getJsonArray(dataList, TABLE));
        result.add("start", start);
        result.add("limit", limit);
        result.add("totalRecords", select.getRowCount());
        result.add("length", dataList.size());
        return result.build();
    }

    @HttpPath("/save")
    @HttpMethod("PUT")
    public void save(JsonRequest req, JsonResponse response) {
        SqlParser parser = database.getParser();
        List<DataSet> sourceList = parser.getDataSetList(req.getJsonArray(), TABLE);
        List<DataSet> insertList = new ArrayList<>();
        List<DataSet> updateList = new ArrayList<>();
        KeySequence sequence = KeySequence.create(database.getQuery());
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
            database.getQuery().insert(insertList, TABLE, INSERT);
        }
        if (!updateList.isEmpty()) {
            database.getQuery().update(updateList, TABLE, UPDATE);
        }
        database.getQuery().commit();
        response.success(localization.getMessage("actionSave.msg", sourceList.size()));
    }

    @HttpPath("/update")
    @HttpMethod("PUT")
    public void update(JsonRequest req, JsonResponse response) {
        SqlParser parser = database.getParser();
        List<DataSet> sourceList = parser.getDataSetList(req.getJsonArray(), TABLE);
        DataSet sessionUser = (DataSet) req.getSessionAttribute(SESSION_USER);
        SqlQuery query = database.getQuery();
        KeySequence sequence = KeySequence.create(query);
        for (DataSet source : sourceList) {
            setDefault(source, sessionUser, sequence);
            parser.hasRequired(source, TABLE, REQUIRED);
        }
        query.update(sourceList, TABLE, UPDATE).commit();
        response.success(localization.getMessage("actionSave.msg", sourceList.size()));
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public void delete(JsonArray req, JsonResponse response) {
        List<DataSet> dataSets = database.getParser().getDataSetList(req, TABLE);
        database.getQuery().delete(dataSets, TABLE).commit();
        response.success(localization.getMessage("actionDelete.msg", dataSets.size()));
    }

    @HttpPath("/reload")
    public JsonObject load(JsonRequest req) {
        SqlParser parser = database.getParser();
        DataSet expense = parser.getDataSet(req.getJsonObject(), TABLE);
        SelectQuery selectQuery = database.getQuery().selectAll(TABLE);
        if (0 < expense.getLong("id")) {
            selectQuery.equalTo("id", expense.getLong("id"));
            expense = selectQuery.limit(1).getDataSet();
        } else if (!expense.getString("code", "").isEmpty()) {
            selectQuery.equalTo("code", expense.getString("code"));
            expense = selectQuery.limit(1).getDataSet();
        } else {
            expense.set("code", "AUTO");
            expense.set("expense_date", LocalDate.now());
        }
        return parser.getJsonObject(expense, TABLE).build();
    }

    @HttpPath("/export/csv")
    public void exportCsv(HttpServletResponse res) {
        SelectQuery selectQuery = database.getQuery().selectFrom("expense ex",
                "ex.expense_date, ex.code, ex.description, ec.code, cu.code, ex.amount")
                .join(" INNER JOIN currency cu ON cu.id = ex.currency INNER JOIN expense_category ec")
                .join(" ON ec.id = ex.expense_category").orderBy(" ex.expense_date, ex.code");
        SqlServletUtils.sendCsvAttachment(res, selectQuery, CSV_EXPORT, "expense");
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

package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.db.util.SqlParser;
import dcapture.io.FormModel;
import dcapture.io.JsonRequest;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Path("/expense")
public class ExpenseService {
    private SqlDatabase database;

    @Inject
    public ExpenseService(SqlDatabase database) {
        this.database = database;
    }

    @Path("/search")
    public JsonObject search(JsonObject req) throws SQLException {
        FormModel model = new FormModel(req);
        final long start = model.getLongSafe("start");
        int limit = model.getIntSafe("limit");
        limit = 0 < limit ? limit : 20;
        SelectQuery dataQuery = database.getSelectQuery().selectColumnSet("expense", "search");
        WhereQuery whereQuery = dataQuery.whereQuery()
                .likeColumnSet(model.getStringSafe("searchText"), "expense", "searchText");
        dataQuery.append(whereQuery).append(" ORDER BY expense_date DESC, code DESC").limit(limit, start);
        List<DataSet> dataList = dataQuery.getDataSetList();
        //
        SelectQuery totalQuery = database.getSelectQuery()
                .append("SELECT COUNT(*) FROM ").addTable("expense").append(whereQuery);
        int totalRecords = totalQuery.getInt();
        SqlParser parser = new SqlParser(database);
        JsonArray array = parser.getArray(dataList, "expense");
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("expense", array);
        result.add("start", start);
        result.add("limit", limit);
        result.add("totalRecords", totalRecords);
        result.add("length", dataList.size());
        return result.build();
    }

    @Path("/save")
    public JsonArray save(JsonRequest req) throws SQLException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> expenseList = parser.getDataSetList(req.getJsonArray(), "expense");
        DataSet sessionUser = (DataSet) req.getSessionAttribute("session_user");
        for (DataSet expense : expenseList) {
            setDefaultValue(expense, sessionUser);
            parser.hasRequiredValue(expense, "expense");
        }
        SqlTransaction transaction = database.getTransaction();
        transaction.begin().save(expenseList, "expense").commit();
        return Json.createArrayBuilder().build();
    }

    @Path("/delete")
    public JsonArray delete(JsonArray req) throws SQLException {
        SqlParser parser = new SqlParser(database);
        List<DataSet> dataSets = parser.getDataSetList(req, "expense");
        SqlTransaction transaction = database.getTransaction();
        transaction.begin().delete(dataSets, "expense").commit();
        return req;
    }

    @Path("/reload")
    public JsonObject load(JsonRequest req) throws SQLException {
        SqlParser parser = new SqlParser(database);
        DataSet expense = parser.getDataSet(req.getJsonObject(), "expense");
        SelectQuery selectQuery = database.getSelectQuery().select("expense");
        if (expense.isValidPId()) {
            selectQuery.append(selectQuery.whereQuery().equalTo("id", expense.getPId()));
            expense = selectQuery.limit(1).getDataSet();
        } else if (!expense.getString("code", "").isEmpty()) {
            selectQuery.append(selectQuery.whereQuery().equalTo("code", expense.getString("code")));
            expense = selectQuery.limit(1).getDataSet();
        } else {
            expense.set("code", "AUTO");
            expense.set("expense_date", LocalDate.now());
            expense.set("status", "Active");
        }
        return parser.getObject(expense, "expense");
    }

    private void setDefaultValue(DataSet source, DataSet sessionUser) throws SQLException {
        LocalDate localDate = source.getLocalDate("expense_date");
        if (localDate == null) {
            localDate = LocalDate.now();
            source.set("expense_date", localDate);
        }
        String code = source.getString("code", "");
        if (code.isEmpty() || "AUTO".equals(code.toUpperCase())) {
            KeySequence register = new KeySequence(database);
            code = register.next("expense", localDate);
            source.set("code", code);
        }
        String status = (String) source.get("status");
        if ("Active".equals(status) || "Inactive".equals(status)) {
            source.set("status", status);
        } else {
            source.set("status", "Active");
        }
        if (0 == source.getPId()) {
            source.set("created_by", sessionUser);
        }
    }
}

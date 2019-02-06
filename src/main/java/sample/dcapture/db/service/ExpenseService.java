package sample.dcapture.db.service;

import dcapture.db.core.DataSet;
import dcapture.db.core.SelectBuilder;
import dcapture.db.core.SqlDatabase;
import dcapture.db.util.DataSetRequest;
import dcapture.db.util.DataSetResult;
import dcapture.db.util.Paging;
import dcapture.io.HttpMethod;
import dcapture.io.HttpPath;
import sample.dcapture.db.shared.KeySequence;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@HttpPath("/expense")
public class ExpenseService {
    private static final int PAGE_LIMIT = 20;
    private static final String EXPENSE_TBL = "expense", SESSION_USER_TBL = "session_user";
    private static final String[] REQUIRED = new String[]{"code", "description", "expense_category", "currency", "amount"};
    private static final String SELECT_QRY = "ex.id, ex.expense_date, ex.code, ex.description, ec.id, ec.code, ec.name, cu.id, cu.symbol, cu.code, ex.amount";
    private static final String[] INSERT = new String[]{"expense_date", "code", "description", "expense_category", "currency", "amount", "created_by", "created_on"};
    private static final String[] UPDATE = new String[]{"expense_date", "code", "description", "expense_category", "currency", "amount"};
    private static final String[] CSV_EXPORT = new String[]{"date", "code", "description", "category_code", "currency_code", "amount"};
    private final SqlDatabase database;

    @Inject
    public ExpenseService(SqlDatabase database) {
        this.database = database;
    }

    @HttpPath("/search")
    public DataSetResult search(DataSetRequest req) {
        return database.transact(query -> {
            Paging paging = req.getPaging(PAGE_LIMIT);
            SelectBuilder select = query.selectFrom("expense ex", SELECT_QRY);
            select.leftJoin("expense_category ec ON ec.id=ex.expense_category").leftJoin("currency cu ON cu.id=ex.currency");
            select.like(req.getString("searchText"), "ex.code", "ex.description", "ec.name");
            select.orderBy("ex.expense_date DESC, ex.code DESC").limit(paging.getLimit(), paging.getOffset());
            paging.setDataList(query.getDataSetList(select));
            paging.setTotalRecords(query.getCount(select));
            return DataSetResult.asJsonObject(paging, EXPENSE_TBL);
        });
    }

    @HttpPath("/save")
    @HttpMethod("PUT")
    public DataSetResult save(DataSetRequest req) {
        return database.transact(query -> {
            List<DataSet> sourceList = req.getDataSetList(EXPENSE_TBL);
            List<DataSet> insertList = new ArrayList<>();
            List<DataSet> updateList = new ArrayList<>();
            KeySequence sequence = KeySequence.create(query);
            DataSet sessionUser = (DataSet) req.getSessionAttribute(SESSION_USER_TBL);
            for (DataSet source : sourceList) {
                setDefault(source, sessionUser, sequence);
                validateRequired(source);
                if (0 == source.getLong("id")) {
                    insertList.add(source);
                } else {
                    updateList.add(source);
                }
            }
            if (!insertList.isEmpty()) {
                query.insert(insertList, EXPENSE_TBL, INSERT);
            }
            if (!updateList.isEmpty()) {
                query.update(updateList, EXPENSE_TBL, UPDATE);
            }
            return DataSetResult.success("actionSave.msg", insertList.size() + updateList.size());
        });
    }

    @HttpPath("/update")
    @HttpMethod("PUT")
    public DataSetResult update(DataSetRequest req) {
        return database.transact(query -> {
            List<DataSet> sourceList = req.getDataSetList(EXPENSE_TBL);
            DataSet sessionUser = (DataSet) req.getSessionAttribute(SESSION_USER_TBL);
            KeySequence sequence = KeySequence.create(query);
            for (DataSet source : sourceList) {
                setDefault(source, sessionUser, sequence);
                validateRequired(source);
            }
            query.update(sourceList, EXPENSE_TBL, UPDATE);
            return DataSetResult.success("actionSave.msg", sourceList.size());
        });
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public DataSetResult delete(DataSetRequest req) {
        return database.transact(query -> {
            List<DataSet> sourceList = req.getDataSetList(EXPENSE_TBL);
            query.delete(sourceList, EXPENSE_TBL);
            return DataSetResult.success("actionDelete.msg", sourceList.size());
        });
    }

    @HttpPath("/reload")
    public DataSetResult load(DataSetRequest req) {
        return database.transact(query -> {
            DataSet expense = req.getDataSet(EXPENSE_TBL);
            SelectBuilder select = query.selectAll(EXPENSE_TBL);
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
            return DataSetResult.asJsonObject(expense, EXPENSE_TBL, null);
        });
    }

    @HttpPath("/export/csv")
    public DataSetResult exportCsv() {
        return database.transact(query -> {
            SelectBuilder selectBuilder = query.selectFrom("expense ex",
                    "ex.expense_date, ex.code, ex.description, ec.code, cu.code, ex.amount")
                    .leftJoin("currency cu ON cu.id = ex.currency")
                    .leftJoin("  expense_category ec ON ec.id = ex.expense_category")
                    .orderBy(" ex.expense_date, ex.code");
            return DataSetResult.asCsv(query.getResult(selectBuilder), CSV_EXPORT, "expense");
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
            sequence.generate(EXPENSE_TBL, source, "code", "expense_date");
        }
        if (0 == source.getLong("id")) {
            source.set("created_by", sessionUser);
            source.set("created_on", LocalDateTime.now());
        }
    }

    private void validateRequired(DataSet dataSet) {
        for (String col : REQUIRED) {
            Object value = dataSet.get(col);
            if (value == null) {
                throw new RuntimeException(EXPENSE_TBL + "." + col + ".invalid");
            } else if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    throw new RuntimeException(EXPENSE_TBL + "." + col + ".invalid");
                }
            }
        }
    }
}

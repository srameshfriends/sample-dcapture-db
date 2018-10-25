package sample.dcapture.db.api;

import dcapture.db.core.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class KeySequence {
    public static final String TABLE = "key_sequence", TABLE_LINE = "key_sequence_line";
    public static final String DAILY = "DAILY", MONTHLY = "MONTHLY", YEARLY = "YEARLY", NO_RESET = "NO_RESET";
    private String dailyPattern, monthlyPattern, yearlyPattern;
    private SqlDatabase database;

    public KeySequence(SqlDatabase database) {
        this.database = database;
        dailyPattern = "yyMMdd";
        monthlyPattern = "yyMM";
        yearlyPattern = "yy";
    }

    public String next(String name) throws SQLException {
        return next(name, null);
    }

    public String next(String name, LocalDate localDate) throws SQLException {
        SelectQuery query = database.getSelectQuery().select(TABLE);
        query.append(query.whereQuery().equalTo("name", name)).limit(1);
        DataSet keySeq = query.getDataSet();
        if (keySeq == null) {
            throw new NullPointerException("Key sequence not created for " + name);
        }
        String type = keySeq.getString("type", "");
        if (localDate == null && ("MONTHLY".equals(type) || "YEARLY".equals(type) || "DAILY".equals(type))) {
            throw new NullPointerException("Key sequence (Name: " + name + ",type: " + type + ") date should not be null");
        }
        return findNext(keySeq, localDate);
    }

    private String findNext(DataSet keySeq, LocalDate localDate) throws SQLException {
        final String type = keySeq.getString("type", "");
        SelectQuery query = database.getSelectQuery().select(TABLE_LINE);
        WhereQuery whereQuery = query.whereQuery().equalTo(TABLE, keySeq.getPId());
        if ("MONTHLY".equals(type)) {
            whereQuery.and("year", localDate.getYear()).and("month", localDate.getMonthValue());
        } else if ("YEARLY".equals(type)) {
            whereQuery.and("year", localDate.getYear());
        } else if ("DAILY".equals(type)) {
            whereQuery.and("year", localDate.getYear()).and("month", localDate.getMonthValue())
                    .and("day", localDate.getDayOfMonth());
        } else {
            whereQuery.and("year", 0);
        }
        query.append(whereQuery).limit(1);
        DataSet keySeqLine = query.getDataSet();
        if (keySeqLine == null) {
            if ("MONTHLY".equals(type)) {
                keySeqLine = addMonthlyLine(keySeq, localDate);
            } else if ("YEARLY".equals(type)) {
                keySeqLine = addYearlyLine(keySeq, localDate);
            } else if ("DAILY".equals(type)) {
                keySeqLine = addDailyLine(keySeq, localDate);
            } else {
                keySeqLine = addLine(keySeq);
            }
        }
        String prefix = keySeqLine.getString("prefix", "");
        String pattern = keySeqLine.getString("pattern", "");
        String text = localDate == null ? "" : DateTimeFormatter.ofPattern(pattern).format(localDate);
        final int index = keySeqLine.getInt("last_index") + 1;
        final String key = prefix + text + index;
        UpdateQuery updateQuery = database.getUpdateQuery().update(TABLE_LINE);
        updateQuery.set("last_index", index).set("last_key", key);
        updateQuery.append(updateQuery.whereQuery().equalTo("id", keySeqLine.getPId()));
        updateQuery.executeUpdate();
        return key;
    }

    public void setSequence(String name, String type, String prefix) throws SQLException {
        if (name == null || type == null || prefix == null) {
            throw new NullPointerException("Key Sequence parameter should not be null (name, type, prefix) "
                    + name + ", " + type + ", " + prefix);
        }
        if (!(MONTHLY.equals(type) || DAILY.equals(type) || YEARLY.equals(type) || NO_RESET.equals(type))) {
            throw new NullPointerException("Key Sequence type not matched [MONTHLY, DAILY, YEARLY, NO_RESET] : " + type);
        }
        SelectQuery query = database.getSelectQuery().select(TABLE);
        query.append(query.whereQuery().equalTo("name", name)).limit(1);
        DataSet keySeq = query.getDataSet();
        if (keySeq == null) {
            InsertQuery insertQuery = database.getInsertQuery().insert(TABLE);
            insertQuery.set("name", name).set("type", type).set("prefix", prefix).executeUpdate();
        } else if (!prefix.equals(keySeq.getString("prefix", ""))) {
            UpdateQuery updateQry = database.getUpdateQuery().update(TABLE);
            updateQry.set("id", keySeq.getPId()).set("name", name).set("prefix", prefix);
            updateQry.append(updateQry.whereQuery().equalTo("id", keySeq.getPId())).executeUpdate();
        }
    }

    public void commit() throws SQLException {
        database.getProcessor().commit();
    }

    private DataSet addYearlyLine(DataSet keySeq, LocalDate localDate) throws SQLException {
        InsertQuery insertQuery = database.getInsertQuery().insert(TABLE_LINE);
        insertQuery.set(TABLE, keySeq.getPId()).set("pattern", yearlyPattern).set("year", localDate.getYear())
                .set("prefix", keySeq.get("prefix")).set("last_index", 0).set("last_key", "").executeUpdate();
        SelectQuery query = database.getSelectQuery().select(TABLE_LINE);
        WhereQuery whereQuery = query.whereQuery().equalTo(TABLE, keySeq.getPId());
        whereQuery.and("year", localDate.getYear());
        return query.append(whereQuery).limit(1).getDataSet();
    }

    private DataSet addMonthlyLine(DataSet keySeq, LocalDate localDate) throws SQLException {
        InsertQuery insertQuery = database.getInsertQuery().insert(TABLE_LINE);
        insertQuery.set(TABLE, keySeq.getPId()).set("pattern", monthlyPattern).set("prefix", keySeq.get("prefix"))
                .set("year", localDate.getYear()).set("month", localDate.getMonthValue())
                .set("last_index", 0).set("last_key", "").executeUpdate();
        SelectQuery query = database.getSelectQuery().select(TABLE_LINE);
        WhereQuery whereQuery = query.whereQuery().equalTo(TABLE, keySeq.getPId());
        whereQuery.and("year", localDate.getYear()).and("month", localDate.getMonthValue());
        return query.append(whereQuery).limit(1).getDataSet();
    }

    private DataSet addDailyLine(DataSet keySeq, LocalDate localDate) throws SQLException {
        InsertQuery insertQuery = database.getInsertQuery().insert(TABLE_LINE);
        insertQuery.set(TABLE, keySeq.getPId()).set("pattern", dailyPattern).set("prefix", keySeq.get("prefix"))
                .set("year", localDate.getYear()).set("month", localDate.getMonthValue())
                .set("day", localDate.getDayOfMonth()).set("last_index", 0).set("last_key", "").executeUpdate();
        SelectQuery query = database.getSelectQuery().select(TABLE_LINE);
        WhereQuery whereQuery = query.whereQuery().equalTo(TABLE, keySeq.getPId());
        whereQuery.and("year", localDate.getYear()).and("month", localDate.getMonthValue())
                .and("day", localDate.getDayOfMonth());
        return query.append(whereQuery).limit(1).getDataSet();
    }

    private DataSet addLine(DataSet keySeq) throws SQLException {
        InsertQuery insertQuery = database.getInsertQuery().insert(TABLE_LINE).set(TABLE, keySeq.getPId());
        insertQuery.set("pattern", "").set("year", 0).set("last_index", 0).set("last_key", "").executeUpdate();
        SelectQuery query = database.getSelectQuery().select(TABLE_LINE);
        WhereQuery whereQuery = query.whereQuery().equalTo(TABLE, keySeq.getPId());
        return query.append(whereQuery.and("year", 0)).limit(1).getDataSet();
    }
}

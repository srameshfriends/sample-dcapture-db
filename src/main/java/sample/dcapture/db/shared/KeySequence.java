package sample.dcapture.db.shared;

import dcapture.db.core.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class KeySequence {
    private static final String TABLE = "key_sequence", TABLE_LINE = "key_sequence_line";
    private static final String DAILY = "DAILY", MONTHLY = "MONTHLY", YEARLY = "YEARLY", NO_RESET = "NO_RESET";
    private static final String[] TABLE_INSERT = new String[]{"name", "type", "prefix"};
    private static final String[] TABLE_UPDATE = new String[]{"type", "prefix"};
    private String dailyPattern, monthlyPattern, yearlyPattern;
    private final SqlQuery query;

    private KeySequence(SqlQuery query) {
        this.query = query;
        dailyPattern = "yyMMdd";
        monthlyPattern = "yyMM";
        yearlyPattern = "yy";
    }

    public static synchronized KeySequence create(SqlQuery query) {
        return new KeySequence(query);
    }

    public void generate(final String name, DataSet document, String keyColumn, String dateColumn) {
        SelectQuery sequenceQry = query.selectAll(TABLE).equalTo("name", name).limit(1).forUpdate();
        if (sequenceQry == null) {
            throw new NullPointerException("Key sequence not created for " + name);
        }
        LocalDate localDate = document.getLocalDate(dateColumn);
        DataSet sequence = sequenceQry.getDataSet();
        DataSet sequenceLine = getOrInsertSequenceLine(name, sequence, localDate, document);
        String prefix = sequenceLine.getString("prefix", "");
        String pattern = sequenceLine.getString("pattern", "");
        String text = localDate == null ? "" : DateTimeFormatter.ofPattern(pattern).format(localDate);
        final int index = sequenceLine.getInt("last_index") + 1;
        final String key = prefix + text + index;
        document.set(keyColumn, key);
        UpdateQuery updateQuery = query.update(TABLE_LINE).set("last_index", index).set("last_key", key);
        updateQuery.equalTo("id", sequenceLine.getLong("id"));
    }

    public void generate(final String name, List<DataSet> documentList, String keyColumn, String dateColumn) {
        SelectQuery sequenceQry = query.selectAll(TABLE).equalTo("name", name).limit(1).forUpdate();
        if (sequenceQry == null) {
            throw new NullPointerException("Key sequence not created for " + name);
        }
        DataSet sequence = sequenceQry.getDataSet();
        int index;
        String key;
        for (DataSet document : documentList) {
            LocalDate localDate = document.getLocalDate(dateColumn);
            DataSet sequenceLine = getOrInsertSequenceLine(name, sequence, localDate, document);
            String prefix = sequenceLine.getString("prefix", "");
            String pattern = sequenceLine.getString("pattern", "");
            String text = localDate == null ? "" : DateTimeFormatter.ofPattern(pattern).format(localDate);
            index = sequenceLine.getInt("last_index") + 1;
            key = prefix + text + index;
            document.set(keyColumn, key);
            UpdateQuery updateQuery = query.update(TABLE_LINE).set("last_index", index).set("last_key", key);
            updateQuery.equalTo("id", sequenceLine.getLong("id"));
        }
    }

    private DataSet getOrInsertSequenceLine(final String name, DataSet sequence, LocalDate localDate, DataSet forShowError) {
        final String type = sequence.getString("type");
        if (localDate == null && !NO_RESET.equals(type)) {
            throw new NullPointerException("Key sequence (Name: " + name
                    + ",type: " + type + ") date should not be null [" + forShowError + "]");
        }
        SelectQuery lineQry = query.selectAll(TABLE_LINE).equalTo(TABLE, sequence.getLong("id")).limit(1).forUpdate();
        DataSet sequenceLine = lineQry.getDataSet();
        if (sequenceLine == null) {
            sequenceLine = new DataSet();
            sequenceLine.set(TABLE, sequence.get("id"));
            sequenceLine.set("prefix", sequence.get("prefix"));
            int year = 0, month = 0, day = 0;
            InsertQuery insertQuery = query.insert(TABLE_LINE).set(TABLE, sequence.get("id"))
                    .set("prefix", sequence.get("prefix"));
            if (NO_RESET.equals(type)) {
                insertQuery.set("pattern", "").set("year", 0).set("month", 0).set("day", 0)
                        .set("last_index", 0).set("last_key", "");
            } else if (YEARLY.equals(type)) {
                insertQuery.set("pattern", yearlyPattern).set("year", localDate.getYear()).set("month", 0).set("day", 0)
                        .set("last_index", 0).set("last_key", "");
                year = localDate.getYear();
            } else if (MONTHLY.equals(type)) {
                insertQuery.set("pattern", monthlyPattern).set("year", localDate.getYear())
                        .set("month", localDate.getMonthValue()).set("day", 0).set("last_index", 0).set("last_key", "");
                year = localDate.getYear();
                month = localDate.getMonthValue();
            } else if (localDate != null) {
                insertQuery.set("pattern", dailyPattern).set("year", localDate.getYear())
                        .set("month", localDate.getMonthValue()).set("day", localDate.getDayOfMonth())
                        .set("last_index", 0).set("last_key", "");
                year = localDate.getYear();
                month = localDate.getMonthValue();
                day = localDate.getDayOfMonth();
            } else {
                throw new NullPointerException("Key sequence (Name: " + name
                        + ", Unknown Type  (" + type + ") : " + forShowError + "");
            }
            sequenceLine.set("year", year).set("month", month).set("day", day);
        }
        return sequenceLine;
    }

    public void save(DataSet source) {
        String name = source.getString("name", null), type = source.getString("type", null);
        String prefix = source.getString("prefix");
        if (name == null || type == null) {
            throw new NullPointerException("Key Sequence parameter should not be null (name, type, prefix) "
                    + name + ", " + type + ", " + prefix);
        }
        name = name.trim();
        type = type.trim();
        if (!(MONTHLY.equals(type) || DAILY.equals(type) || YEARLY.equals(type) || NO_RESET.equals(type))) {
            throw new NullPointerException("Key Sequence type not matched [MONTHLY, DAILY, YEARLY, NO_RESET] : " + type);
        }
        prefix = prefix == null ? "" : prefix.trim();
        SelectQuery selectQuery = query.selectAll(TABLE).equalTo("name", name).limit(1);
        DataSet sequence = selectQuery.getDataSet();
        if (sequence != null) {
            sequence.set("type", type);
            sequence.set("prefix", prefix);
            query.update(sequence, TABLE, TABLE_UPDATE);
        } else {
            source.set("name", name);
            source.set("type", type);
            source.set("prefix", prefix);
            query.insert(source, TABLE, TABLE_INSERT);
        }
    }

    public void save(List<DataSet> sourceList) {
        ArrayList<DataSet> insertList = new ArrayList<>();
        ArrayList<DataSet> updateList = new ArrayList<>();
        for (DataSet source : sourceList) {
            String name = source.getString("name", null), type = source.getString("type", null);
            String prefix = source.getString("prefix");
            if (name == null || type == null) {
                throw new NullPointerException("Key Sequence parameter should not be null (name, type, prefix) "
                        + name + ", " + type + ", " + prefix);
            }
            name = name.trim();
            type = type.trim();
            if (!(MONTHLY.equals(type) || DAILY.equals(type) || YEARLY.equals(type) || NO_RESET.equals(type))) {
                throw new NullPointerException("Key Sequence type not matched [MONTHLY, DAILY, YEARLY, NO_RESET] : " + type);
            }
            prefix = prefix == null ? "" : prefix.trim();
            SelectQuery selectQuery = query.selectAll(TABLE).equalTo("name", name).limit(1);
            DataSet sequence = selectQuery.getDataSet();
            if (sequence != null) {
                if (!sequence.equals("type", type) || !sequence.equals("prefix", prefix)) {
                    sequence.unlock();
                    sequence.set("type", type);
                    sequence.set("prefix", prefix);
                    updateList.add(sequence);
                }
            } else {
                source.set("name", name);
                source.set("type", type);
                source.set("prefix", prefix);
                insertList.add(source);
            }
        }
        if (!updateList.isEmpty()) {
            query.update(updateList, TABLE, TABLE_INSERT);
        }
        if (!insertList.isEmpty()) {
            query.insert(insertList, TABLE, TABLE_INSERT);
        }
    }
}

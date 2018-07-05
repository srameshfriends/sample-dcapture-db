package sample.dcapture.db.service;

import dcapture.io.JsonMapper;
import dcapture.io.Paging;
import dcapture.db.core.*;
import dcapture.db.postgres.PgQuery;

import javax.json.*;
import java.math.BigDecimal;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SqlMapper extends JsonMapper {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss");

    public static DataModel toDataModel(SqlDatabase database, SqlTable table, JsonObject obj) {
        DataModel model = new DataModel();
        if (table.getPrimary() != null) {
            model.setId(getLong(obj, table.getPrimary().getName()));
        }
        if (table.getVersion() != null) {
            model.setRev(getInt(obj, table.getVersion().getName()));
        }
        for (SqlColumn col : table.getColumns()) {
            JsonValue json = obj.get(col.getName());
            if (json == null) {
                continue;
            }
            if (col.getReference() != null) {
                SqlTable refTable = database.getTable(col.getReference());
                if (refTable == null) {
                    throw new NullPointerException(col.toString() + " : Sql Column reference is table is null");
                } else if (json instanceof JsonObject) {
                    model.setValue(col.getName(), toDataSet(database, refTable, (JsonObject) json));
                } else if (json instanceof JsonNumber) {
                    model.setValue(col.getName(), new DataSet(((JsonNumber) json).intValue()));
                } else if (json instanceof JsonString) {
                    long referenceId = toLong(((JsonString) json).getString());
                    if (0 < referenceId) {
                        model.setValue(col.getName(), new DataSet(referenceId));
                    }
                } else {
                    throw new NullPointerException(col.toString() + " : is not a valid format > " + json.toString());
                }
            } else if (json instanceof JsonString) {
                String text = ((JsonString) json).getString();
                if (String.class.equals(col.getModel())) {
                    model.setValue(col.getName(), text);
                } else if (LocalDate.class.equals(col.getModel())) {
                    model.setValue(col.getName(), LocalDate.parse(text));
                } else if (LocalDateTime.class.equals(col.getModel())) {
                    model.setValue(col.getName(), LocalDateTime.parse(text));
                } else if (LocalTime.class.equals(col.getModel())) {
                    model.setValue(col.getName(), LocalTime.parse(text));
                } else if (Integer.class.equals(col.getModel())) {
                    model.setValue(col.getName(), toInt(text));
                } else if (Double.class.equals(col.getModel())) {
                    model.setValue(col.getName(), toDouble(text));
                } else if (Long.class.equals(col.getModel())) {
                    model.setValue(col.getName(), toLong(text));
                } else if (BigDecimal.class.equals(col.getModel())) {
                    model.setValue(col.getName(), toBigDecimal(text));
                } else if (Boolean.class.equals(col.getModel())) {
                    if ("true".equals(text.toLowerCase())) {
                        model.setValue(col.getName(), true);
                    } else {
                        model.setValue(col.getName(), false);
                    }
                } else {
                    throw new IllegalArgumentException(col.getModel() + " \t " + col.toString() + " convert to string format error : " + json.toString());
                }
            } else if (json instanceof JsonNumber) {
                JsonNumber number = ((JsonNumber) json);
                if (Integer.class.equals(col.getModel())) {
                    model.setValue(col.getName(), number.intValue());
                } else if (Double.class.equals(col.getModel())) {
                    model.setValue(col.getName(), number.doubleValue());
                } else if (Long.class.equals(col.getModel())) {
                    model.setValue(col.getName(), number.longValue());
                } else if (BigDecimal.class.equals(col.getModel())) {
                    model.setValue(col.getName(), number.bigDecimalValue());
                } else {
                    throw new IllegalArgumentException(col.getModel() + " \t " + col.toString() + " convert to number format error : " + json.toString());
                }
            } else if (JsonValue.ValueType.TRUE.equals(json.getValueType())) {
                model.setValue(col.getName(), true);
            } else if (JsonValue.ValueType.FALSE.equals(json.getValueType())) {
                model.setValue(col.getName(), false);
            } else if (JsonValue.ValueType.NULL.equals(json.getValueType())) {
                model.setValue(col.getName(), null);
            } else {
                throw new IllegalArgumentException(col.getModel() + " \t " + col.toString() + " convert to dataSet data type not supported : " + json.toString());
            }
        }
        return model;
    }

    public static DataSet toDataSet(SqlDatabase database, SqlTable table, JsonObject obj) {
        long id = 0;
        int rev = 0;
        if (table.getPrimary() != null) {
            id = getLong(obj, table.getPrimary().getName());
        }
        if (table.getVersion() != null) {
            rev = getInt(obj, table.getVersion().getName());
        }
        Map<String, Object> values = new HashMap<>();
        for (SqlColumn col : table.getColumns()) {
            JsonValue json = obj.get(col.getName());
            if (json == null) {
                continue;
            }
            if (col.getReference() != null) {
                SqlTable refTable = database.getTable(col.getReference());
                if (refTable == null) {
                    throw new NullPointerException(col.toString() + " : Sql Column reference is table is null");
                } else if (json instanceof JsonObject) {
                    values.put(col.getName(), toDataSet(database, refTable, (JsonObject) json));
                } else if (json instanceof JsonNumber) {
                    values.put(col.getName(), new DataSet(((JsonNumber) json).intValue()));
                } else if (json instanceof JsonString) {
                    id = toLong(((JsonString) json).getString());
                    if (0 < id) {
                        values.put(col.getName(), new DataSet(id));
                    }
                } else {
                    throw new NullPointerException(col.toString() + " : is not a valid format > " + json.toString());
                }
            } else if (json instanceof JsonString) {
                String text = ((JsonString) json).getString();
                if (String.class.equals(col.getModel())) {
                    values.put(col.getName(), text);
                } else if (LocalDate.class.equals(col.getModel())) {
                    values.put(col.getName(), LocalDate.parse(text));
                } else if (LocalDateTime.class.equals(col.getModel())) {
                    values.put(col.getName(), LocalDateTime.parse(text));
                } else if (LocalTime.class.equals(col.getModel())) {
                    values.put(col.getName(), LocalTime.parse(text));
                } else if (Integer.class.equals(col.getModel())) {
                    values.put(col.getName(), toInt(text));
                } else if (Double.class.equals(col.getModel())) {
                    values.put(col.getName(), toDouble(text));
                } else if (Long.class.equals(col.getModel())) {
                    values.put(col.getName(), toLong(text));
                } else if (BigDecimal.class.equals(col.getModel())) {
                    values.put(col.getName(), toBigDecimal(text));
                } else if (Boolean.class.equals(col.getModel())) {
                    if ("true".equals(text.toLowerCase())) {
                        values.put(col.getName(), true);
                    } else {
                        values.put(col.getName(), false);
                    }
                } else {
                    throw new IllegalArgumentException(col.getModel() + " \t " + col.toString() + " convert to string format error : " + json.toString());
                }
            } else if (json instanceof JsonNumber) {
                JsonNumber number = ((JsonNumber) json);
                if (Integer.class.equals(col.getModel())) {
                    values.put(col.getName(), number.intValue());
                } else if (Double.class.equals(col.getModel())) {
                    values.put(col.getName(), number.doubleValue());
                } else if (Long.class.equals(col.getModel())) {
                    values.put(col.getName(), number.longValue());
                } else if (BigDecimal.class.equals(col.getModel())) {
                    values.put(col.getName(), number.bigDecimalValue());
                } else {
                    throw new IllegalArgumentException(col.getModel() + " \t " + col.toString() + " convert to number format error : " + json.toString());
                }
            } else if (JsonValue.ValueType.TRUE.equals(json.getValueType())) {
                values.put(col.getName(), true);
            } else if (JsonValue.ValueType.FALSE.equals(json.getValueType())) {
                values.put(col.getName(), false);
            } else if (JsonValue.ValueType.NULL.equals(json.getValueType())) {
                values.put(col.getName(), null);
            } else {
                throw new IllegalArgumentException(col.getModel() + " \t " + col.toString() + " convert to dataSet data type not supported : " + json.toString());
            }
        }
        return new DataSet(id, rev, values);
    }

    public static List<DataSet> toDataSets(SqlDatabase database, SqlTable sqlTable, JsonArray array) {
        List<DataSet> dataSetList = new ArrayList<>();
        List<JsonObject> objectList = getList(array);
        for (JsonObject object : objectList) {
            dataSetList.add(toDataSet(database, sqlTable, object));
        }
        return dataSetList;
    }

    public static List<DataModel> toDataModels(SqlDatabase database, SqlTable sqlTable, JsonArray array) {
        List<DataModel> models = new ArrayList<>();
        List<JsonObject> objectList = getList(array);
        for (JsonObject object : objectList) {
            models.add(toDataModel(database, sqlTable, object));
        }
        return models;
    }

    public static SqlQuery[] querySearchCount(SqlDatabase database, SqlTable sqlTable, Paging paging) {
        SqlQuery dataQuery = new PgQuery(), countQuery = new PgQuery();
        dataQuery.add("SELECT ");
        countQuery.add("SELECT COUNT(*) ");
        if (sqlTable.getPrimary() != null) {
            dataQuery.add(sqlTable.getPrimary().getName()).add(",");
        }
        if (sqlTable.getVersion() != null) {
            dataQuery.add(sqlTable.getVersion().getName()).add(",");
        }
        StringBuilder sb = new StringBuilder();
        for (SqlColumn col : sqlTable.getColumns()) {
            sb.append(col.getName()).append(",");
        }
        sb.replace(sb.length() - 1, sb.length(), " ");
        dataQuery.add(sb.toString()).add(" FROM ");
        countQuery.add(" FROM ");
        if (database.getSchema() != null) {
            dataQuery.add(database.getSchema()).add(".");
            countQuery.add(database.getSchema()).add(".");
        }
        dataQuery.add(sqlTable.getName());
        countQuery.add(sqlTable.getName());
        if (paging.getSearchText() != null && !paging.getSearchText().trim().isEmpty()) {
            dataQuery.add(" WHERE ");
            dataQuery.searchText(sqlTable, "search", paging.getSearchText());
            countQuery.add(" WHERE ");
            countQuery.searchText(sqlTable, "search", paging.getSearchText());
        }
        dataQuery.add(" ORDER BY ");
        if (paging.getSortingOrder().isEmpty()) {
            ColumnGroup columnGroup = sqlTable.getColumnGroup("search");
            if (columnGroup.getOrderBy() != null && 0 < columnGroup.getOrderBy().length) {
                StringBuilder qsb = new StringBuilder();
                for (String order : columnGroup.getOrderBy()) {
                    qsb.append(order).append(",");
                }
                qsb.replace(qsb.length() - 1, qsb.length(), " ");
                dataQuery.add(qsb.toString());
            } else {
                dataQuery.add(sqlTable.getColumns().iterator().next().getName());
            }
        } else {
            StringBuilder qsb = new StringBuilder();
            for (String order : paging.getSortingOrder()) {
                qsb.append(order).append(",");
            }
            qsb.replace(qsb.length() - 1, qsb.length(), " ");
            dataQuery.add(qsb.toString());
        }
        if (0 < paging.getLimit()) {
            dataQuery.limit(paging.getLimit(), paging.getStart());
        }
        return new SqlQuery[]{dataQuery, countQuery};
    }

    public static SqlQuery querySelectAll(SqlDatabase database, String sqlTable) {
        return querySelectAll(database, database.getTable(sqlTable));
    }

    public static SqlQuery querySelectAll(SqlDatabase database, SqlTable sqlTable) {
        SqlQuery query = new PgQuery();
        query.add("SELECT ");
        if (sqlTable.getPrimary() != null) {
            query.add(sqlTable.getPrimary().getName()).add(",");
        }
        if (sqlTable.getVersion() != null) {
            query.add(sqlTable.getVersion().getName()).add(",");
        }
        StringBuilder sb = new StringBuilder();
        for (SqlColumn col : sqlTable.getColumns()) {
            sb.append(col.getName()).append(",");
        }
        sb.replace(sb.length() - 1, sb.length(), " ");
        query.add(sb.toString()).add(" FROM ");
        if (database.getSchema() != null) {
            query.add(database.getSchema()).add(".");
        }
        query.add(sqlTable.getName()).add(" ");
        return query;
    }

    public static Paging parsePaging(JsonObject obj) {
        Paging paging = new Paging();
        paging.setStart(getInt(obj, "start"));
        paging.setLimit(getInt(obj, "limit"));
        paging.setSearchText(getString(obj, "searchText"));
        List<String> orderByList = getList(obj, "sortingOrder");
        paging.setSortingOrder(orderByList);
        return paging;
    }

    public static DataSet merge(DataSet target, DataSet source) {
        Map<String, Object> values = new HashMap<>(target.getValues());
        values.putAll(source.getValues());
        return new DataSet(target.getId(), target.getRev(), values);
    }

    public static JsonObject toJsonObject(SqlDatabase database, SqlTable sqlTable, DataSet dataSet) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        if (sqlTable.getPrimary() != null) {
            json.add(sqlTable.getPrimary().getName(), dataSet.getId());
        }
        if (sqlTable.getVersion() != null) {
            json.add(sqlTable.getVersion().getName(), dataSet.getRev());
        }
        for (SqlColumn col : sqlTable.getColumns()) {
            Object value = dataSet.getValue(col.getName());
            if (value != null) {
                if (value instanceof DataSet) {
                    SqlTable refTable = database.getTable(col.getReference());
                    if (refTable == null) {
                        throw new NullPointerException(col.toString() + " : Sql Column reference is table is null");
                    }
                    json.add(col.getName(), toJsonObject(database, refTable, (DataSet) value));
                } else if (value instanceof String) {
                    json.add(col.getName(), (String) value);
                } else if (value instanceof Integer) {
                    json.add(col.getName(), (Integer) value);
                } else if (value instanceof Long) {
                    json.add(col.getName(), (Long) value);
                } else if (value instanceof Double) {
                    json.add(col.getName(), (Double) value);
                } else if (value instanceof BigDecimal) {
                    json.add(col.getName(), (BigDecimal) value);
                } else if (value instanceof Boolean) {
                    json.add(col.getName(), (Boolean) value);
                } else if (value instanceof Date) {
                    json.add(col.getName(), toString((Date) value));
                } else if (value instanceof LocalDate) {
                    json.add(col.getName(), toString((LocalDate) value));
                } else if (value instanceof LocalDateTime) {
                    json.add(col.getName(), toString((LocalDateTime) value));
                } else if (value instanceof LocalTime) {
                    json.add(col.getName(), toString((LocalTime) value));
                } else {
                    throw new IllegalArgumentException(
                            col.toString() + " :  Entity to json format not yet implemented! " + value);
                }
            } else if (col.getReference() != null) {
                json.add(col.getName(), Json.createObjectBuilder().build());
            } else {
                switch (col.getType()) {
                    case Types.VARCHAR:
                        json.add(col.getName(), "");
                        break;
                    case Types.INTEGER:
                    case Types.BIGINT:
                        json.add(col.getName(), 0);
                        break;
                    case Types.DOUBLE:
                    case Types.DECIMAL:
                        json.add(col.getName(), 0D);
                        break;
                    case Types.BOOLEAN:
                        json.add(col.getName(), false);
                        break;
                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        json.add(col.getName(), "");
                        break;
                    default:
                        throw new IllegalArgumentException(
                                col.toString() + " :  Entity to json format not yet implemented! " + col.getType());
                }
            }
        }
        return json.build();
    }

    static JsonArray toJsonArray(SqlDatabase database, SqlTable sqlTable, List<DataSet> dataSetList) {
        JsonArrayBuilder array = Json.createArrayBuilder();
        if (dataSetList != null) {
            for (DataSet dataSet : dataSetList) {
                array.add(toJsonObject(database, sqlTable, dataSet));
            }
        }
        return array.build();
    }

    static String isValidRequired(SqlTable sqlTable, String columnGroup, DataSet dataSet) {
        ColumnGroup group = sqlTable.getColumnGroup(columnGroup);
        for (String col : group.getColumns()) {
            Object value = dataSet.getValue(col);
            if (value == null) {
                return sqlTable.getName() + "." + col + ".invalid";
            } else if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    return sqlTable.getName() + "." + col + ".invalid";
                }
            }
        }
        return null;
    }

    public static void setPaging(JsonObjectBuilder builder, Paging paging, int totalRecords, int length) {
        builder.add("start", paging.getStart());
        builder.add("limit", paging.getLimit());
        builder.add("totalRecords", totalRecords);
        builder.add("length", length);
    }

    private static String toString(Date date) {
        return dateFormat.format(date.getTime());
    }

    private static String toString(LocalDate date) {
        return dateFormat.format(date);
    }

    private static String toString(LocalDateTime date) {
        return dateTimeFormat.format(date);
    }

    private static String toString(LocalTime localTime) {
        return timeFormatter.format(localTime);
    }

    public static int toInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            // ignore exception
        }
        return 0;
    }

    public static long toLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            // ignore exception
        }
        return 0L;
    }

    public static double toDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            // ignore exception
        }
        return 0.0D;
    }

    public static BigDecimal toBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            // ignore exception
        }
        return BigDecimal.ZERO;
    }
}

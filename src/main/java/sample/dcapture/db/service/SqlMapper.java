package sample.dcapture.db.service;

import dcapture.db.core.*;
import dcapture.io.JsonMapper;

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

    public static List<DataSet> toDataSets(SqlDatabase database, String tableName, JsonArray array) {
        SqlTable sqlTable = database.getTable(tableName);
        List<DataSet> dataSetList = new ArrayList<>();
        List<JsonObject> objectList = getList(array);
        for (JsonObject object : objectList) {
            dataSetList.add(toDataSet(database, sqlTable, object));
        }
        return dataSetList;
    }

    public static List<DataModel> toDataModels(SqlDatabase database, String tableName, JsonArray array) {
        List<DataModel> models = new ArrayList<>();
        List<JsonObject> objectList = getList(array);
        SqlTable sqlTable = database.getTable(tableName);
        for (JsonObject object : objectList) {
            models.add(toDataModel(database, sqlTable, object));
        }
        return models;
    }

    public static DataSet merge(DataSet target, DataSet source) {
        Map<String, Object> values = new HashMap<>(target.getValues());
        values.putAll(source.getValues());
        return new DataSet(target.getId(), target.getRev(), values);
    }

    public static JsonObject toJsonObject(SqlDatabase database, String table, DataSet dataSet) {
        return toJsonObject(database, database.getTable(table), dataSet);
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

    static JsonArray toJsonArray(SqlDatabase database, String tableName, List<DataSet> dataSetList) {
        SqlTable sqlTable = database.getTable(tableName);
        JsonArrayBuilder array = Json.createArrayBuilder();
        if (dataSetList != null) {
            for (DataSet dataSet : dataSetList) {
                array.add(toJsonObject(database, sqlTable, dataSet));
            }
        }
        return array.build();
    }

    public static String isValid(SqlDatabase database, String tableName, String columnGroup, DataSet dataSet) {
        SqlTable sqlTable = database.getTable(tableName);
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

package sample.dcapture.sql.dev;

import dcapture.sql.core.FetchGroup;
import dcapture.sql.core.SqlColumn;
import dcapture.sql.core.SqlTable;
import dcapture.sql.core.SqlTypeMap;

import javax.json.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

class SqlTableBuilder {
    private SqlTypeMap sqlTypeMap;

    private Class<?> getType(String type, String tableName) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        try {
            return Class.forName(type.trim());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException(tableName + " : table type class not found (" + type + ")");
    }

    private JsonArray getJsonArray(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonArray) {
            return ((JsonArray) value);
        }
        return null;
    }

    private boolean getBoolean(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        return value != null && JsonValue.ValueType.TRUE.equals(value.getValueType());
    }

    private int getInt(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonNumber) {
            return ((JsonNumber) value).intValue();
        }
        return 0;
    }

    private String getString(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        }
        return null;
    }

    private FetchGroup getFetchGroup(SqlTable sqlTable, JsonObject obj) {
        List<String> columnList = new ArrayList<>();
        for (SqlColumn column : sqlTable.getColumns()) {
            columnList.add(column.getName());
        }
        String name = getString(obj, "name");
        JsonArray columnArray = getJsonArray(obj, "columns");
        JsonArray orderByArray = getJsonArray(obj, "orderBy");
        if (columnArray == null || columnArray.isEmpty()) {
            return null;
        }
        List<String> fetchColList = new ArrayList<>();
        for (JsonValue value : columnArray) {
            if (value instanceof JsonString) {
                String col = ((JsonString) value).getString();
                col = col.trim().toLowerCase();
                if (columnList.contains(col)) {
                    fetchColList.add(col);
                }
            }
        }
        if (fetchColList.isEmpty()) {
            return null;
        }
        List<String> orderByColList = new ArrayList<>();
        if (orderByArray != null && !orderByArray.isEmpty()) {
            for (JsonValue orderBy : orderByArray) {
                if (orderBy instanceof JsonString) {
                    String col = ((JsonString) orderBy).getString();
                    col = col.trim().toLowerCase();
                    if (columnList.contains(col)) {
                        orderByColList.add(col);
                    }
                }
            }
        }
        if (orderByColList.isEmpty()) {
            orderByColList.add(fetchColList.get(0));
        }
        FetchGroup fetchGroup = new FetchGroup(sqlTable.getName(), name);
        fetchGroup.setColumns(fetchColList);
        String[] orderByStrings = new String[orderByColList.size()];
        fetchGroup.setOrderBy(orderByColList.toArray(orderByStrings));
        return fetchGroup;
    }

    private SqlColumn getSqlColumn(SqlTable sqlTable, JsonObject obj) {
        String name = getString(obj, "name");
        String type = getString(obj, "type");
        String fieldName = getString(obj, "field");
        String reference = getString(obj, "reference");

        if (name == null || name.trim().isEmpty()) {
            throw new NullPointerException("Table : " + sqlTable.getName() + " >> column name should not be null or empty");
        }
        if (fieldName == null || fieldName.trim().isEmpty()) {
            fieldName = name;
        } else {
            fieldName = fieldName.trim();
        }
        if (type == null || type.trim().isEmpty()) {
            throw new NullPointerException("Table : " + sqlTable.getName() + ", Column : " + name + " >> type should not be null or empty");
        }
        final int sqlType = sqlTypeMap.getSqlType(type);
        final Class<?> model = sqlTypeMap.getType(type);
        if (0 == sqlType) {
            throw new NullPointerException("Table : " + sqlTable.getName()
                    + ", Table Type : " + sqlTable.getType()
                    + ", Column : " + name + ", Field : " + fieldName + ", Model : " + model
                    + " >> sql data type not supported");
        }
        if (model == null) {
            throw new NullPointerException("Table : " + sqlTable.getName()
                    + ", Table Type : " + sqlTable.getType()
                    + ", Column : " + name + ", Field : " + fieldName
                    + " >> sql data model should not be null");
        }
        SqlColumn sqlColumn = new SqlColumn(sqlTable.getName(), name, sqlType, model);
        sqlColumn.setReference(reference);
        sqlColumn.setField(fieldName);
        sqlColumn.setLength(getInt(obj, "length"));
        sqlColumn.setAutoIncrement(getBoolean(obj, "autoIncrement"));
        sqlColumn.setNotNull(getBoolean(obj, "notNull"));
        return sqlColumn;
    }

    private SqlTable getSqlTable(JsonObject obj) {
        String tableName = getString(obj, "name");
        String type = getString(obj, "type");
        String modelText = getString(obj, "model");
        boolean isPrimary = getBoolean(obj, "primary");
        boolean isVersion = getBoolean(obj, "version");
        JsonArray columns = getJsonArray(obj, "columns");
        JsonArray fgArray = getJsonArray(obj, "fetchGroups");
        Class<?> model = getType(modelText, tableName);
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new NullPointerException(tableName + " : \t table name should not be null or empty");
        }
        if (columns == null || 2 > columns.size()) {
            throw new NullPointerException(tableName + " : \t table columns should not be null or minimum 2 columns required");
        }
        SqlTable sqlTable = new SqlTable(tableName, type, model);
        if (isPrimary) {
            sqlTable.setPrimary();
        }
        if (isVersion) {
            sqlTable.setVersion();
        }
        List<SqlColumn> columnList = new ArrayList<>();
        for (JsonValue jv : columns) {
            if (jv instanceof JsonObject) {
                columnList.add(getSqlColumn(sqlTable, (JsonObject) jv));
            }
        }
        sqlTable.setColumns(columnList);
        boolean isEmptyGroup = false;
        List<FetchGroup> fetchGroups = new ArrayList<>();
        if (fgArray != null) {
            for (JsonValue entry : fgArray) {
                if (entry instanceof JsonObject) {
                    FetchGroup fetchGroup = getFetchGroup(sqlTable, (JsonObject) entry);
                    if (fetchGroup != null) {
                        fetchGroups.add(fetchGroup);
                        isEmptyGroup = fetchGroup.getName().equals("");
                    }
                }
            }
        }
        if (!isEmptyGroup) {
            List<String> colList = new ArrayList<>();
            for (SqlColumn column : columnList) {
                colList.add(column.getName());
            }
            String[] orderBy;
            if (sqlTable.getPrimary() != null) {
                orderBy = new String[]{sqlTable.getPrimary().getName()};
            } else {
                orderBy = new String[]{colList.get(0)};
            }
            FetchGroup emptyGroup = new FetchGroup(sqlTable.getName(), "");
            emptyGroup.setColumns(colList);
            emptyGroup.setOrderBy(orderBy);
            fetchGroups.add(emptyGroup);
        }
        sqlTable.setFetchGroups(fetchGroups);
        return sqlTable;
    }

    List<SqlTable> getTableList(SqlTypeMap sqlTypeMap, File jsonFile) {
        this.sqlTypeMap = sqlTypeMap;
        List<SqlTable> tableList = new ArrayList<>();
        try {
            JsonReader reader = Json.createReader(new FileInputStream(jsonFile));
            JsonArray array = reader.readArray();
            for (JsonValue value : array) {
                if (value instanceof JsonObject) {
                    tableList.add(getSqlTable((JsonObject) value));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return tableList;
    }
}

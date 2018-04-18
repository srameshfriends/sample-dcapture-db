package sample.dcapture.sql;

import dcapture.sql.core.FetchGroup;
import dcapture.sql.core.SqlColumn;
import dcapture.sql.core.SqlTable;
import dcapture.sql.core.SqlTypeMap;

import javax.json.*;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.SQLType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SampleDatabase {
    private SqlTypeMap sqlTypeMap;

    private File getClassPathFile(String fileName) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL url = classloader.getResource(fileName);
        try {
            String uriPath = url == null ? null : Paths.get(url.toURI()).toString();
            if (uriPath != null) {
                return new File(uriPath);
            }
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        throw new NullPointerException("File not found at class path " + fileName);
    }

    private Map<String, Field> getFieldMap(Class<?> model) {
        Map<String, Field> map = new HashMap<>();
        Field[] fieldArray = model.getDeclaredFields();
        for (Field field : fieldArray) {
            map.put(field.getName(), field);
        }
        return map;
    }

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

    private FetchGroup getFetchGroup(List<String> tableColumnList, JsonObject obj) {
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
                if (tableColumnList.contains(col)) {
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
                    if (tableColumnList.contains(col)) {
                        orderByColList.add(col);
                    }
                }
            }
        }
        if (orderByColList.isEmpty()) {
            orderByColList.add(fetchColList.get(0));
        }
        FetchGroup fetchGroup = new FetchGroup();
        fetchGroup.setName(name);
        fetchGroup.setColumns(fetchColList);
        String[] orderByStrings = new String[orderByColList.size()];
        fetchGroup.setOrderBy(orderByColList.toArray(orderByStrings));
        return fetchGroup;
    }

    private SqlColumn getSqlColumn(SqlTable sqlTable, Map<String, Field> fieldMap, JsonObject obj) {
        String name = getString(obj, "name");
        String type = getString(obj, "type");
        String fieldName = getString(obj, "field");
        SQLType sqlType;
        Class<?> fieldClass = null;
        if (name == null || name.trim().isEmpty()) {
            throw new NullPointerException("Table : " + sqlTable.getName() + " >> column name should not be null or empty");
        }
        if (sqlTable.getType() == null) {
            if (type == null || type.trim().isEmpty()) {
                throw new NullPointerException("Table : " + sqlTable.getName() + ", Column : " + name + " >> type should not be null or empty");
            }
            sqlType = sqlTypeMap.getSqlType(type);
        } else {
            if (fieldName == null || fieldName.trim().isEmpty()) {
                fieldName = name;
            }
            fieldName = fieldName.trim();
            Field field = fieldMap.get(fieldName);
            if (field == null) {
                throw new NullPointerException("Table : " + sqlTable.getName()
                        + ", Table Type : " + sqlTable.getType()
                        + ", Column : " + name + ", Field : " + fieldName
                        + " >> field not found for ('" + fieldName + "')");
            }
            fieldName = fieldName.trim();
            fieldClass = field.getType();
            sqlType = sqlTypeMap.getSqlType(fieldClass);
        }
        if (sqlType == null) {

            throw new NullPointerException("Table : " + sqlTable.getName()
                    + ", Table Type : " + sqlTable.getType()
                    + ", Column : " + name + ", Field : " + fieldName + ", Field Class : " + fieldClass
                    + " >> sql data type not supported");
        }
        SqlColumn sqlColumn = new SqlColumn();
        sqlColumn.setName(name);
        sqlColumn.setSqlType(sqlType);
        sqlColumn.setField(fieldName);
        sqlColumn.setType(fieldClass);
        sqlColumn.setLength(getInt(obj, "length"));
        sqlColumn.setAutoIncrement(getBoolean(obj, "autoIncrement"));
        sqlColumn.setNotNull(getBoolean(obj, "notNull"));
        return sqlColumn;
    }

    private SqlTable getSqlTable(JsonObject obj) {
        String name = getString(obj, "name");
        String typeText = getString(obj, "type");
        String primary = getString(obj, "primary");
        String version = getString(obj, "version");
        JsonArray columns = getJsonArray(obj, "columns");
        JsonArray fgArray = getJsonArray(obj, "fetchGroups");
        Class<?> tableType = getType(typeText, name);
        if (name == null || name.trim().isEmpty()) {
            throw new NullPointerException("sql table name should not be null or empty");
        }
        if (columns == null || 2 > columns.size()) {
            throw new NullPointerException("sql table columns should not be null or minimum 2 columns required");
        }
        SqlTable sqlTable = new SqlTable();
        sqlTable.setName(name);
        sqlTable.setType(tableType);
        Map<String, Field> fieldMap = sqlTable.getType() == null ? null : getFieldMap(sqlTable.getType());
        List<SqlColumn> columnList = new ArrayList<>();
        for (JsonValue jv : columns) {
            if (jv instanceof JsonObject) {
                columnList.add(getSqlColumn(sqlTable, fieldMap, (JsonObject) jv));
            }
        }
        List<String> columnNameList = new ArrayList<>();
        for (SqlColumn column : columnList) {
            columnNameList.add(column.getName());
        }
        List<FetchGroup> fetchGroups = new ArrayList<>();
        if (fgArray != null) {
            for (JsonValue entry : fgArray) {
                if (entry instanceof JsonObject) {
                    FetchGroup fetchGroup = getFetchGroup(columnNameList, (JsonObject) entry);
                    if (fetchGroup != null) {
                        fetchGroups.add(fetchGroup);
                    }
                }
            }
        }
        sqlTable.setColumns(columnList);
        sqlTable.setFetchGroups(fetchGroups);
        sqlTable.setVersion(version);
        sqlTable.setPrimary(primary);
        return sqlTable;
    }

    public List<SqlTable> loadTableList(SqlTypeMap sqlTypeMap, String fileName) {
        this.sqlTypeMap = sqlTypeMap;
        List<SqlTable> tableList = new ArrayList<>();
        try {
            File databaseFile = getClassPathFile(fileName);
            JsonReader reader = Json.createReader(new FileInputStream(databaseFile));
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


package sample.dcapture.sql.service;

import dcapture.sql.core.SqlMetaData;
import dcapture.sql.core.SqlResult;
import sample.dcapture.sql.model.User;

import javax.json.*;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataParser {

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public String getString(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        }
        return null;
    }

    public String getString(JsonObject obj, String key, String safeDefault) {
        String value = getString(obj, key);
        return value == null ? safeDefault : value;
    }

    public JsonObject getJsonObject(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }
        return null;
    }

    public JsonArray getJsonArray(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonArray) {
            return (JsonArray) value;
        }
        return null;
    }

    public <T> List<T> getList(JsonObject obj, String name) {
        JsonValue value = obj.get(name);
        if (!(value instanceof JsonArray)) {
            return new ArrayList<>();
        }
        return getList((JsonArray) value);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(JsonArray array) {
        List<T> resultList = new ArrayList<>();
        for (JsonValue value : array) {
            Object result;
            if (value instanceof JsonString) {
                result = ((JsonString) value).getString();
                resultList.add((T) result);
            } else if (value instanceof JsonNumber) {
                result = ((JsonNumber) value).doubleValue();
                resultList.add((T) result);
            } else if (value instanceof JsonObject || value instanceof JsonArray) {
                resultList.add((T) value);
            } else {
                throw new IllegalArgumentException("Type conversion not yet implemented");
            }
        }
        return resultList;
    }

    private long getLong(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonNumber) {
            return ((JsonNumber) value).longValue();
        }
        return 0L;
    }

    public int getInt(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonNumber) {
            return ((JsonNumber) value).intValue();
        }
        return 0;
    }

    private int toInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            // ignore
        }
        return 0;
    }

    public String getErrorMessage(User user) {
        if (user.getEmail() == null) {
            return "Email should not be null";
        } else if (user.getPassword() == null) {
            return "Password should not be null";
        } else if (user.getConfirmPassword() == null) {
            return "Confirm Password should not be null";
        } else if (user.getUserName() == null) {
            return "User Name should not be null";
        } else if (user.getFullName() == null) {
            return "Full Name should not be null";
        }
        return null;
    }

    public User toUser(Map<String, String[]> map) {
        User user = new User();
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            if (entry.getValue() == null || 1 > entry.getValue().length) {
                continue;
            }
            String name = entry.getKey();
            if ("email".equals(name)) {
                user.setEmail(entry.getValue()[0]);
            } else if ("password".equals(name)) {
                user.setPassword(entry.getValue()[0]);
            } else if ("userName".equals(name)) {
                user.setUserName(entry.getValue()[0]);
            } else if ("id".equals(name)) {
                user.setId(toInt(entry.getValue()[0]));
            } else if ("fullName".equals(name)) {
                user.setFullName(entry.getValue()[0]);
            } else if ("confirmPassword".equals(name)) {
                user.setConfirmPassword(entry.getValue()[0]);
            }
        }
        return user;
    }

    public User toUser(JsonObject obj) {
        if (obj == null) {
            return null;
        }
        User user = new User();
        user.setId(getInt(obj, "id"));
        user.setEmail(getString(obj, "email"));
        user.setUserName(getString(obj, "userName"));
        user.setFullName(getString(obj, "fullName"));
        user.setPassword(getString(obj, "password"));
        user.setConfirmPassword(getString(obj, "confirmPassword"));
        return user;
    }

    public JsonObject getJsonObject(User user) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (user != null) {
            builder.add("id", user.getId());
            builder.add("email", nullSafe(user.getEmail()));
            builder.add("id", nullSafe(user.getEmail()));
            builder.add("userName", nullSafe(user.getUserName()));
            builder.add("fullName", nullSafe(user.getFullName()));
            builder.add("password", nullSafe(user.getPassword()));
            builder.add("confirmPassword", nullSafe(user.getConfirmPassword()));
        }
        return builder.build();
    }

    public JsonArray getJsonArrayUser(SqlResult result) {
        JsonArrayBuilder array = Json.createArrayBuilder();
        for (Object[] dataArray : result.getDataList()) {
            int index = 1;
            JsonObjectBuilder obj = Json.createObjectBuilder();
            for (Object data : dataArray) {
                SqlMetaData metaData = result.getMetaData(index);
                if ("id".equals(metaData.getColumn())) {
                    obj.add("id", data == null ? 0 : (Integer) data);
                } else if ("email".equals(metaData.getColumn())) {
                    obj.add("email", data == null ? "" : (String) data);
                } else if ("user_name".equals(metaData.getColumn())) {
                    obj.add("userName", data == null ? "" : (String) data);
                } else if ("full_name".equals(metaData.getColumn())) {
                    obj.add("fullName", data == null ? "" : (String) data);
                }
                index += 1;
            }
            array.add(obj);
        }
        return array.build();
    }

    public JsonObject getJsonObject(HttpServletRequest request) {
        try {
            JsonReader reader = Json.createReader(request.getReader());
            JsonStructure structure = reader.read();
            if (structure instanceof JsonObject) {
                return (JsonObject) structure;
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }
}

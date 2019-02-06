package sample.dcapture.db.service;

import dcapture.db.core.DataSet;
import dcapture.db.core.SelectBuilder;
import dcapture.db.core.SqlDatabase;
import dcapture.db.util.DataSetRequest;
import dcapture.db.util.DataSetResult;
import dcapture.db.util.Paging;
import dcapture.io.HttpMethod;
import dcapture.io.HttpPath;
import dcapture.io.Localization;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@HttpPath(value = "/user", secured = false)
public class AppsUserService {
    private static final int PAGE_LIMIT = 20;
    private static final String EMAIL_REGEX = "^[\\w-+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-z]{2,})$";
    private static final String APPS_USER_TBL = "apps_user";
    private static final String[] UPDATE = new String[]{"name", "status"};
    private static final String[] CREATE_USER = new String[]{"email", "name", "password", "status"};
    private static final String[] SEARCH = new String[]{"email", "name", "status"};
    private static final String[] CODE_NAME = new String[]{"code", "name"};
    private SqlDatabase database;
    private Localization locale;

    @Inject
    public AppsUserService(SqlDatabase database, Localization locale) {
        this.database = database;
        this.locale = locale;
    }

    @HttpPath("/search")
    public DataSetResult search(DataSetRequest req) {
        return database.transact(query -> {
            Paging paging = req.getPaging(PAGE_LIMIT);
            SelectBuilder select = query.selectFrom(APPS_USER_TBL, "email", "name", "status");
            select.like(req.getString("searchText"), "email", "name");
            select.orderBy(" email, name").limit(paging.getLimit(), paging.getOffset());
            paging.setDataList(query.getDataSetList(select));
            paging.setTotalRecords(query.getCount(select));
            return DataSetResult.asJsonObject(paging, APPS_USER_TBL, SEARCH);
        });
    }

    @HttpPath(value = "/create1", secured = false)
    public DataSetResult create1(DataSetRequest request) {
        final String name = request.getString("name");
        final String email = request.getString("email");
        if (notValid(name) || !isValidEmail(email)) {
            throw new RuntimeException(locale.get("email.invalid"));
        }
        return database.transact(query -> {
            DataSet appsUser = query.getDataSet(query.selectAll(APPS_USER_TBL).where("email", email).limit(1));
            if (appsUser != null && !"Pending".equals(appsUser.getString("status", ""))) {
                return DataSetResult.error("emailAlreadyRegistered", email);
                // throw new RuntimeException(locale.getMessage("emailAlreadyRegistered", email));
            } else if (appsUser == null) {
                appsUser = new DataSet().set("email", email).set("name", name)
                        .set("password", UUID.randomUUID().toString()).set("status", "Pending");
                query.insert(appsUser, APPS_USER_TBL, CREATE_USER);
            }
            appsUser.set("name", name);
            appsUser.set("code", appsUser.get("password"));
            return DataSetResult.asJsonObject(appsUser, CODE_NAME);
        });
    }

    @HttpPath(value = "/create2", secured = false)
    public DataSetResult create2(DataSetRequest request) {
        final String code = request.getString("code");
        final String value = request.getString("value");
        if (notValid(code)) {
            throw new IllegalArgumentException(locale.get("actionPrevious"));
        } else if (notValid(value)) {
            throw new IllegalArgumentException(locale.get("apps_user.password.invalid"));
        }
        return database.transact(query -> {
            DataSet appsUser = query.getDataSet(query.selectAll(APPS_USER_TBL).where("password", code).limit(1));
            if (appsUser == null) {
                throw new NullPointerException(locale.get("actionPrevious"));
            }
            appsUser.unlock().set("password", value).set("status", "Active");
            query.update(appsUser, APPS_USER_TBL, CREATE_USER);
            return DataSetResult.asJsonObject(appsUser, SEARCH);
        });
    }

    @HttpPath("/update")
    @HttpMethod("PUT")
    public DataSetResult update(DataSetRequest req) {
        return database.transact(query -> {
            List<DataSet> userList = req.getDataSetList(APPS_USER_TBL);
            for (DataSet user : userList) {
                setStatus(user);
                validateRequired(user);
            }
            query.update(userList, APPS_USER_TBL, UPDATE);
            return DataSetResult.success("actionSave.msg", userList.size());
        });
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public DataSetResult delete(DataSetRequest req) {
        return database.transact(query -> {
            List<DataSet> userList = req.getDataSetList(APPS_USER_TBL);
            query.delete(userList, APPS_USER_TBL);
            return DataSetResult.success("actionDelete.msg", userList.size());
        });
    }

    private void setStatus(DataSet source) {
        String status = (String) source.get("status");
        if ("Active".equals(status) || "Inactive".equals(status)) {
            source.set("status", status);
        } else {
            source.set("status", "Active");
        }
    }

    private boolean notValid(String code) {
        return code == null || code.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            Pattern pattern = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);
            return pattern.matcher(email).matches();
        }
        return false;
    }

    private void validateRequired(DataSet dataSet) {
        for (String col : UPDATE) {
            Object value = dataSet.get(col);
            if (value == null) {
                throw new RuntimeException(APPS_USER_TBL + "." + col + ".invalid");
            } else if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    throw new RuntimeException(APPS_USER_TBL + "." + col + ".invalid");
                }
            }
        }
    }
}

package sample.dcapture.sql.service;

import dcapture.io.LocaleException;
import dcapture.io.Paging;
import dcapture.sql.core.*;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Path;
import java.sql.SQLException;
import java.util.List;

@Path("/expense_category")
public class ExpenseCategoryService extends SqlMapper {
    private static final Logger logger = Logger.getLogger(ExpenseCategoryService.class);
    private SqlDatabase database;

    @Inject
    public ExpenseCategoryService(SqlDatabase database) {
        this.database = database;
    }

    @Path("/search")
    public JsonObject search(JsonObject req) {
        JsonObjectBuilder result = Json.createObjectBuilder();
        try {
            Paging paging = parsePaging(req);
            SqlTable sqlTable = database.getTable("expense_category");
            SqlQuery[] queries = querySearchCount(database, sqlTable, paging);
            SqlReader reader = database.getReader();
            List<Entity> dataList = reader.find(sqlTable.getName(), queries[0]);
            Number count = (Number) reader.getValue(queries[1]);
            JsonArray dataArray = parseJsonArray(database, sqlTable, dataList);
            result.add("expense_category", dataArray);
            setPaging(result, paging, count.intValue(), dataList.size());
        } catch (SQLException ex) {
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
        return result.build();
    }

    @Path("/save")
    public JsonArray save(JsonArray req) throws SQLException {
        SqlTable sqlTable = database.getTable("expense_category");
        List<Entity> entityList = parseEntities(database, sqlTable, req);
        for (Entity entity : entityList) {
            setStatus(entity);
            String error = isValidRequired(sqlTable, "required", entity);
            if (error != null) {
                throw new LocaleException(error);
            }
        }
        SqlTransaction transaction = database.beginTransaction();
        transaction.save("expense_category", "edit", "unique", entityList);
        transaction.commit();
        return req;
    }

    @Path("/delete")
    public JsonArray delete(JsonArray req) throws SQLException {
        SqlTable sqlTable = database.getTable("expense_category");
        List<Entity> entityList = parseEntities(database, sqlTable, req);
        SqlTransaction transaction = database.beginTransaction();
        transaction.delete("expense_category", entityList);
        transaction.commit();
        return req;
    }

    private void setStatus(Entity source) {
        String status = (String) source.getValue("status");
        if ("Active".equals(status) || "Inactive".equals(status)) {
            source.setValue("status", status);
        } else {
            source.setValue("status", "Active");
        }
    }
}

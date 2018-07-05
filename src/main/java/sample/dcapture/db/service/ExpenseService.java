package sample.dcapture.db.service;

import dcapture.io.LocaleException;
import dcapture.io.Paging;
import dcapture.db.core.*;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Path;
import java.sql.SQLException;
import java.util.*;

@Path("/expense")
public class ExpenseService extends SqlMapper {
    private static final Logger logger = Logger.getLogger(ExpenseService.class);
    private SqlDatabase database;

    @Inject
    public ExpenseService(SqlDatabase database) {
        this.database = database;
    }

    @Path("/search")
    public JsonObject search(JsonObject req) {
        JsonObjectBuilder result = Json.createObjectBuilder();
        try {
            Paging paging = parsePaging(req);
            SqlTable sqlTable = database.getTable("expense");
            SqlQuery[] queries = querySearchCount(database, sqlTable, paging);
            SqlReader reader = database.getReader();
            List<DataSet> dataList = reader.find(sqlTable.getName(), queries[0]);
            Number count = (Number) reader.getValue(queries[1]);
            JsonArray dataArray = toJsonArray(database, sqlTable, dataList);
            result.add("expense", dataArray);
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
        SqlTable sqlTable = database.getTable("expense");
        List<DataModel> modelList = toDataModels(database, sqlTable, req);
        List<DataSet> dataSets = new ArrayList<>();
        for (DataModel model : modelList) {
            setTransactionCode(model);
            setStatus(model);
            String error = isValidRequired(sqlTable, "required", model);
            if (error != null) {
                throw new LocaleException(error);
            }
            dataSets.add(model.as());
        }
        SqlTransaction transaction = database.beginTransaction();
        transaction.save("expense", "edit", dataSets);
        transaction.commit();
        return req;
    }

    @Path("/delete")
    public JsonArray delete(JsonArray req) throws SQLException {
        SqlTable sqlTable = database.getTable("expense");
        List<DataSet> dataSets = toDataSets(database, sqlTable, req);
        SqlTransaction transaction = database.beginTransaction();
        transaction.delete("expense", dataSets);
        transaction.commit();
        return req;
    }

    private void setStatus(DataModel source) {
        String status = (String) source.getValue("status");
        if ("Active".equals(status) || "Inactive".equals(status)) {
            source.setValue("status", status);
        } else {
            source.setValue("status", "Active");
        }
    }

    private void setTransactionCode(DataModel source) {
        if (source.getValue("code") == null) {
            UUID random = UUID.randomUUID();
            String code = random.toString();
            if (4 < code.length()) {
                code = code.substring(0, 4);
            }
            source.setValue("code", code);
        }
    }
}

package sample.dcapture.db.service;

import dcapture.db.core.DataSet;
import dcapture.db.core.SelectBuilder;
import dcapture.db.core.SqlDatabase;
import dcapture.db.util.DataSetRequest;
import dcapture.db.util.DataSetResult;
import dcapture.db.util.Paging;
import dcapture.io.HttpMethod;
import dcapture.io.HttpPath;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@HttpPath("/currency")
public class CurrencyService {
    private static final int PAGE_LIMIT = 20;
    private static final String CURRENCY_TBL = "currency";
    private static final String[] REQUIRED = new String[]{"code", "name", "symbol"};
    private static final String[] INS_UPD_EXT = new String[]{"code", "name", "symbol", "precision"};
    private SqlDatabase database;

    @Inject
    public CurrencyService(SqlDatabase database) {
        this.database = database;
    }

    @HttpPath("/search")
    public DataSetResult search(DataSetRequest req) {
        return database.transact(query -> {
            Paging paging = req.getPaging(PAGE_LIMIT);
            SelectBuilder select = query.selectAll(CURRENCY_TBL).like(req.getString("searchText"), "code, name")
                    .orderBy("code, name").limit(paging.getLimit(), paging.getOffset());
            paging.setDataList(query.getDataSetList(select));
            paging.setTotalRecords(query.getCount(select));
            return DataSetResult.asJsonObject(paging, CURRENCY_TBL);
        });
    }

    @HttpPath("/reload")
    public DataSetResult load(DataSetRequest req) {
        return database.transact(query -> {
            DataSet source = req.getDataSet(CURRENCY_TBL);
            SelectBuilder select = query.selectAll(CURRENCY_TBL);
            if (source.isPrimaryId("id")) {
                select.where("id", source.getLong("id")).limit(1);
                source = query.getDataSet(select);
            } else {
                source.set("precision", 4);
            }
            return DataSetResult.asJsonObject(source, CURRENCY_TBL);
        });
    }

    @HttpPath("/save")
    @HttpMethod("PUT")
    public DataSetResult save(DataSetRequest req) {
        List<DataSet> sourceList = req.getDataSetList(CURRENCY_TBL);
        List<DataSet> insertList = new ArrayList<>();
        List<DataSet> updateList = new ArrayList<>();
        for (DataSet source : sourceList) {
            validateRequired(source);
            if (0 == source.getLong("id")) {
                insertList.add(source);
            } else {
                updateList.add(source);
            }
        }
        return database.transact(query -> {
            if (!insertList.isEmpty()) {
                query.insert(insertList, CURRENCY_TBL, INS_UPD_EXT);
            }
            if (!updateList.isEmpty()) {
                query.update(updateList, CURRENCY_TBL, INS_UPD_EXT);
            }
            return DataSetResult.success("actionSave.msg", insertList.size() + updateList.size());
        });
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public DataSetResult delete(DataSetRequest req) {
        return database.transact(query -> {
            List<DataSet> sourceList = req.getDataSetList(CURRENCY_TBL);
            query.delete(sourceList, CURRENCY_TBL);
            return DataSetResult.success("actionDelete.msg", sourceList.size());
        });
    }

    @HttpPath("/import/csv")
    @HttpMethod("PUT")
    public DataSetResult importCsv(DataSetRequest req) throws IOException {
        List<DataSet> sourceList = null; // database.getParser().getDataSetsFromCsv(request, TABLE);
        for (DataSet model : sourceList) {
            validateRequired(model);
        }
        return database.transact(query -> {
            query.insert(sourceList, CURRENCY_TBL, INS_UPD_EXT);
            return DataSetResult.success("actionImport.msg", sourceList.size());
        });
    }

    @HttpPath("/export/csv")
    public DataSetResult exportCsv() {
        return database.transact(query -> {
            SelectBuilder select = query.selectFrom(CURRENCY_TBL, INS_UPD_EXT).orderBy("code, name");
            return null; // DataSetResult.asCsv database.getParser().sendCsvAttachment(response, CURRENCY_TBL, query.getResult(select));
        });
    }

    private void validateRequired(DataSet dataSet) {
        for (String col : REQUIRED) {
            Object value = dataSet.get(col);
            if (value == null) {
                throw new RuntimeException(CURRENCY_TBL + "." + col + ".invalid");
            } else if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    throw new RuntimeException(CURRENCY_TBL + "." + col + ".invalid");
                }
            }
        }
    }
}

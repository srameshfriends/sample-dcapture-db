package sample.dcapture.db.service;

import dcapture.db.core.DataSet;
import dcapture.db.core.SqlDatabase;
import dcapture.db.util.DataSetResult;
import dcapture.io.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import sample.dcapture.db.shared.KeySequence;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@HttpPath("/console")
public class ConsoleService implements IOStream {
    private static final Logger logger = Logger.getLogger(ConsoleService.class);
    private final AppSettings settings;
    private final SqlDatabase database;

    @Inject
    public ConsoleService(AppSettings settings, SqlDatabase database) {
        this.settings = settings;
        this.database = database;
    }

    @HttpMethod("GET")
    @HttpPath("/execute/forwardQueries")
    public DataSetResult executeForwardQueries() {
        return database.transact(query -> {
            database.getTools(query).executeForwardQueries();
            return DataSetResult.successMessage("successful");
        });
    }

    @HttpMethod("GET")
    @HttpPath("/keySequence/insert")
    public DataSetResult keySequenceInsert() {
        final List<DataSet> sourceList = new ArrayList<>();
        String path = settings.getAppData("keySequence");
        if (path == null) {
            return DataSetResult.error("keySequence.invalid");
        }
        logger.info("Loading App Data : " + ConsoleService.class.getResource(path));
        InputStream stream = ConsoleService.class.getResourceAsStream(path);
        try (CSVParser parser = CSVParser.parse(stream, StandardCharsets.UTF_8, CSVFormat.DEFAULT)) {
            List<CSVRecord> recordList = parser.getRecords();
            for (CSVRecord record : recordList) {
                DataSet source = new DataSet();
                source.set("name", record.get(0).trim());
                source.set("type", record.get(1).trim());
                source.set("prefix", record.get(2).trim());
                sourceList.add(source);
            }
            database.transact(query -> {
                KeySequence keySequence = KeySequence.create(query);
                keySequence.save(sourceList);
                return true;
            });
        } catch (IOException ex) {
            ex.printStackTrace();
            return DataSetResult.errorMessage("Key sequence would not be loaded : " + ex.getMessage());
        }
        return DataSetResult.successMessage("successful");
    }

    @HttpMethod("GET")
    @HttpPath("/locale/insert")
    public DataSetResult localeInsert() throws Exception {
        Localization localization = Localization.load(settings, ConsoleService.this);
        List<DataSet> sourceList = getDataSetList(localization.getPropertiesMap());
        logger.info("Insert localizations : " + sourceList.size());
        database.transact(query -> {
            query.insert(sourceList, "locale", new String[]{"language", "code", "name"});
            return true;
        });
        return DataSetResult.successMessage("successful");
    }

    private List<DataSet> getDataSetList(Map<String, Properties> localeMap) {
        List<DataSet> dataSetList = new ArrayList<>();
        for (Map.Entry<String, Properties> entry : localeMap.entrySet()) {
            Properties prop = entry.getValue();
            for (Map.Entry<Object, Object> keyValue : prop.entrySet()) {
                DataSet dataSet = new DataSet();
                dataSet.set("language", entry.getKey());
                dataSet.set("code", keyValue.getKey().toString());
                dataSet.set("name", keyValue.getValue().toString());
                dataSetList.add(dataSet);
            }
        }
        return dataSetList;
    }

    @Override
    public InputStream getResourceAsStream(String file) {
        return ConsoleService.class.getResourceAsStream(file);
    }
}

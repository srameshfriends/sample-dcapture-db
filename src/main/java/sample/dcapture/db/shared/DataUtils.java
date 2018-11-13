package sample.dcapture.db.shared;

import dcapture.db.core.DataSet;
import dcapture.db.core.SqlDatabase;
import dcapture.db.h2.H2Database;
import dcapture.db.postgres.PgDatabase;
import dcapture.io.AppSettings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class DataUtils {
    private static final Logger logger = Logger.getLogger("sample.dcapture.db");

    public static SqlDatabase loadDatabase(AppSettings settings) {
        String[] values = settings.getDatabase().trim().split(" ");
        if (3 > values.length) {
            throw new IllegalArgumentException("Database user and password should be encrypted, and format is : " +
                    "name [space] url [space] userName [space] password");
        }
        String url = values[0].trim(), user = values[1].trim(), password = values[2].trim();
        if (url.isEmpty()) {
            throw new IllegalArgumentException("Database url not valid");
        }
        if (user.isEmpty()) {
            throw new IllegalArgumentException("Database user not valid");
        }
        if (password.isEmpty()) {
            throw new IllegalArgumentException("Database password not valid");
        }
        List<JsonArray> tables = new ArrayList<>();
        for (String path : settings.getDatabases()) {
            logger.severe("Database configuration reading from " + DataUtils.class.getResource(path));
            JsonReader reader = Json.createReader(DataUtils.class.getResourceAsStream(path));
            tables.add(reader.readArray());
        }
        List<JsonObject> columnSets = new ArrayList<>();
        if (settings.getColumnSets() != null) {
            for (String columnSet : settings.getColumnSets()) {
                InputStream stream = DataUtils.class.getResourceAsStream(columnSet);
                JsonReader reader = Json.createReader(stream);
                columnSets.add(reader.readObject());
            }
        }
        Properties prop = new Properties();
        prop.put("url", url);
        prop.put("user", AppSettings.decode(user));
        prop.put("password", AppSettings.decode(password));
        prop.put("tables", tables);
        prop.put("columnSets", columnSets);
        if (url.toLowerCase().contains("postgresql")) {
            PgDatabase.PgDatabaseBuilder builder = new PgDatabase.PgDatabaseBuilder();
            return builder.build(prop);
        } else if (url.toLowerCase().contains("h2")) {
            H2Database.H2DatabaseBuilder builder = new H2Database.H2DatabaseBuilder();
            return builder.build(prop);
        } else {
            throw new NullPointerException("Database url driver not supported : " + url);
        }
    }

    public static List<DataSet> loadKeySequences(AppSettings settings) {
        final List<DataSet> sourceList = new ArrayList<>();
        String path = settings.getAppData("keySequence");
        if (path == null) {
            return sourceList;
        }
        logger.severe("Loading App Data : " + DataUtils.class.getResource(path));
        InputStream stream = DataUtils.class.getResourceAsStream(path);
        try (CSVParser parser = CSVParser.parse(stream, StandardCharsets.UTF_8, CSVFormat.DEFAULT)){
            List<CSVRecord> recordList = parser.getRecords();
            for (CSVRecord record : recordList) {
                DataSet source = new DataSet();
                source.set("name", record.get(0).trim());
                source.set("type", record.get(1).trim());
                source.set("prefix", record.get(2).trim());
                sourceList.add(source);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return sourceList;
    }
}

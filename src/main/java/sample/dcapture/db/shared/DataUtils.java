package sample.dcapture.db.shared;

import dcapture.db.core.DataSet;
import dcapture.io.AppSettings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DataUtils {
    private static final Logger logger = Logger.getLogger("sample.dcapture.db");

    public static List<DataSet> loadKeySequences(AppSettings settings) {
        final List<DataSet> sourceList = new ArrayList<>();
        String path = settings.getAppData("keySequence");
        if (path == null) {
            return sourceList;
        }
        logger.severe("Loading App Data : " + DataUtils.class.getResource(path));
        InputStream stream = DataUtils.class.getResourceAsStream(path);
        try (CSVParser parser = CSVParser.parse(stream, StandardCharsets.UTF_8, CSVFormat.DEFAULT)) {
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

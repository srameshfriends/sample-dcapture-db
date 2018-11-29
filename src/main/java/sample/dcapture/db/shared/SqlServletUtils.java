package sample.dcapture.db.shared;

import dcapture.db.core.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class SqlServletUtils {
    private static DateTimeFormatter nameFormatter = DateTimeFormatter.ofPattern("yy MM dd-HH mm");
    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");
    private static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm");

    public static List<DataSet> getDataSetsFromCsv(HttpServletRequest req, String tableName)
            throws IOException {
        SqlTable sqlTable = SqlContext.getSqlTable(tableName);
        CSVParser parser = CSVParser.parse(req.getInputStream(), StandardCharsets.UTF_8, CSVFormat.DEFAULT.withHeader());
        Map<String, Integer> headerMap = parser.getHeaderMap();
        List<CSVRecord> recordList = parser.getRecords();
        SqlColumn sqlColumn;
        List<DataSet> list = new ArrayList<>();
        for (CSVRecord record : recordList) {
            DataSet dataSet = new DataSet();
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                sqlColumn = sqlTable.getColumn(entry.getKey());
                if (sqlColumn != null) {
                    dataSet.set(sqlColumn.getName(), getFieldValue(sqlColumn, record.get(entry.getValue())));
                }
            }
            list.add(dataSet);
        }
        return list;
    }

    private static Object getFieldValue(SqlColumn sqlColumn, String text) {
        if (text == null || text.trim().isEmpty()) {
            if (String.class.equals(sqlColumn.getModel())) {
                return text == null ? null : "";
            } else if (Boolean.class.equals(sqlColumn.getModel())) {
                return false;
            } else if (Double.class.equals(sqlColumn.getModel())) {
                return 0D;
            } else if (Integer.class.equals(sqlColumn.getModel())) {
                return 0;
            } else if (BigDecimal.class.equals(sqlColumn.getModel())) {
                return BigDecimal.ZERO;
            }
            return null;
        }
        text = text.trim();
        if (sqlColumn.getReference() != null) {
            try {
                DataSet dateSet = new DataSet();
                dateSet.set(sqlColumn.getName(), Long.parseLong(text));
                return dateSet;
            } catch (NumberFormatException ex) {
                //ignore
            }
            return null;
        } else if (String.class.equals(sqlColumn.getModel())) {
            return text;
        } else if (Boolean.class.equals(sqlColumn.getModel())) {
            return "true".equals(text.trim().toLowerCase());
        } else if (Double.class.equals(sqlColumn.getModel())) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ex) {
                //ignore
            }
            return 0D;
        } else if (Integer.class.equals(sqlColumn.getModel())) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                //ignore
            }
            return 0;
        } else if (BigDecimal.class.equals(sqlColumn.getModel())) {
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ex) {
                //ignore
            }
            return BigDecimal.ZERO;
        } else if (LocalDate.class.equals(sqlColumn.getModel())) {
            try {
                return dateFormat.parse(text);
            } catch (NumberFormatException ex) {
                //ignore
            }
            return null;
        } else if (LocalDateTime.class.equals(sqlColumn.getModel())) {
            try {
                return dateTimeFormat.parse(text);
            } catch (NumberFormatException ex) {
                //ignore
            }
            return null;
        } else if (LocalTime.class.equals(sqlColumn.getModel())) {
            try {
                return timeFormat.parse(text);
            } catch (NumberFormatException ex) {
                //ignore
            }
            return text;
        }
        throw new IllegalArgumentException(sqlColumn.toString() + " :  ");
    }

    private static String getFileName(String name, String format) {
        String suffix = "-" + nameFormatter.format(LocalDateTime.now()) + "." + format;
        suffix = suffix.replaceAll("\\s", "");
        return name + suffix;
    }

    public static int sendCsvAttachment(HttpServletResponse resp, String[] header, String name, List<Object[]> sources) {
        resp.setContentType("text/csv");
        resp.setHeader("Content-disposition", "attachment; filename=" + getFileName(name, "csv"));
        try (CSVPrinter printer = new CSVPrinter(resp.getWriter(), CSVFormat.DEFAULT.withHeader(header))) {
            for (Object[] values : sources) {
                printer.printRecord(values);
            }
            printer.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return sources.size();
    }

    public static int sendCsvAttachment(HttpServletResponse resp, String name, SqlResult result) {
        resp.setContentType("text/csv");
        resp.setHeader("Content-disposition", "attachment; filename=" + getFileName(name, "csv"));
        List<String> header = new ArrayList<>();
        for (SqlMetaData smd : result.getMetaDataList()) {
            header.add(smd.getTable() + "." + smd.getColumn());
        }
        String[] headerArray = header.toArray(new String[0]);
        int count = result.getObjectsList().size();
        try (CSVPrinter printer = new CSVPrinter(resp.getWriter(), CSVFormat.DEFAULT.withHeader(headerArray))) {
            for (Object[] values : result.getObjectsList()) {
                printer.printRecord(values);
            }
            printer.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return count;
    }

    public static void sendCsv(HttpServletResponse resp, SqlResult result) {
        resp.setContentType("text/csv");
        List<String> header = new ArrayList<>();
        for (SqlMetaData smd : result.getMetaDataList()) {
            header.add(smd.getColumn());
        }
        String[] headerArray = header.toArray(new String[0]);
        try (CSVPrinter printer = new CSVPrinter(resp.getWriter(), CSVFormat.DEFAULT.withHeader(headerArray))) {
            for (Object[] values : result.getObjectsList()) {
                printer.printRecord(values);
            }
            printer.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

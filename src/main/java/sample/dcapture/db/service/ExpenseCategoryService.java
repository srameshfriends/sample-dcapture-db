package sample.dcapture.db.service;

import dcapture.db.core.DataSet;
import dcapture.db.core.SelectBuilder;
import dcapture.db.core.SqlDatabase;
import dcapture.db.util.DataSetRequest;
import dcapture.db.util.DataSetResult;
import dcapture.db.util.Paging;
import dcapture.io.HttpMethod;
import dcapture.io.HttpPath;
import sample.dcapture.db.shared.KeySequence;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@HttpPath(value = "/expense_category", secured = false)
public class ExpenseCategoryService {
    private static final int PAGE_LIMIT = 20;
    private static final String UPLOAD_DIR = "dcapture";
    private static final String CATEGORY_TBL = "expense_category";
    private static final String[] REQUIRED = new String[]{"code", "name"};
    private static final String[] INSERT = REQUIRED, UPDATE = REQUIRED;
    private final SqlDatabase database;

    @Inject
    public ExpenseCategoryService(SqlDatabase database) {
        this.database = database;
    }

    @HttpPath("/search")
    public DataSetResult search(DataSetRequest req) {
        return database.transact(query -> {
            Paging paging = req.getPaging(PAGE_LIMIT);
            SelectBuilder select = query.selectAll(CATEGORY_TBL).like(req.getString("searchText"), REQUIRED)
                    .orderBy("code, name").limit(paging.getLimit(), paging.getOffset());
            paging.setDataList(query.getDataSetList(select));
            paging.setTotalRecords(query.getCount(select));
            return DataSetResult.asJsonObject(paging, CATEGORY_TBL);
        });
    }

    @HttpPath("/reload")
    public DataSetResult load(DataSetRequest req) {
        return database.transact(query -> {
            DataSet source = req.getDataSet(CATEGORY_TBL);
            SelectBuilder select = query.selectAll(CATEGORY_TBL);
            if (source.isPrimaryId("id")) {
                select.where("id", source.getLong("id")).limit(1);
                source = query.getDataSet(select);
            } else {
                source.set("code", "AUTO");
            }
            return DataSetResult.asJsonObject(source, CATEGORY_TBL);
        });
    }

    @HttpPath("/save")
    @HttpMethod("PUT")
    public DataSetResult save(DataSetRequest req) {
        return database.transact(query -> {
            List<DataSet> sourceList = req.getDataSetList(CATEGORY_TBL);
            List<DataSet> insertList = new ArrayList<>();
            List<DataSet> updateList = new ArrayList<>();
            KeySequence keySequence = KeySequence.create(query);
            for (DataSet source : sourceList) {
                String code = source.getString("code", "");
                if (code.isEmpty() || "AUTO".equals(code.toUpperCase())) {
                    keySequence.generate(CATEGORY_TBL, source, "code");
                }
                validateRequired(source);
                if (0 == source.getLong("id")) {
                    insertList.add(source);
                } else {
                    updateList.add(source);
                }
            }
            if (!insertList.isEmpty()) {
                query.insert(insertList, CATEGORY_TBL, INSERT);
            }
            if (!updateList.isEmpty()) {
                query.update(updateList, CATEGORY_TBL, UPDATE);
            }
            return DataSetResult.success("actionSave.msg", insertList.size() + updateList.size());
        });
    }

    @HttpPath("/delete")
    @HttpMethod("DELETE")
    public DataSetResult delete(DataSetRequest req) {
        return database.transact(query -> {
            List<DataSet> sourceList = req.getDataSetList(CATEGORY_TBL);
            query.delete(sourceList, "expense_category");
            return DataSetResult.success("actionDelete.msg", sourceList.size());
        });
    }

    @HttpPath(value = "/upload", secured = false)
    @HttpMethod("GET")
    private void upload(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.append("<!DOCTYPE html>\r\n")
                .append("<html>\r\n")
                .append("    <head>\r\n")
                .append("        <title>File Upload Form</title>\r\n")
                .append("    </head>\r\n")
                .append("    <body>\r\n");
        writer.append("<h1>Upload file</h1>\r\n");
        writer.append("<form method=\"POST\" action=\"receipt\" ")
                .append("enctype=\"multipart/form-data\">\r\n");
        writer.append("<input type=\"file\" name=\"fileName1\"/><br/><br/>\r\n");
        writer.append("<input type=\"file\" name=\"fileName2\"/><br/><br/>\r\n");
        writer.append("<input type=\"submit\" value=\"Submit\"/>\r\n");
        writer.append("</form>\r\n");
        writer.append("    </body>\r\n").append("</html>\r\n");
    }

    @HttpPath(value = "/receipt", secured = false)
    public void receipt(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        // gets absolute path of the web application
        String userHome = System.getProperty("user.home");
        // constructs path of the directory to save uploaded file
        Path uploadFolder = Paths.get(userHome, UPLOAD_DIR);
        if (!Files.isDirectory(uploadFolder)) {
            Files.createDirectory(uploadFolder);
        }
        PrintWriter writer = response.getWriter();
        // write all files in upload folder
        writer.write(uploadFolder.toString());
        for (Part part : request.getParts()) {
            if (part != null && part.getSize() > 0) {
                String fileName = part.getSubmittedFileName();
                Path path = Paths.get(uploadFolder.toString(), fileName);
                Files.copy(part.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                writer.write("File successfully uploaded to "
                        + uploadFolder.toString() + File.separator + fileName + "<br>\r\n");
            }
        }
        writer.close();
    }

    private void validateRequired(DataSet dataSet) {
        for (String col : REQUIRED) {
            Object value = dataSet.get(col);
            if (value == null) {
                throw new RuntimeException(CATEGORY_TBL + "." + col + ".invalid");
            } else if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    throw new RuntimeException(CATEGORY_TBL + "." + col + ".invalid");
                }
            }
        }
    }
}

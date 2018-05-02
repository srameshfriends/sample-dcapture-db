package sample.dcapture.sql.service;

import dcapture.sql.core.*;
import org.apache.log4j.Logger;
import sample.dcapture.sql.model.User;

import javax.json.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

public class UserServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(UserServlet.class);
    private DatabaseContext context;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        context = (DatabaseContext) config.getServletContext().getAttribute(DatabaseContext.class.getSimpleName());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getPathInfo() == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Requested service not available!");
            return;
        }
        String pathInfo = request.getPathInfo().trim().toLowerCase();
        pathInfo = pathInfo.replaceFirst("/", "");
        if ("search".equals(pathInfo)) {
            search(response);
        } else if ("create".equals(pathInfo)) {
            createUser(request, response);
        } else if ("update".equals(pathInfo)) {
            updateUser(request, response);
        } else if ("delete".equals(pathInfo)) {
            deleteUser(request, response);
        } else {
            response.sendRedirect("../error.html?" + "Service not found '" + pathInfo + "'");
        }
    }

    private void createUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            DataParser parser = new DataParser();
            User user = parser.toUser(request.getParameterMap());
            String error = parser.getErrorMessage(user);
            if (error != null) {
                logger.error(error + " \t " + user);
                response.sendRedirect("../error.html?" + error);
                return;
            }
            SqlTransaction transaction = context.beginTransaction();
            transaction.insert(user, "all");
            transaction.commit();
            response.sendRedirect("../user-list.html");
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            response.sendRedirect("../error.html?" + ex.getMessage());
        }
    }

    private void updateUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            DataParser parser = new DataParser();
            User user = parser.toUser(request.getParameterMap());
            String error = parser.getErrorMessage(user);
            if (error != null) {
                logger.error(error + " \t " + user);
                response.sendRedirect("../error.html?" + error);
                return;
            }
            SqlTransaction transaction = context.beginTransaction();
            transaction.update(user, "all");
            transaction.commit();
            response.sendRedirect("../user-list.html");
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            response.sendRedirect("../error.html?" + ex.getMessage());
        }
    }

    private void deleteUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            DataParser parser = new DataParser();
            JsonObject object = parser.getJsonObject(request);
            User user = parser.toUser(object);
            if (1 > user.getId()) {
                String error = "This is not a valid user to delete : " + user.toString();
                logger.error(error);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, error);
                return;
            }
            SqlTransaction transaction = context.beginTransaction();
            transaction.delete(user);
            transaction.commit();
            send(parser.getJsonObject(user), response);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        }
    }

    private void search(HttpServletResponse response) throws IOException {
        try {
            SqlQuery query = context.getQuery().add("SELECT id, email, user_name, full_name, password FROM ")
                    .table("user").add(" ORDER BY email");
            SqlTransaction transaction = context.beginTransaction();
            SqlResult sqlResult = transaction.getResult(query);
            DataParser parser = new DataParser();
            JsonArray array = parser.getJsonArrayUser(sqlResult);
            send(array, response);
        } catch (SQLException ex) {
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,  ex.getMessage());
        }
    }

    private void send(JsonStructure result, HttpServletResponse response) throws IOException {
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            JsonWriter writer = Json.createWriter(response.getWriter());
            writer.write(result);
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
            response.sendRedirect("../error.html?" + ex.getMessage());
        }
    }
}

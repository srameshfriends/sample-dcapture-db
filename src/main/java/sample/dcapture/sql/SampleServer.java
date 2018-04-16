package sample.dcapture.sql;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class SampleServer extends SampleSettings {
    private static final Logger logger = Logger.getLogger(SampleServer.class);
    private Server server;

    private void init() {
        ServletHolder defaultServlet = new ServletHolder(DefaultServlet.class);
        defaultServlet.setInitParameter("resourceBase", RESOURCE_BASE);
        ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContext.setContextPath("/");
        servletContext.setResourceBase(RESOURCE_BASE);
        servletContext.addEventListener(new SampleContextListener());
        servletContext.addServlet(defaultServlet, "/*");
        servletContext.setWelcomeFiles(new String[]{"index.html"});
        addServlet(servletContext);
        server = new Server(SERVER_PORT);
        server.setHandler(servletContext);
        logger.info("Resource base : " + servletContext.getResourceBase());
        logger.info("http://localhost:" + SERVER_PORT + "/index.html");
    }

    private void start() {
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(final String[] args) {
        SampleServer server = new SampleServer();
        server.init();
        server.start();
    }
}

package sample.dcapture.db.luncher;

import dcapture.io.BaseSettings;
import dcapture.io.DispatcherListener;
import dcapture.io.DispatcherServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main extends Registry {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    @Override
    protected void start(String... args) throws Exception {
        InputStream stream = Main.class.getResourceAsStream("/logging.properties");
        LogManager.getLogManager().readConfiguration(stream);
        BaseSettings settings = BaseSettings.load(Main.class);
        Server server = new Server(settings.getPort());
        ServletHolder defaultServlet = new ServletHolder(DefaultServlet.class);
        ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContext.setContextPath("/");
        servletContext.setResourceBase(Main.class.getResource("/webapp").toURI().toString());
        servletContext.addServlet(DispatcherServlet.class, "/api/*");
        servletContext.addServlet(defaultServlet, "/*");
        servletContext.setGzipHandler(new GzipHandler());
        DispatcherListener listener = new DispatcherListener();
        listener.setRegistry(this);
        servletContext.addEventListener(listener);
        servletContext.setWelcomeFiles(new String[]{"index.html"});
        servletContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        String url = server.getURI().toString();
        url = url.substring(0, url.length() - 1);
        logger.severe(url + ":" + settings.getPort() + "/index.html");
        logger.severe("Resource base : " + servletContext.getResourceBase());
        server.setHandler(servletContext);
        server.start();
        server.join();
        logger.severe("*** READY TO USE ***");
    }

    @Override
    protected void stop(String... args) {
    }

    public static void main(String... args) {
        try {
            new Main().start(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}

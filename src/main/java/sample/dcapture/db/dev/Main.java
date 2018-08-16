package sample.dcapture.db.dev;

import dcapture.io.BaseSettings;
import dcapture.io.DispatcherListener;
import dcapture.io.DispatcherServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main extends Registry implements Runnable {
    private static final Logger logger = LogManager.getLogger(Main.class);

    @Override
    public void run() {
        try {
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
            logger.info(url + ":" + settings.getPort() + "/index.html");
            logger.info("Resource base : " + servletContext.getResourceBase());
            server.setHandler(servletContext);
            server.start();
            server.join();
            logger.info("*** READY TO USE ***");
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
            System.exit(1);
        }
    }

    public static void main(String... args) throws Exception {
        new Main().run();
    }
}

package sample.dcapture.db.dev;

import dcapture.io.AppSettings;
import dcapture.io.DispatcherServlet;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import sample.dcapture.db.service.SampleServletListener;

import javax.servlet.MultipartConfigElement;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    public Main() throws Exception {
        SampleServletListener listener = new SampleServletListener();
        AppSettings settings = AppSettings.load(listener.getResourceAsStream(AppSettings.PATH));
        Server server = new Server(settings.getPort());
        ServletHolder defaultHolder = new ServletHolder(new DefaultServlet());
        ServletHolder dispatcherHolder = new ServletHolder(new DispatcherServlet());
        dispatcherHolder.getRegistration().setMultipartConfig(getMultipartConfig());
        ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContext.setContextPath("/");
        servletContext.setResourceBase("/home/ramesh/IdeaProjects/sample-dcapture-db/src/main/webapp/");
        servletContext.addServlet(dispatcherHolder, "/api/*");
        servletContext.addServlet(defaultHolder, "/*");
        servletContext.setGzipHandler(new GzipHandler());
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
    }

    private MultipartConfigElement getMultipartConfig() { // 5 MB , 20 MB, 0
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "dcapture", "multipart");
        return new MultipartConfigElement(path.toString()); // 5242880L, 20971520L, 0
    }

    public static void main(String... args) {
        try {
            new Main();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}

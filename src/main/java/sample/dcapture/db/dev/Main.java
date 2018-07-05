package sample.dcapture.db.dev;

import dcapture.io.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main extends Registry implements Runnable {

    @Override
    public void run() {
        try {
            BaseSettings settings = getBaseSettings();
            final String resourceBase = settings.getWebAppFolder().getAbsolutePath();
            Server server = new Server(settings.getPort());
            ServletHolder defaultServlet = new ServletHolder(DefaultServlet.class);
            defaultServlet.setInitParameter("resourceBase", resourceBase);
            ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
            servletContext.setContextPath("/");
            servletContext.setResourceBase(resourceBase);
            servletContext.addServlet(DispatcherServlet.class, "/api/*");
            servletContext.addServlet(defaultServlet, "/*");
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
        }
    }

    public static void main(String... args) {
        new Main().run();
    }
}

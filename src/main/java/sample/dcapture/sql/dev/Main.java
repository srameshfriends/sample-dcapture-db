package sample.dcapture.sql.dev;

import dcapture.io.*;
import dcapture.sql.core.SqlDatabase;
import dcapture.sql.core.SqlForwardTool;
import dcapture.sql.core.SqlLogger;
import dcapture.sql.core.SqlTable;
import dcapture.sql.postgres.PgDatabase;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.bind.Binder;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

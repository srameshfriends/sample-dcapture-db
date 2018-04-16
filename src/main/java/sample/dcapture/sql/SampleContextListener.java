package sample.dcapture.sql;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class SampleContextListener extends SampleSettings implements ServletContextListener {

    @Override
    public final void contextInitialized(ServletContextEvent event) {
        setAttribute(event.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
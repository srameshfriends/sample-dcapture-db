module sample.dcapture.db {
    requires dcapture.db;
    requires dcapture.io;
    requires com.zaxxer.hikari;
    requires log4j.slf4j.impl;
    requires org.apache.logging.log4j;
    requires java.json;
    requires java.ws.rs;
    requires java.sql;
    requires javax.servlet.api;
    requires javax.inject;
    requires org.glassfish.java.json;
    requires org.apache.commons.io;
    requires commons.csv;
    requires jetty.io;
    requires commons.fileupload;
    requires jetty.server;
    requires jetty.servlet;
    requires pustike.inject;
    requires jetty.util;
    requires h2;
    opens sample.dcapture.db.service to dcapture.io, pustike.inject;
}
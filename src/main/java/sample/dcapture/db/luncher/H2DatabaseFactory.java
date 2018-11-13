package sample.dcapture.db.luncher;

import java.sql.SQLException;

public class H2DatabaseFactory {
    private org.h2.tools.Server webServer, tcpServer;

    public static void main(String... args) {
        System.out.println(" *** DCapture H2 Database *** ");
        H2DatabaseFactory database = new H2DatabaseFactory();
        database.start();
    }

    private void start() {
        try {
            if (tcpServer == null) {
                tcpServer = org.h2.tools.Server.createTcpServer();
            }
            if (webServer == null) {
                webServer = org.h2.tools.Server.createWebServer();
            }
            if (!tcpServer.isRunning(true)) {
                tcpServer.start();
            }
            if (webServer != null && !webServer.isRunning(true)) {
                webServer.start();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}

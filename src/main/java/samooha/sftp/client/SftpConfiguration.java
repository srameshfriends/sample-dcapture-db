package samooha.sftp.client;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class SftpConfiguration {
    private static final Logger logger = Logger.getLogger(SftpConfiguration.class);
    private final File configFile;
    private Properties properties;
    private boolean download = false, upload = false, deleteRemoteArchive = false, deleteLocalArchive = false;
    private boolean localWrite = true, remoteWrite = true, remoteArchive = true, localArchive = true;

    public SftpConfiguration(File configFile) {
        this.configFile = configFile;
    }

    public boolean isDownload() {
        return download;
    }

    public boolean isUpload() {
        return upload;
    }

    public boolean isDeleteRemoteArchive() {
        return deleteRemoteArchive;
    }

    public boolean isDeleteLocalArchive() {
        return deleteLocalArchive;
    }

    public boolean isLocalWrite() {
        return localWrite;
    }

    public boolean isRemoteWrite() {
        return remoteWrite;
    }

    public boolean isNotRemoteArchive() {
        return !remoteArchive;
    }

    public boolean isNotLocalArchive() {
        return !localArchive;
    }

    public String get(String name) {
        return properties == null ? null : properties.getProperty(name);
    }

    public void loadConfiguration() throws IOException {
        if (configFile == null) {
            throw new IOException("Configuration file path should not be null");
        }
        if (!configFile.isFile()) {
            throw new IOException("Configuration file not found at (" + configFile + ")");
        }
        properties = new Properties();
        properties.load(new FileReader(configFile));
        updateLog4jConfiguration(properties.getProperty("sftp.log"), properties.getProperty("sftp.pid"));
        logger.info("SFTP Configuration Read from (" + configFile + ")");
        setOperationMode(properties.getProperty("sftp.operation.mode"));
        if (get("sftp.host") == null) {
            throw new NullPointerException("SFTP Host should not be empty");
        }
        if (get("sftp.user") == null) {
            throw new NullPointerException("SFTP User should not be empty");
        }
        if (get("sftp.password") == null) {
            throw new NullPointerException("SFTP Password should not be empty");
        }
        if (get("sftp.port") == null) {
            throw new NullPointerException("SFTP Port should not be empty");
        }
        if (get("sftp.home") == null) {
            throw new NullPointerException("SFTP Home should not be empty");
        }
        if (get("sftp.local.read") != null) {
            remoteWrite = true;
            if (get("sftp.remote.write") == null) {
                properties.put("sftp.remote.write", "in");
            }
        }
        if (get("sftp.local.write") != null) {
            localWrite = true;
            if (get("sftp.remote.read") == null) {
                properties.put("sftp.remote.read", "out");
            }
        }
        if (get("sftp.local.archive") != null) {
            localArchive = true;
        }
        if (get("sftp.remote.archive") != null) {
            remoteArchive = true;
        }
        logger.info(" *** Configured *** ");
    }

    private void setOperationMode(String mode) {
        if (mode == null) {
            return;
        }
        String[] modeArray = mode.toUpperCase().split(",");
        for (String operation : modeArray) {
            switch (operation.trim()) {
                case "DOWNLOAD":
                    download = true;
                    break;
                case "UPLOAD":
                    upload = true;
                    break;
                case "DELETE_REMOTE_ARCHIVE":
                    deleteRemoteArchive = true;
                    break;
                case "DELETE_LOCALE_ARCHIVE":
                    deleteLocalArchive = true;
                    break;
            }
        }
    }

    private void updateLog4jConfiguration(String directory, String pid) throws IOException {
        File folder = directory == null ? null : new File(directory);
        if (folder == null) {
            return;
        }
        if(!folder.isDirectory()) {
            FileUtils.forceMkdir(folder);
        }
        Properties props = new Properties();
        try (InputStream configStream = getClass().getResourceAsStream("/log4j.properties")) {
            props.load(configStream);
        } catch (IOException e) {
            System.err.println("ERROR: Cannot load log4j.properties file");
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String suffix = format.format(new Date());
        suffix = suffix.replaceAll("-", "");
        String loggingAt = new File(folder, pid + "-" + suffix + ".log").toString();
        props.setProperty("log4j.appender.logfile.File", loggingAt);
        PropertyConfigurator.configure(props);
        logger.warn("SFTP SERVICE LOGGED AT " + loggingAt);
    }

    public boolean isRunning() throws IOException {
        String pid = get("sftp.pid");
        if (pid == null) {
            throw new NullPointerException("sftp.pid configuration should not be null");
        }
        File file = new File(System.getProperty("java.io.tmpdir"), pid + ".ini");
        if (!file.isFile()) {
            return false;
        }
        String status = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return "RUNNING".equals(status);
    }

    public void lock() throws IOException {
        String pid = get("sftp.pid");
        if (pid == null) {
            throw new NullPointerException("sftp.pid configuration should not be null");
        }
        File file = new File(System.getProperty("java.io.tmpdir"), pid + ".ini");
        FileUtils.write(file, "RUNNING", StandardCharsets.UTF_8);
    }

    public void unlock() throws IOException {
        String pid = get("sftp.pid");
        if (pid == null) {
            throw new NullPointerException("sftp.pid configuration should not be null");
        }
        File file = new File(System.getProperty("java.io.tmpdir"), pid + ".ini");
        FileUtils.write(file, "STAND BY", StandardCharsets.UTF_8);
    }

    public int getPort() {
        try {
            return Integer.parseInt(get("sftp.port"));
        } catch (Exception ex) {
            logger.info("SFTP Service default port 22 is used.");
        }
        return 22;
    }
}

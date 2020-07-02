package sftp.mailbox;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class SftpConfiguration {
    private static final Logger logger = org.apache.log4j.Logger.getLogger(SftpConfiguration.class);
    private final String filePath;
    private Properties properties;
    private boolean download = false, upload = false, deleteRemoteArchive = false, deleteLocalArchive = false;
    private boolean localWrite = true, remoteWrite = true, remoteArchive = true, localArchive = true;

    public SftpConfiguration(String filePath) {
        this.filePath = filePath;
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

    public boolean isRemoteArchive() {
        return remoteArchive;
    }

    public boolean isLocalArchive() {
        return localArchive;
    }

    public String get(String name) {
        return properties == null ? null : properties.getProperty(name);
    }

    public void loadConfiguration() throws IOException {
        if (filePath == null) {
            throw new IOException("Configuration file path should not be null");
        }
        File file = new File(filePath);
        if (!file.isFile()) {
            throw new IOException("Configuration file not found at (" + filePath + ")");
        }
        properties = new Properties();
        properties.load(new FileReader(file));
        updateLog4jConfiguration(properties.getProperty("sftp.log"), properties.getProperty("sftp.pid"));
        logger.info("SFTP Configuration Read from (" + file.toString() + ")");
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
        try {
            InputStream configStream = getClass().getResourceAsStream("/log4j.properties");
            props.load(configStream);
            configStream.close();
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
            // ignore exception
        }
        return 22;
    }
}

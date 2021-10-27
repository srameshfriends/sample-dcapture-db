package samooha.sftp.client;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Main.class);
    private final SftpConfiguration config;
    private SftpService sftpService;

    public Main(SftpConfiguration configuration) {
        this.config = configuration;
    }

    private void connect() throws JSchException, IOException {
        config.lock();
        logger.info(config.get("sftp.host") + " : Connection request is progress... ");
        sftpService = new SftpService(config);
        sftpService.connect();
        logger.info(" *** Connected *** ");
    }

    private void stop() throws IOException {
        if (sftpService != null) {
            sftpService.disconnect();
        }
        config.unlock();
    }

    public static void main(String[] args) {
        Path path;
        if (args == null || 0 == args.length) {
            path = Paths.get(System.getProperty("user.home"), "sftp-service", "config", "sftp.properties");
        } else {
            path = Paths.get(args[0].trim());
        }
        if (!Files.exists(path)) {
            logger.warn("SFTP Configuration properties file not valid.\n" + path + "\nExiting now");
            System.exit(1);
            return;
        }
        SftpConfiguration config = null;
        try {
            config = new SftpConfiguration(path.toFile());
            config.loadConfiguration();
            Main program = new Main(config);
            program.connect();
            if (config.isUpload()) {
                List<File> fileList = program.upload();
                if (fileList != null) {
                    program.onUploadArchive(fileList);
                }
            }
            if (config.isDownload()) {
                List<ChannelSftp.LsEntry> entries = program.download();
                if (entries != null) {
                    program.onRemoteArchive(entries);
                }
            }
            if (config.isDeleteRemoteArchive()) {
                program.deleteRemoteArchiveFiles();
            }
            if (config.isDeleteLocalArchive()) {
                program.deleteLocalArchiveFiles();
            }
            program.stop();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.trace(ex.getMessage(), ex);
            try {
                if (config != null) {
                    config.unlock();
                }
            } catch (IOException ig) {
                // ignore exception
            }
            System.exit(1);
        }
    }

    private List<File> upload() throws JSchException, SftpException {
        if (!config.isRemoteWrite()) {
            logger.info("Upload folder not found at configuration");
            return null;
        }
        logger.info(" ***** UPLOAD PROCESS IS STARTED ***** ");
        final String writeTo = config.get("sftp.remote.write");
        File readFrom = new File(config.get("sftp.home"), config.get("sftp.local.read"));
        if (!readFrom.isDirectory()) {
            logger.error("SFTP Home folder not valid : " + readFrom);
            return null;
        }
        logger.info("READ FILES FROM : " + readFrom);
        List<File> fileList = (List<File>) FileUtils.listFiles(readFrom, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        if (fileList.isEmpty()) {
            logger.info("No files to upload!");
            return null;
        }
        logger.info(" ***** UPLOAD FILES TO REMOTE HOST ***** ");
        int index = 0;
        for (File file : fileList) {
            index += 1;
            logger.info(index + ", " + file.toString() + " - UPLOAD TO : " + writeTo + "/" + file.getName());
            sftpService.upload(file, writeTo, file.getName());
        }
        logger.info(" ***** UPLOAD FILES COMPLETED ***** ");
        return fileList;
    }

    private void onUploadArchive(final List<File> fileList) throws IOException {
        if (config.isNotLocalArchive()) {
            logger.info("Local files archive folder not found at configuration!");
            return;
        }
        logger.info(" ***** LOCAL FILES ARCHIVE IS STARTED ***** ");
        final File home = new File(config.get("sftp.home"));
        if (!home.isDirectory()) {
            logger.error("SFTP Home folder not valid : " + home);
            return;
        }
        File source = new File(home, config.get("sftp.local.read"));
        if (!source.isDirectory()) {
            logger.error("SFTP local folder not valid : " + source);
            return;
        }
        logger.info("READ FROM TO ARCHIVE : " + source);
        if (fileList.isEmpty()) {
            logger.info("No files to archive!");
        }
        logger.info(" ***** ARCHIVE FILES ***** ");
        File archive = new File(home, config.get("sftp.local.archive"));
        archive = new File(archive, config.get("sftp.local.read"));
        int index = 1;
        for (File sourceFile : fileList) {
            File targetFile = new File(archive, sourceFile.getName());
            logger.info(index + ", " + sourceFile + " - ARCHIVE TO : " + targetFile);
            FileUtils.moveFile(sourceFile, targetFile);
            index += 1;
        }
        logger.info(" ***** UPLOAD FILES COMPLETED ***** ");
    }

    private List<ChannelSftp.LsEntry> download() throws JSchException, SftpException {
        if (!config.isLocalWrite()) {
            logger.info("Download folder not found at configuration");
            return null;
        }
        final File home = new File(config.get("sftp.home"));
        if (!home.isDirectory()) {
            logger.error("SFTP Home folder not valid : " + home);
            return null;
        }
        logger.info(" ***** DOWNLOAD PROCESS IS STARTED ***** ");
        File localPath = new File(home, config.get("sftp.local.write"));
        if (!localPath.isDirectory()) {
            logger.info("SFTP local write folder not valid : " + localPath);
            return null;
        }
        logger.info("WRITE FILES TO : " + localPath);
        final String remotePath = config.get("sftp.remote.read");
        List<ChannelSftp.LsEntry> entries = sftpService.getFiles(remotePath);
        if (entries == null || entries.isEmpty()) {
            logger.info("No files to download.");
            return null;
        }
        logger.info(" ***** DOWNLOAD FILES FROM REMOTE HOST ***** ");
        int index = 0;
        for (ChannelSftp.LsEntry entry : entries) {
            index += 1;
            File toFile = new File(localPath, entry.getFilename());
            logger.info(index + ", " + remotePath + File.separator + entry.getFilename() + " - DOWNLOAD TO : " + toFile);
            sftpService.download(remotePath, entry.getFilename(), toFile);
        }
        logger.info(" ***** DOWNLOAD FILES COMPLETED ***** ");
        return entries;
    }

    private void onRemoteArchive(List<ChannelSftp.LsEntry> entries) throws JSchException, SftpException {
        if (config.isNotRemoteArchive()) {
            logger.info("Remote files archive folder not found at configuration");
            return;
        }
        logger.info("REMOTE FILES ARCHIVE IS STARTED");
        final String read = config.get("sftp.remote.read");
        final String archive = config.get("sftp.remote.archive");
        logger.info("ARCHIVE FILES TO : " + archive + "/" + read);
        if (entries == null || entries.isEmpty()) {
            logger.info("NO FILES TO ARCHIVE ON REMOTE SERVICE.");
            return;
        }
        logger.info(" ***** ARCHIVE FILES ON REMOTE SERVICE ***** ");
        int index = 0;
        for (ChannelSftp.LsEntry entry : entries) {
            index += 1;
            String writeTo = archive + "/" + read;
            String writeToFile = writeTo + "/" + entry.getFilename();
            logger.info(index + ", " + read + "/" + entry.getFilename() + " - ARCHIVE TO : " + writeToFile);
            sftpService.rename(entry.getFilename(), read, writeTo);
        }
        logger.info(" ***** ARCHIVE REMOTE FILES COMPLETED ***** ");
    }

    private void deleteRemoteArchiveFiles() throws JSchException, SftpException {
        if (config.isNotRemoteArchive()) {
            logger.info("Remote files archive folder not found to delete files!");
            return;
        }
        logger.info("REMOTE ARCHIVE FILES DELETING");
        String read = config.get("sftp.remote.read"), archive = config.get("sftp.remote.archive");
        final String remotePath = archive + "/" + read;
        List<ChannelSftp.LsEntry> entries = sftpService.getFiles(remotePath);
        if (entries == null || entries.isEmpty()) {
            logger.info("No files to delete.");
        } else {
            int index = 0;
            for (ChannelSftp.LsEntry entry : entries) {
                index += 1;
                logger.info(index + ", " + remotePath + "/" + entry.getFilename());
                sftpService.delete(remotePath, entry.getFilename());
            }
        }
        logger.info(" ***** REMOTE ARCHIVE FILES DELETED ***** ");
    }

    private void deleteLocalArchiveFiles() throws IOException {
        if (config.isNotLocalArchive()) {
            logger.info("Local files archive folder not found to delete files!");
            return;
        }
        logger.info("LOCAL ARCHIVE FILES DELETING");
        final File home = new File(config.get("sftp.home"));
        File archive = new File(home, config.get("sftp.local.archive"));
        archive = new File(archive, config.get("sftp.local.read"));
        logger.info("DELETE LOCAL ARCHIVE FILES FROM : " + archive);
        List<File> fileList = (List<File>) FileUtils.listFiles(archive, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        if (fileList.isEmpty()) {
            logger.info("No files to delete!");
        }
        int index = 1;
        for (File sourceFile : fileList) {
            File file = new File(archive, sourceFile.getName());
            logger.info(index + ", " + file);
            FileUtils.forceDelete(file);
            index += 1;
        }
        logger.info(" ***** LOCAL ARCHIVE FILES DELETED ***** ");
    }
}

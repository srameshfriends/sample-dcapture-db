package samooha.sftp.client;

import com.jcraft.jsch.*;

import java.io.File;
import java.util.*;

public class SftpService {
    private final String host, user, pass;
    private final int port;
    private Session session = null;

    public SftpService(SftpConfiguration config) {
        this.host = config.get("sftp.host");
        this.user = config.get("sftp.user");
        this.pass = config.get("sftp.password");
        this.port = config.getPort();
    }

    public void connect() throws JSchException {
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        JSch jsch = new JSch();
        session = jsch.getSession(user, host, port);
        session.setConfig(config);
        session.setPassword(pass);
        session.connect();
    }

    public void disconnect() {
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }

    public void upload(File sourceFile, String path, String remoteFile) throws JSchException, SftpException {
        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftpChannel = (ChannelSftp) channel;
        sftpChannel.cd(path);
        sftpChannel.put(sourceFile.toString(), remoteFile);
        sftpChannel.exit();
    }

    public void download(String remotePath, String remoteFileName, File targetFile) throws JSchException, SftpException {
        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftpChannel = (ChannelSftp) channel;
        sftpChannel.cd(remotePath);
        sftpChannel.get(remoteFileName, targetFile.toString());
        sftpChannel.exit();
    }

    public void rename(String fileName, String fromPath, String toPath) throws JSchException, SftpException {
        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftpChannel = (ChannelSftp) channel;
        sftpChannel.rename(fromPath + "/" + fileName, toPath + "/" + fileName);
        sftpChannel.exit();
    }

    public void delete(String remotePath, String fileName) throws JSchException, SftpException {
        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftpChannel = (ChannelSftp) channel;
        sftpChannel.cd(remotePath);
        sftpChannel.rm(fileName);
        sftpChannel.exit();
    }

    @SuppressWarnings("unchecked")
    public List<ChannelSftp.LsEntry> getFiles(String directory) throws JSchException, SftpException {
        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftpChannel = (ChannelSftp) channel;
        Vector<ChannelSftp.LsEntry> vector = (Vector<ChannelSftp.LsEntry>)sftpChannel.ls(directory);
        if (vector == null || 0 == vector.size()) {
            return new ArrayList<>();
        }
        Iterator<ChannelSftp.LsEntry> iterator = vector.iterator();
        List<ChannelSftp.LsEntry> valueList = new ArrayList<>();
        while (iterator.hasNext()) {
            ChannelSftp.LsEntry entry = iterator.next();
            if (!entry.getAttrs().isDir()) {
                valueList.add(entry);
            }
        }
        sftpChannel.exit();
        return valueList;
    }
}

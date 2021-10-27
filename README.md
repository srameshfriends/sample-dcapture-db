#### SFTP Client Communication

- Java 11 language is used to communicate files via sftp protocol.

- Library
    - JSch is a pure Java implementation of SSH2, jsch-0.1.55 . http://www.jcraft.com/jsch/
        - JSch enables us to use either Password Authentication or Public Key Authentication to access a remote server.
          We'll use password authentication:
    - commons-io-2.11.1 file io operation utility
    - slf4j 2.0.0-alpha5 logging purpose

- The basic process is similar mailbox
    - locale outbox => remote inbox
    - Files has been uploaded to remote server, and locale outbox files archived.
    - remote outbox => locale inbox
    - Files has been downloaded from remote server, then remote outbox files archived.
    - Each process(pid) has separate logging.

- User Home /sftp-mailbox/config.properties

```
     sftp.pid=test-process-name
     sftp.host=192.168.1.102
     sftp.user=admin
     sftp.password=password
     sftp.port=22
     sftp.home=C:\\Users\\admin\\sftp-client\\mailbox
     sftp.local.read=out
     sftp.local.write=in
     sftp.local.archive=archive
     sftp.remote.read=out
     sftp.remote.write=in
     sftp.remote.archive=archive
     sftp.log=C:\\Users\\admin\\sftp-client\\bin\\log
     sftp.operation.mode=DOWNLOAD,DELETE_REMOTE_ARCHIVE,UPLOAD,DELETE_LOCALE_ARCHIVE
```

 - Operation mode will decide to run specific process 
    - DOWNLOAD : Download files from remote sftp server
    - UPLOAD : Upload files from local sftp to remote server
    - DELETE_REMOTE_ARCHIVE : Delete files in remote sftp server archive directory files
    - DELETE_LOCAL_ARCHIVE : Delete files in remote sftp server archive directory files
    
- Application logging under the following directory  
    - User Home / sftp-mailbox/log/sftp-mailbox.log

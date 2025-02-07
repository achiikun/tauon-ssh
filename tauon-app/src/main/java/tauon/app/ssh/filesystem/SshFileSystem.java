package tauon.app.ssh.filesystem;

import net.schmizz.sshj.sftp.*;
import net.schmizz.sshj.sftp.FileMode.Type;
import net.schmizz.sshj.xfer.FilePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.RemoteOperationException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.exceptions.TauonOperationException;
import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.util.misc.PathUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SshFileSystem implements FileSystem {
    private static final Logger LOG = LoggerFactory.getLogger(SshFileSystem.class);
    
    public static final String PROTO_SFTP = "sftp";
    
    private final TauonRemoteSessionInstance ssh;
    private String home;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SshFileSystem(TauonRemoteSessionInstance ssh) {
        this.ssh = ssh;
    }

    interface Operator{
        void operate(SFTPClient sftp) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException, IOException;
    }
    
    interface OperatorReturn<T>{
        T operateReturn(SFTPClient sftp) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException, IOException;
    }
    
    private void getConnectedSftpClient(Operator operator) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        ssh.ensureConnected();
        try{
            operator.operate(ssh.getSftpClient());
        } catch (SFTPException e) {
            if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                throw new RemoteOperationException.PermissionDenied(e);
            }else{
                throw new RemoteOperationException.SFTPException(e);
            }
        } catch (IOException e) {
            throw new RemoteOperationException.RealIOException(e);
        }
    }
    
    private <T> T getConnectedSftpClientReturn(OperatorReturn<T> operatorReturn) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        return getConnectedSftpClientReturn(null, operatorReturn);
    }
    
    private <T> T getConnectedSftpClientReturn(String path, OperatorReturn<T> operatorReturn) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        synchronized (ssh) {
            boolean force = false;
            while (true) {
                ssh.ensureConnected(force);
                try {
                    return operatorReturn.operateReturn(ssh.getSftpClient());
                } catch (SFTPException e) {
                    if (path != null && (
                            e.getStatusCode() == Response.StatusCode.NO_SUCH_FILE
                            || e.getStatusCode() == Response.StatusCode.NO_SUCH_PATH)
                    ) {
                        throw new RemoteOperationException.FileNotFound(e, path);
                    }
                    if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                        throw new RemoteOperationException.PermissionDenied(e);
                    } else {
                        if(!force && e.getCause() instanceof TimeoutException){
                            force = true;
                            // Continue to another iteration forcing connection
                        }else{
                            throw new RemoteOperationException.SFTPException(e);
                        }
                    }
                } catch (IOException e) {
                    throw new RemoteOperationException.RealIOException(e);
                }
            }
        }
    }
    
    private void getConnectedSftpClient(String path, Operator operator) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        synchronized (ssh) {
            boolean force = false;
            while (true) {
                ssh.ensureConnected(force);
                try {
                    operator.operate(ssh.getSftpClient());
                    return;
                } catch (SFTPException e) {
                    if (e.getStatusCode() == Response.StatusCode.NO_SUCH_FILE
                            || e.getStatusCode() == Response.StatusCode.NO_SUCH_PATH) {
                        throw new RemoteOperationException.FileNotFound(e, path);
                    }
                    if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                        throw new RemoteOperationException.PermissionDenied(e);
                    } else {
                        if(!force && e.getCause() instanceof TimeoutException){
                            force = true;
                            // Continue to another iteration forcing connection
                        }else{
                            throw new RemoteOperationException.SFTPException(e);
                        }
                    }
                } catch (IOException e) {
                    throw new RemoteOperationException.RealIOException(e);
                }
            }
        }
    }

    @Override
    public void delete(FileInfo f) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
//        synchronized (this.ssh) {
            getConnectedSftpClientReturn(f.getPath(), sftp -> {
                if (f.getType() == FileType.DIR) {
                    List<FileInfo> list = list(f.getPath());
                    if (list != null && !list.isEmpty()) {
                        for (FileInfo fc : list) {
                            delete(fc);
                        }
                    }
                    sftp.rmdir(f.getPath());
                } else {
                    sftp.rm(f.getPath());
                }
                return null;
            });
//            try {
//                if (f.getType() == FileType.DIR) {
//                    List<FileInfo> list = list(f.getPath());
//                    if (list != null && !list.isEmpty()) {
//                        for (FileInfo fc : list) {
//                            delete(fc);
//                        }
//                    }
//                    sftp.rmdir(f.getPath());
//                } else {
//                    sftp.rm(f.getPath());
//                }
//            } catch (SFTPException e) {
//                if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
//                    throw new RemoteOperationException.PermissionDenied(e);
//                }else{
//                    throw new RemoteOperationException.SFTPException(e);
//                }
//            } catch (IOException e) {
//                throw new RemoteOperationException.RealIOException(e);
//            }
//        }

    }

    @Override
    public void chmod(int perm, String path) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        getConnectedSftpClientReturn(path, sftp -> {
            sftp.chmod(path, perm);
            return null;
        });
//        synchronized (this.ssh) {
//            SFTPClient sftp = getConnectedSftpClient();
//            try {
//                sftp.chmod(path, perm);
//            } catch (SFTPException e) {
//                if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
//                    throw new RemoteOperationException.PermissionDenied(e);
//                }else{
//                    throw new RemoteOperationException.SFTPException(e);
//                }
//            } catch (IOException e) {
//                throw new RemoteOperationException.RealIOException(e);
//            }
//        }
    }

    @Override
    public List<FileInfo> list(String path) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        synchronized (this.ssh) {
            return listFiles(path);
        }
    }

    private FileInfo resolveSymlink(SFTPClient sftp, String name, String pathToResolve, FileAttributes attrs, String longName)
            throws RemoteOperationException {
        try {
            LOG.debug("Following symlink: {}", pathToResolve);
            while (true) {
                String str = sftp.readlink(pathToResolve);
                LOG.debug("Read symlink: {}={}", pathToResolve, str);
                LOG.debug("Getting link attrs: {}", pathToResolve);
                attrs = sftp.stat(pathToResolve);

                if (attrs.getType() != Type.SYMLINK) {
                    return new FileInfo(
                            name,
                            pathToResolve,
                            (attrs.getType() == Type.DIRECTORY ? -1 : attrs.getSize()),
                            attrs.getType() == Type.DIRECTORY ? FileType.DIR_LINK : FileType.FILE_LINK,
                            attrs.getMtime() * 1000,
                            FilePermission.toMask(attrs.getPermissions()),
                            PROTO_SFTP,
                            getPermissionStr(attrs.getPermissions()),
                            attrs.getAtime(),
                            longName,
                            name.startsWith(".")
                    );
                }
            }
        } catch (SFTPException e) {
            if (e.getStatusCode() == Response.StatusCode.NO_SUCH_FILE
                    || e.getStatusCode() == Response.StatusCode.NO_SUCH_PATH
                    || e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                // If fails, return an empty file to represent it
                return new FileInfo(
                        name,
                        pathToResolve,
                        0,
                        FileType.FILE_LINK,
                        attrs.getMtime() * 1000,
                        FilePermission.toMask(attrs.getPermissions()),
                        PROTO_SFTP,
                        getPermissionStr(attrs.getPermissions()),
                        attrs.getAtime(),
                        longName,
                        name.startsWith(".")
                );
            }else{
                throw new RemoteOperationException.SFTPException(e);
            }
        } catch (IOException e) {
            throw new RemoteOperationException.RealIOException(e);
        }

    }

    private List<FileInfo> listFiles(String ppath) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
//        synchronized (this.ssh) {
            return getConnectedSftpClientReturn(ppath, sftp -> {
                String path = ppath;
                LOG.debug("Listing file: {}", path);
                List<FileInfo> childs = new ArrayList<>();
                if (path == null || path.isEmpty()) {
                    path = this.getHome();
                }
                List<RemoteResourceInfoWrapper> files = ls(path);
                for (RemoteResourceInfoWrapper file : files) {
                    RemoteResourceInfo ent = file.getInfo();
                    String longName = file.getLongPath();
                    
                    FileAttributes attrs = ent.getAttributes();
                    
                    if (attrs.getType() == Type.SYMLINK) {
                        try {
                            childs.add(resolveSymlink(sftp, ent.getName(), ent.getPath(), attrs, longName));
                        } catch (RemoteOperationException e) {
                            LOG.error("Exception while resolving symlink. Ignored.", e);
                        }
                    } else {
                        FileInfo e = new FileInfo(ent.getName(), ent.getPath(),
                                (ent.isDirectory() ? -1 : attrs.getSize()),
                                ent.isDirectory() ? FileType.DIR : FileType.FILE, attrs.getMtime() * 1000,
                                FilePermission.toMask(attrs.getPermissions()), PROTO_SFTP,
                                getPermissionStr(attrs.getPermissions()), attrs.getAtime(), longName,
                                ent.getName().startsWith("."));
                        childs.add(e);
                    }
                }
                return childs;
            });
            
    }

    @Override
    public void close() {
        this.closed.set(true);
    }

    @Override
    public String getHome() throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        LOG.debug("Getting home directory... on {}", Thread.currentThread().getName());

        if (home != null) {
            return home;
        }
        return getConnectedSftpClientReturn(sftp -> sftp.canonicalize(""));
//        synchronized (ssh) {
//            System.out.println("Getting home directory");
//            SFTPClient sftp = getConnectedSftpClient();
//            try {
//                this.home = sftp.canonicalize("");
//            } catch (IOException e) {
//                throw new RemoteOperationException.RealIOException(e);
//            }
//            return this.home;
//        }

    }

    @Override
    public String[] getRoots() {
        return new String[]{"/"};
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public FileInfo getInfo(String path) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        return getConnectedSftpClientReturn(path, sftp -> {
            FileAttributes attrs = sftp.stat(path);
            if (attrs.getType() == Type.SYMLINK) {
                return resolveSymlink(sftp, PathUtils.getFileName(path), path, attrs, null);
            } else {
                String name = PathUtils.getFileName(path);
                return new FileInfo(name, path, (attrs.getType() == Type.DIRECTORY ? -1 : attrs.getSize()),
                        attrs.getType() == Type.DIRECTORY ? FileType.DIR : FileType.FILE,
                        attrs.getMtime() * 1000, FilePermission.toMask(attrs.getPermissions()), PROTO_SFTP,
                        getPermissionStr(attrs.getPermissions()), attrs.getAtime(), null, name.startsWith("."));
            }
        });
        
//        synchronized (ssh) {
//            SFTPClient sftp = getConnectedSftpClient();
//            try {
//                FileAttributes attrs = sftp.stat(path);
//                if (attrs.getType() == Type.SYMLINK) {
//                    return resolveSymlink(sftp, PathUtils.getFileName(path), path, attrs, null);
//                } else {
//                    String name = PathUtils.getFileName(path);
//                    return new FileInfo(name, path, (attrs.getType() == Type.DIRECTORY ? -1 : attrs.getSize()),
//                            attrs.getType() == Type.DIRECTORY ? FileType.DIR : FileType.FILE,
//                            attrs.getMtime() * 1000, FilePermission.toMask(attrs.getPermissions()), PROTO_SFTP,
//                            getPermissionStr(attrs.getPermissions()), attrs.getAtime(), null, name.startsWith("."));
//                }
//            } catch (SFTPException e) {
//                if (e.getStatusCode() == Response.StatusCode.NO_SUCH_FILE
//                        || e.getStatusCode() == Response.StatusCode.NO_SUCH_PATH) {
//                    throw new RemoteOperationException.FileNotFound(e, path);
//                }else{
//                    throw new RemoteOperationException.SFTPException(e);
//                }
//            } catch (IOException e) {
//                throw new RemoteOperationException.RealIOException(e);
//            }
//        }
    }

    @Override
    public void createLink(String src, String dst, boolean hardLink) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
         getConnectedSftpClientReturn(src, sftp -> {
             if (hardLink) {
                 throw new RemoteOperationException.NotImplemented("Hardlink is not implemented.");
                 // sftp..hardlink(src, dst);
             } else {
                 sftp.symlink(src, dst);
             }
             
             return null;
        });
        
//        synchronized (ssh) {
//            SFTPClient sftp = getConnectedSftpClient();
//            try {
//                if (hardLink) {
//                    throw new RemoteOperationException.NotImplemented("Hardlink is not implemented.");
//                    // sftp..hardlink(src, dst);
//                } else {
//                    sftp.symlink(src, dst);
//                }
//            } catch (SFTPException e) {
//                if (e.getStatusCode() == Response.StatusCode.NO_SUCH_FILE
//                        || e.getStatusCode() == Response.StatusCode.NO_SUCH_PATH) {
//                    throw new RemoteOperationException.FileNotFound(e, src);
//                }else{
//                    throw new RemoteOperationException.SFTPException(e);
//                }
//            } catch (IOException e) {
//                throw new RemoteOperationException.RealIOException(e);
//            }
//        }
    }

    @Override
    public void deleteFile(String f, boolean throwIfFileDoesNotExist) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        getConnectedSftpClient(f, sftp -> {
            try{
                sftp.rm(f);
            }catch (SFTPException e) {
                if (throwIfFileDoesNotExist || (
                        e.getStatusCode() != Response.StatusCode.NO_SUCH_FILE
                        && e.getStatusCode() != Response.StatusCode.NO_SUCH_PATH
                )) {
                    throw e;
                }
            }
        });
//        synchronized (ssh) {
//            SFTPClient sftp = getConnectedSftpClient();
//            sftp.rm(f);
//        }
    }

    @Override
    public void createFile(String path) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        getConnectedSftpClientReturn(path, sftp -> {
            sftp.open(path, EnumSet.of(OpenMode.APPEND, OpenMode.CREAT)).close();
            return null;
        });
//        synchronized (ssh) {
//            SFTPClient sftp = getConnectedSftpClient();
//            try {
//                sftp.open(path, EnumSet.of(OpenMode.APPEND, OpenMode.CREAT)).close();
//            } catch (SFTPException e) {
//                if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
//                    throw new AccessDeniedException(path);
//                }
//            }
////            catch (Exception e) {
////                if (ssh.isConnected()) {
////                    throw new FileNotFoundException(e.getMessage());
////                }
////                throw new Exception(e);
////            }
//        }
    }

    @Override
    public void rename(String oldName, String newName) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        getConnectedSftpClientReturn(oldName, sftp -> {
            sftp.rename(oldName, newName);
            return null;
        });
//        synchronized (ssh) {
//            try {
//                SFTPClient sftp = getConnectedSftpClient();
//                sftp.rename(oldName, newName);
//            } catch (SFTPException e) {
//                if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
//                    throw new AccessDeniedException(oldName);
//                }
//            }
////            catch (Exception e) {
////                if (ssh.isConnected()) {
////                    throw new FileNotFoundException(e.getMessage());
////                }
////                throw new Exception(e);
////            }
//
//        }
    }

    @Override
    public void mkdir(String path) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException{
        getConnectedSftpClientReturn(path, sftp -> {
            sftp.mkdir(path);
            return null;
        });
//        synchronized (ssh) {
//            SFTPClient sftp = getConnectedSftpClient();
//            try {
//                sftp.mkdir(path);
//            } catch (SFTPException e) {
//                if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
//                    throw new AccessDeniedException(path);
//                }
//            }
////            catch (Exception e) {
////                if (ssh.isConnected()) {
////                    throw new FileNotFoundException(e.getMessage());
////                }
////                throw new Exception(e);
////            }
//        }
    }

    @Override
    public boolean mkdirs(String absPath) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException{
        return getConnectedSftpClientReturn(absPath, sftp -> {
            if (absPath.equals("/")) {
                return true;
            }
            
            try {
                // If stat crashes, the file does not exist
                sftp.stat(absPath);
                return false;
            }
            catch (Exception e) {
//                if (!ssh.isConnected()) {
//                    throw e;
//                }
            }
            
            String parent = PathUtils.getParent(absPath);
            
            mkdirs(parent);
            sftp.mkdir(absPath);
            
            return true;
        });
        
//        synchronized (ssh) {
//            SFTPClient sftp = getConnectedSftpClient();
//            System.out.println("mkdirs: " + absPath);
//            if (absPath.equals("/")) {
//                return true;
//            }
//
//            try {
//                // If stat crashes, the file does not exist
//                sftp.stat(absPath);
//                return false;
//            }
//            catch (Exception e) {
////                if (!ssh.isConnected()) {
////                    throw e;
////                }
//            }
//
//            System.out.println("Folder does not exists: " + absPath);
//
//            String parent = PathUtils.getParent(absPath);
//
//            mkdirs(parent);
//            sftp.mkdir(absPath);
//
//            return true;
//        }

    }

    @Override
    public long getAllFiles(String dir, String baseDir, Map<String, String> fileMap, Map<String, String> folderMap)
            throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        return getConnectedSftpClientReturn(dir, sftp -> {
            long size = 0;
            String parentFolder = PathUtils.combine(baseDir, PathUtils.getFileName(dir), File.separator);
            
            folderMap.put(dir, parentFolder);
            
            List<FileInfo> list = list(dir);
            for (FileInfo f : list) {
                if (f.getType() == FileType.DIR) {
                    folderMap.put(f.getPath(), PathUtils.combine(parentFolder, f.getName(), File.separator));
                    size += getAllFiles(f.getPath(), parentFolder, fileMap, folderMap);
                } else {
                    fileMap.put(f.getPath(), PathUtils.combine(parentFolder, f.getName(), File.separator));
                    size += f.getSize();
                }
            }
            return size;
            
        });
//            synchronized (ssh) {
//            SFTPClient sftp = getConnectedSftpClient();
//
//        }

    }

    @Override
    public boolean isConnected() {
        return !closed.get() && ssh.isConnected();
    }

    @Override
    public String getProtocol() {
        return PROTO_SFTP;
    }


    public InputTransferChannel inputTransferChannel() throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        return getConnectedSftpClientReturn(sftp -> new InputTransferChannel() {
            @Override
            public InputStream getInputStream(String path) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
                return getConnectedSftpClientReturn(path, sftp1 -> {
                    RemoteFile remoteFile = sftp.open(path, EnumSet.of(OpenMode.READ));
                    return new SSHRemoteFileInputStream(remoteFile,
                            sftp.getSFTPEngine().getSubsystem().getLocalMaxPacketSize());
                });
            }
            
            @Override
            public String getSeparator() {
                return "/";
            }
            
            @Override
            public long getSize(String path) throws Exception {
                return getInfo(path).getSize();
            }
        });
        
//        synchronized (ssh) {
//            SFTPClient sftp = getConnectedSftpClient();
//            try {
//                return new InputTransferChannel() {
//                    @Override
//                    public InputStream getInputStream(String path) throws Exception {
//                        RemoteFile remoteFile = sftp.open(path, EnumSet.of(OpenMode.READ));
//                        return new SSHRemoteFileInputStream(remoteFile,
//                                sftp.getSFTPEngine().getSubsystem().getLocalMaxPacketSize());
//                    }
//
//                    @Override
//                    public String getSeparator() {
//                        return "/";
//                    }
//
//                    @Override
//                    public long getSize(String path) throws Exception {
//                        return getInfo(path).getSize();
//                    }
//                };
//            } catch (Exception e) {
//                if (ssh.isConnected()) {
//                    throw new FileNotFoundException();
//                }
//                throw new Exception();
//            }
//        }
    }

    public OutputTransferChannel outputTransferChannel() throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        return getConnectedSftpClientReturn(sftp -> new OutputTransferChannel() {
            @Override
            public OutputStream getOutputStream(String path) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
                return getConnectedSftpClientReturn(path, sftp1 -> {
                    RemoteFile remoteFile = sftp1.open(path, EnumSet.of(OpenMode.WRITE, OpenMode.TRUNC, OpenMode.CREAT));
                    return new SSHRemoteFileOutputStream(remoteFile, sftp.getSFTPEngine().getSubsystem().getRemoteMaxPacketSize());
                });
                
//                try {
//                    RemoteFile remoteFile = sftp.open(path,
//                            EnumSet.of(OpenMode.WRITE, OpenMode.TRUNC, OpenMode.CREAT));
//                    return new SSHRemoteFileOutputStream(remoteFile,
//                            sftp.getSFTPEngine().getSubsystem().getRemoteMaxPacketSize());
//                } catch (SFTPException e) {
//                    if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
//                        throw new AccessDeniedException(e.getMessage());
//                    }
//                    throw e;
//                }
            }
            
            @Override
            public String getSeparator() {
                return "/";
            }
        });
        
//        System.out.println("Create OutputTransferChannel");
//        synchronized (ssh) {
//            SFTPClient sftp = getConnectedSftpClient();
//            try {
//                return new OutputTransferChannel() {
//                    @Override
//                    public OutputStream getOutputStream(String path) throws Exception {
//                        try {
//                            RemoteFile remoteFile = sftp.open(path,
//                                    EnumSet.of(OpenMode.WRITE, OpenMode.TRUNC, OpenMode.CREAT));
//                            return new SSHRemoteFileOutputStream(remoteFile,
//                                    sftp.getSFTPEngine().getSubsystem().getRemoteMaxPacketSize());
//                        } catch (SFTPException e) {
//                            if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
//                                throw new AccessDeniedException(e.getMessage());
//                            }
//                            throw e;
//                        }
//                    }
//
//                    @Override
//                    public String getSeparator() {
//                        return "/";
//                    }
//                };
//            } catch (Exception e) {
//                if (ssh.isConnected()) {
//                    throw new FileNotFoundException();
//                }
//                throw new Exception();
//            }
//        }

    }

    public String getSeparator() {
        return "/";
    }

    public void statFs() throws Exception {
    }

    private List<RemoteResourceInfoWrapper> ls(String path) throws RemoteOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        return getConnectedSftpClientReturn(path, sftp -> {
            final SFTPEngine requester = sftp.getSFTPEngine();
            final byte[] handle = requester
                    .request(requester.newRequest(PacketType.OPENDIR).putString(path,
                            requester.getSubsystem().getRemoteCharset()))
                    .retrieve(requester.getTimeoutMs(), TimeUnit.MILLISECONDS).ensurePacketTypeIs(PacketType.HANDLE)
                    .readBytes();
            try (ExtendedRemoteDirectory dir = new ExtendedRemoteDirectory(requester, path, handle)) {
                return dir.scanExtended(null);
            }
        });
        
    }

    private String getPermissionStr(Set<FilePermission> perms) {
        char[] arr = {'-', '-', '-', '-', '-', '-', '-', '-', '-'};
        if (perms.contains(FilePermission.USR_R)) {
            arr[0] = 'r';
        }
        if (perms.contains(FilePermission.USR_W)) {
            arr[1] = 'w';
        }
        if (perms.contains(FilePermission.USR_X)) {
            arr[2] = 'x';
        }

        if (perms.contains(FilePermission.GRP_R)) {
            arr[3] = 'r';
        }
        if (perms.contains(FilePermission.GRP_W)) {
            arr[4] = 'w';
        }
        if (perms.contains(FilePermission.GRP_X)) {
            arr[5] = 'x';
        }

        if (perms.contains(FilePermission.OTH_R)) {
            arr[6] = 'r';
        }
        if (perms.contains(FilePermission.OTH_W)) {
            arr[7] = 'w';
        }
        if (perms.contains(FilePermission.OTH_W)) {
            arr[8] = 'x';
        }
        return new String(arr);
    }
}

package tauon.app.ssh.filesystem;

import net.schmizz.sshj.sftp.*;
import net.schmizz.sshj.sftp.FileMode.Type;
import net.schmizz.sshj.xfer.FilePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ui.containers.session.pages.files.ssh.SshFileOperations;
import tauon.app.util.misc.PathUtils;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    private SFTPClient getConnectedSftpClient() throws IOException, OperationCancelledException, InterruptedException, SessionClosedException {
        ssh.ensureConnected();
        return ssh.getSftpClient();
    }

    @Override
    public void delete(FileInfo f) throws IOException, OperationCancelledException, InterruptedException, SessionClosedException {
        synchronized (this.ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            try {
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
            } catch (SFTPException e) {
                if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                    throw new AccessDeniedException("Access is denied");
                }
            }
        }

    }

    @Override
    public void chmod(int perm, String path) throws Exception {
        synchronized (this.ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            try {
                sftp.chmod(path, perm);
            } catch (SFTPException e) {
                if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                    throw new AccessDeniedException("Access is denied");
                }
                throw e;
            }
        }
    }

    @Override
    public List<FileInfo> list(String path) throws IOException, OperationCancelledException, InterruptedException, SessionClosedException {
        synchronized (this.ssh) {
            return listFiles(path);
        }
    }

    private FileInfo resolveSymlink(SFTPClient sftp, String name, String pathToResolve, FileAttributes attrs, String longName)
            throws Exception {
        try {
            System.out.println("Following symlink: " + pathToResolve);
            while (true) {
                String str = sftp.readlink(pathToResolve);
                System.out.println("Read symlink: " + pathToResolve + "=" + str);
                System.out.println("Getting link attrs: " + pathToResolve);
                attrs = sftp.stat(pathToResolve);

                if (attrs.getType() != Type.SYMLINK) {
                    return new FileInfo(name, pathToResolve,
                            (attrs.getType() == Type.DIRECTORY ? -1 : attrs.getSize()),
                            attrs.getType() == Type.DIRECTORY ? FileType.DIR_LINK : FileType.FILE_LINK,
                            attrs.getMtime() * 1000, FilePermission.toMask(attrs.getPermissions()), PROTO_SFTP,
                            getPermissionStr(attrs.getPermissions()), attrs.getAtime(), longName, name.startsWith("."));
                }
            }
        } catch (SFTPException e) {
            if (e.getStatusCode() == Response.StatusCode.NO_SUCH_FILE
                    || e.getStatusCode() == Response.StatusCode.NO_SUCH_PATH
                    || e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                return new FileInfo(name, pathToResolve, 0, FileType.FILE_LINK, attrs.getMtime() * 1000,
                        FilePermission.toMask(attrs.getPermissions()), PROTO_SFTP,
                        getPermissionStr(attrs.getPermissions()), attrs.getAtime(), longName, name.startsWith("."));
            }
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    private List<FileInfo> listFiles(String path) throws IOException, OperationCancelledException, InterruptedException, SessionClosedException {
        synchronized (this.ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            System.out.println("Listing file: " + path);
            List<FileInfo> childs = new ArrayList<>();
            try {
                if (path == null || path.isEmpty()) {
                    path = this.getHome();
                }
                List<RemoteResourceInfoWrapper> files = ls(path);
                if (!files.isEmpty()) {
                    for (int i = 0; i < files.size(); i++) {
                        RemoteResourceInfo ent = files.get(i).getInfo();
                        String longName = files.get(i).getLongPath();

                        FileAttributes attrs = ent.getAttributes();

                        if (attrs.getType() == Type.SYMLINK) {
                            try {
                                childs.add(resolveSymlink(sftp, ent.getName(), ent.getPath(), attrs, longName));
                            } catch (Exception e) {
                                e.printStackTrace();
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
                }
            } catch (SFTPException e) {
                if (e.getStatusCode() == Response.StatusCode.NO_SUCH_FILE
                        || e.getStatusCode() == Response.StatusCode.NO_SUCH_PATH) {
                    throw new FileNotFoundException(path);
                }
                if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                    throw new AccessDeniedException(path);
                }
                throw new IOException(e);
            } catch (Exception e) {
                throw new IOException(e);
            }
            return childs;
        }
    }

    @Override
    public void close() {
        this.closed.set(true);
    }

    @Override
    public String getHome() throws IOException, OperationCancelledException, InterruptedException, SessionClosedException {
        System.out.println("Getting home directory... on " + Thread.currentThread().getName());
        if (home != null) {
            return home;
        }

        synchronized (ssh) {
            System.out.println("Getting home directory");
            SFTPClient sftp = getConnectedSftpClient();
            this.home = sftp.canonicalize("");
            return this.home;
        }

    }

    @Override
    public String[] getRoots() throws Exception {
        return new String[]{"/"};
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public FileInfo getInfo(String path) throws Exception {
        synchronized (ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            try {
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
            } catch (SFTPException e) {
                if (e.getStatusCode() == Response.StatusCode.NO_SUCH_FILE
                        || e.getStatusCode() == Response.StatusCode.NO_SUCH_PATH) {
                    throw new FileNotFoundException(path);
                }
                throw e;
            }
        }
    }

    @Override
    public void createLink(String src, String dst, boolean hardLink) throws Exception {
        synchronized (ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            if (hardLink) {
                throw new IOException("Not implemented");
                // sftp..hardlink(src, dst);
            } else {
                sftp.symlink(src, dst);
            }
        }
    }

    @Override
    public void deleteFile(String f) throws Exception {
        synchronized (ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            sftp.rm(f);
        }
    }

    @Override
    public void createFile(String path) throws Exception {
        synchronized (ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            try {
                sftp.open(path, EnumSet.of(OpenMode.APPEND, OpenMode.CREAT)).close();
            } catch (SFTPException e) {
                if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                    throw new AccessDeniedException(path);
                }
            }
//            catch (Exception e) {
//                if (ssh.isConnected()) {
//                    throw new FileNotFoundException(e.getMessage());
//                }
//                throw new Exception(e);
//            }
        }
    }

    @Override
    public void rename(String oldName, String newName) throws Exception {
        synchronized (ssh) {
            try {
                SFTPClient sftp = getConnectedSftpClient();
                sftp.rename(oldName, newName);
            } catch (SFTPException e) {
                if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                    throw new AccessDeniedException(oldName);
                }
            }
//            catch (Exception e) {
//                if (ssh.isConnected()) {
//                    throw new FileNotFoundException(e.getMessage());
//                }
//                throw new Exception(e);
//            }

        }
    }

    @Override
    public void mkdir(String path) throws Exception {
        synchronized (ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            try {
                sftp.mkdir(path);
            } catch (SFTPException e) {
                if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                    throw new AccessDeniedException(path);
                }
            }
//            catch (Exception e) {
//                if (ssh.isConnected()) {
//                    throw new FileNotFoundException(e.getMessage());
//                }
//                throw new Exception(e);
//            }
        }
    }

    @Override
    public boolean mkdirs(String absPath) throws Exception {
        synchronized (ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            System.out.println("mkdirs: " + absPath);
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

            System.out.println("Folder does not exists: " + absPath);

            String parent = PathUtils.getParent(absPath);

            mkdirs(parent);
            sftp.mkdir(absPath);

            return true;
        }

    }

    @Override
    public long getAllFiles(String dir, String baseDir, Map<String, String> fileMap, Map<String, String> folderMap)
            throws Exception {
        synchronized (ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            long size = 0;
            System.out.println("get files: " + dir);
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
        }

    }

    @Override
    public boolean isConnected() {
        return !closed.get() && ssh.isConnected();
    }

    @Override
    public String getProtocol() {
        return PROTO_SFTP;
    }


    public InputTransferChannel inputTransferChannel() throws Exception {
        synchronized (ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            try {
                return new InputTransferChannel() {
                    @Override
                    public InputStream getInputStream(String path) throws Exception {
                        RemoteFile remoteFile = sftp.open(path, EnumSet.of(OpenMode.READ));
                        return new SSHRemoteFileInputStream(remoteFile,
                                sftp.getSFTPEngine().getSubsystem().getLocalMaxPacketSize());
                    }

                    @Override
                    public String getSeparator() {
                        return "/";
                    }

                    @Override
                    public long getSize(String path) throws Exception {
                        return getInfo(path).getSize();
                    }
                };
            } catch (Exception e) {
                if (ssh.isConnected()) {
                    throw new FileNotFoundException();
                }
                throw new Exception();
            }
        }
    }

    public OutputTransferChannel outputTransferChannel() throws Exception {
        System.out.println("Create OutputTransferChannel");
        synchronized (ssh) {
            SFTPClient sftp = getConnectedSftpClient();
            try {
                return new OutputTransferChannel() {
                    @Override
                    public OutputStream getOutputStream(String path) throws Exception {
                        try {
                            RemoteFile remoteFile = sftp.open(path,
                                    EnumSet.of(OpenMode.WRITE, OpenMode.TRUNC, OpenMode.CREAT));
                            return new SSHRemoteFileOutputStream(remoteFile,
                                    sftp.getSFTPEngine().getSubsystem().getRemoteMaxPacketSize());
                        } catch (SFTPException e) {
                            if (e.getStatusCode() == Response.StatusCode.PERMISSION_DENIED) {
                                throw new AccessDeniedException(e.getMessage());
                            }
                            throw e;
                        }
                    }

                    @Override
                    public String getSeparator() {
                        return "/";
                    }
                };
            } catch (Exception e) {
                if (ssh.isConnected()) {
                    throw new FileNotFoundException();
                }
                throw new Exception();
            }
        }

    }

    public String getSeparator() {
        return "/";
    }

    public void statFs() throws Exception {
    }

    private List<RemoteResourceInfoWrapper> ls(String path) throws Exception {
        final SFTPEngine requester = getConnectedSftpClient().getSFTPEngine();
        final byte[] handle = requester
                .request(requester.newRequest(PacketType.OPENDIR).putString(path,
                        requester.getSubsystem().getRemoteCharset()))
                .retrieve(requester.getTimeoutMs(), TimeUnit.MILLISECONDS).ensurePacketTypeIs(PacketType.HANDLE)
                .readBytes();
        try (ExtendedRemoteDirectory dir = new ExtendedRemoteDirectory(requester, path, handle)) {
            return dir.scanExtended(null);
        }
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

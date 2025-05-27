package tauon.app.ssh.filesystem;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.LocalOperationException;
import tauon.app.exceptions.TauonOperationException;
import tauon.app.util.misc.PathUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocalFileSystem implements FileSystem {
    private static final Logger LOG = LoggerFactory.getLogger(LocalFileSystem.class);
    
    public static final String PROTO_LOCAL_FILE = "local";
    
    private static final LocalFileSystem instance = new LocalFileSystem();
    
    public static LocalFileSystem getInstance(){
        return instance;
    }
    
    private LocalFileSystem(){
    
    }

    @Override
    public void chmod(int perm, String path) {
    }

    @Override
    public FileInfo getInfo(String path) throws LocalOperationException {
        File f = new File(path);
        if (!f.exists()) {
            throw new LocalOperationException.FileNotFound(path);
        }
        
        return getFileInfo(f);
    }
    
    @NotNull
    private static FileInfo getFileInfo(File f) throws LocalOperationException {
        Path p = f.toPath();
        
        // TODO follow links to get parameters of the real file (note that the real file may not exist)
        BasicFileAttributes attrs = null;
        try {
            attrs = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new LocalOperationException.RealIOException(e);
        }
        
        FileType type = Files.isSymbolicLink(p) ?
                (f.isDirectory() ? FileType.DIR_LINK : FileType.FILE_LINK) :
                (f.isDirectory() ? FileType.DIR : FileType.FILE);
        
        return new FileInfo(f.getName(), f.getAbsolutePath(), f.length(),
                type, f.lastModified(), -1, PROTO_LOCAL_FILE,
                "", attrs.creationTime().toMillis(), "", f.isHidden());
    }
    
    @Override
    public String getHome() {
        return System.getProperty("user.home");
    }

    @Override
    public List<FileInfo> list(String path) throws LocalOperationException {
        if (path == null || path.length() < 1) {
            path = System.getProperty("user.home");
        }
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        File[] childs = new File(path).listFiles();
        List<FileInfo> list = new ArrayList<>();
        if (childs == null || childs.length < 1) {
            return list;
        }
        for (File f : childs) {
            list.add(getFileInfo(f));
        }
        return list;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    public InputStream getInputStream(String file, long offset) throws LocalOperationException {
        FileInputStream fout = null;
        try {
            fout = new FileInputStream(file);
            fout.skip(offset);
        } catch (IOException e) {
            throw new LocalOperationException.RealIOException(e);
        }
        return fout;
    }

    public OutputStream getOutputStream(String file) throws LocalOperationException {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new LocalOperationException.RealIOException(e);
        }
    }

    @Override
    public void rename(String oldName, String newName) throws LocalOperationException {
//        System.out.println("Renaming from " + oldName + " to: " + newName);
        if (!new File(oldName).renameTo(new File(newName))) {
            throw new LocalOperationException.FileNotFound(null, oldName);
        }
    }

    public synchronized void delete(FileInfo f) throws LocalOperationException {
        if (f.getType() == FileType.DIR) {
            List<FileInfo> list = list(f.getPath());
            if (list != null && list.size() > 0) {
                for (FileInfo fc : list) {
                    delete(fc);
                }
            }
            new File(f.getPath()).delete();
        } else {
            new File(f.getPath()).delete();
        }
    }

    @Override
    public void mkdir(String path) {
        System.out.println("Creating folder: " + path);
        new File(path).mkdirs();
    }
    
    @Override
    public boolean mkdirs(String absPath) {
        return new File(absPath).mkdirs();
    }

    @Override
    public long getAllFiles(String dir, String baseDir, Map<String, String> fileMap, Map<String, String> folderMap)
            throws Exception {
        long size = 0;
        System.out.println("get files: " + dir);
        String parentFolder = PathUtils.combineUnix(baseDir, PathUtils.getFileName(dir));

        folderMap.put(dir, parentFolder);

        List<FileInfo> list = list(dir);
        for (FileInfo f : list) {
            if (f.getType() == FileType.DIR) {
                folderMap.put(f.getPath(), PathUtils.combineUnix(parentFolder, f.getName()));
                size += getAllFiles(f.getPath(), parentFolder, fileMap, folderMap);
            } else {
                fileMap.put(f.getPath(), PathUtils.combineUnix(parentFolder, f.getName()));
                size += f.getSize();
            }
        }
        return size;
    }

    /*
     * (non-Javadoc)
     *
     * @see nixexplorer.core.FileSystemProvider#deleteFile(java.lang.String)
     */
    @Override
    public void deleteFile(String f, boolean throwIfFileDoesNotExist) throws LocalOperationException {
        try {
            if(throwIfFileDoesNotExist){
                    Files.deleteIfExists(new File(f).toPath());
            }else{
                Files.delete(new File(f).toPath());
            }
        } catch (IOException e) {
            throw new LocalOperationException.RealIOException(e);
        }
    }

    @Override
    public String getProtocol() {
        return PROTO_LOCAL_FILE;
    }

    /*
     * (non-Javadoc)
     *
     * @see nixexplorer.core.FileSystemProvider#createFile(java.lang.String)
     */
    @Override
    public void createFile(String path) throws LocalOperationException {
        try {
            Files.createFile(Paths.get(path));
        } catch (IOException e) {
            throw new LocalOperationException.RealIOException(e);
        }
    }

    public void createLink(String src, String dst, boolean hardLink) {
        // TODO
    }

    @Override
    public String getName() {
        return "Local files";
    }

    @Override
    public String[] getRoots() {
        File[] roots = File.listRoots();
        String[] arr = new String[roots.length];
        int i = 0;
        for (File f : roots) {
            arr[i++] = f.getAbsolutePath();
        }
        return arr;
    }

    public InputTransferChannel inputTransferChannel() {
        return new InputTransferChannel() {
            @Override
            public InputStream getInputStream(String path) throws TauonOperationException {
                try {
                    return new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    throw new LocalOperationException.FileNotFound(e, path);
                }
            }

            @Override
            public String getSeparator() {
                return File.separator;
            }

            @Override
            public long getSize(String path) throws Exception {
                return getInfo(path).getSize();
            }

        };
    }

    public OutputTransferChannel outputTransferChannel() {
        return new OutputTransferChannel() {
            @Override
            public OutputStream getOutputStream(String path) throws LocalOperationException.FileNotFound {
                try {
                    return new FileOutputStream(path);
                } catch (FileNotFoundException e) {
                    throw new LocalOperationException.FileNotFound(e, path);
                }
            }

            @Override
            public String getSeparator() {
                return File.separator;
            }
        };
    }

    public String getSeparator() {
        return File.separator;
    }
}

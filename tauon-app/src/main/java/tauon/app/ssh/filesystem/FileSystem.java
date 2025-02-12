package tauon.app.ssh.filesystem;

import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.exceptions.TauonOperationException;

import java.util.List;
import java.util.Map;

public interface FileSystem {// extends AutoCloseable {

    FileInfo getInfo(String path) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException ;

    List<FileInfo> list(String path) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException;

    String getHome() throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException;

    boolean isLocal();

    default boolean isRemote(){
        return !isLocal();
    }
    
    void rename(String oldName, String newName)
            throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException;

    void delete(FileInfo f) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException;

    void deleteFile(String f, boolean throwIfFileDoesNotExist) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException;

    void mkdir(String path) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException;

//    void close() throws TauonOperationException;
    
    void chmod(int perm, String path) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException ;

    boolean mkdirs(String absPath) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException;

    long getAllFiles(String dir, String baseDir,
                     Map<String, String> fileMap, Map<String, String> folderMap)
            throws Exception;

    String getProtocol();

    void createFile(String path) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException;

    String[] getRoots() throws TauonOperationException;

    void createLink(String src, String dst, boolean hardLink)
            throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException;
    
    InputTransferChannel inputTransferChannel() throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException ;

    OutputTransferChannel outputTransferChannel() throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException ;

    String getSeparator();
    
    default String getName(){
        return ""; // TODO
    }
}

package tauon.app.ssh.filesystem.transfer;

import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.exceptions.TauonOperationException;
import tauon.app.ssh.GuiHandle;
import tauon.app.ssh.filesystem.*;
import tauon.app.ui.containers.main.FileTransferProgress;
import tauon.app.util.misc.Constants;
import tauon.app.util.misc.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tauon.app.services.LanguageService.getBundle;
import static tauon.app.util.misc.FormatUtils.$$;


public abstract class FileTransfer implements Runnable{
    private static final int DEFAULT_BUF_SIZE = Short.MAX_VALUE;
    
    private final GuiHandle guiHandle;
    private final FileInfo[] files;
    private final String targetFolder;
    private final Constants.ConflictAction defaultConflictAction;
    
    protected FileTransferProgress progressListener;
    
    protected List<FileInfoHolder> filesToTransfer;
    protected long totalSize;
    
    private int processedFiles = 0;
    private long processedBytes = 0;
    
    public FileTransfer(GuiHandle guiHandle, FileInfo[] files, String targetFolder, Constants.ConflictAction defaultConflictAction){
        this.guiHandle = guiHandle;
        this.defaultConflictAction = defaultConflictAction;
        this.files = files;
        this.targetFolder = targetFolder;
    }
    
    public void setProgressListener(FileTransferProgress progress) {
        this.progressListener = progress;
    }
    
    protected void incrementProcessedFiles(){
        processedFiles++;
        if(progressListener != null)
            progressListener.progress(processedBytes, totalSize, processedFiles, filesToTransfer.size());
    }
    
    protected void incrementProcessedBytes(int amount){
        processedBytes += amount;
        if(progressListener != null)
            progressListener.progress(processedBytes, totalSize, processedFiles, filesToTransfer.size());
    }
    
    /**
     * TODO set this process cancellable
     * @param sourceFs
     * @param targetFs
     * @throws OperationCancelledException
     * @throws TauonOperationException
     * @throws InterruptedException
     * @throws SessionClosedException
     * @throws AlreadyFailedException
     */
    protected void prepareTransfer(FileSystem sourceFs, FileSystem targetFs) throws OperationCancelledException, TauonOperationException, InterruptedException, SessionClosedException, AlreadyFailedException {
        
        List<FileInfoDeduplicate> fileInfoDeduplicates = new ArrayList<>();
        TargetHandler targetHandler = new TargetHandler(targetFs.list(targetFolder));
        
        int duplicatedCount = 0;
        for (FileInfo file: files){
            boolean duplicated = targetHandler.exists(file.getName());
            if(duplicated) duplicatedCount++;
            fileInfoDeduplicates.add(new FileInfoDeduplicate(file, targetFolder, duplicated));
        }
        
        if(duplicatedCount > 0){
            switch (defaultConflictAction){
                case OVERWRITE:
                    if(!guiHandle.promptConfirmation(
                            getBundle().getString("app.files.transfer.dialog.duplicated.title"),
                            duplicatedCount == 1
                                    ? getBundle().getString("app.files.transfer.dialog.duplicated.message.overwrite_1")
                                    : $$(getBundle().getString("app.files.transfer.dialog.duplicated.message.overwrite_n"),
                                    Map.of("FILES", duplicatedCount))
                    )){
                        throw new OperationCancelledException();
                    }
                    
                    break;
                case AUTORENAME:
                    if(!guiHandle.promptConfirmation(
                            getBundle().getString("app.files.transfer.dialog.duplicated.title"),
                            duplicatedCount == 1
                                    ? getBundle().getString("app.files.transfer.dialog.duplicated.message.autorename_1")
                                    : $$(getBundle().getString("app.files.transfer.dialog.duplicated.message.autorename_n"),
                                    Map.of("FILES", duplicatedCount))
                    )){
                        throw new OperationCancelledException();
                    }
                    
                    for(FileInfoDeduplicate f: fileInfoDeduplicates){
                        if(f.duplicated) {
                            targetHandler.proposeNewNameFor(f, fileInfoDeduplicates);
                        }
                    }
                    
                    break;
                case SKIP:
                    if(!guiHandle.promptConfirmation(
                            getBundle().getString("app.files.transfer.dialog.duplicated.title"),
                            duplicatedCount == 1
                                    ? getBundle().getString("app.files.transfer.dialog.duplicated.message.skip_1")
                                    : $$(getBundle().getString("app.files.transfer.dialog.duplicated.message.skip_n"),
                                    Map.of("FILES", duplicatedCount))
                    )){
                        throw new OperationCancelledException();
                    }
                    
                    fileInfoDeduplicates.removeIf(f -> f.duplicated);
                    
                    break;
                case PROMPT:
                    break;
                case CANCEL:
                    guiHandle.showMessage(
                            getBundle().getString("app.files.transfer.dialog.canceled.title"),
                            getBundle().getString("app.files.transfer.dialog.canceled.message")
                    );
                    throw new AlreadyFailedException();
            }
        }
        
        List<FileInfoHolder> filesToTransfer = new ArrayList<>();
        totalSize = 0;
        
        MkDirTree mkDirTree = new MkDirTree(targetFolder, null);
        mkDirTree.processed = true; // Root is created
        for(FileInfoDeduplicate file: fileInfoDeduplicates){
            // TODO ask user for dir links, please
            if (file.info.getType() == FileType.DIR || file.info.getType() == FileType.DIR_LINK) {
                addAllFilesInFolder(sourceFs, file.info, file.proposedName, targetFolder, mkDirTree, filesToTransfer);
            } else {
                filesToTransfer.add(new FileInfoHolder(file, mkDirTree));
                totalSize += file.info.getSize();
            }
            
        }
        
        this.filesToTransfer = filesToTransfer;
        
    }
    
    private void addAllFilesInFolder(FileSystem sourceFs, FileInfo sourceFolder, String proposedName, String targetFolder, MkDirTree parent, List<FileInfoHolder> filesToTransfer) throws OperationCancelledException, TauonOperationException, InterruptedException, SessionClosedException {
        String folderTarget = PathUtils.combineUnix(targetFolder, proposedName == null ? sourceFolder.getName() : proposedName);
        MkDirTree mkDirTree = new MkDirTree(folderTarget, parent);
        List<FileInfo> list = sourceFs.list(sourceFolder.getPath());
        for (FileInfo file : list) {
            if (file.getType() == FileType.DIR) {
                addAllFilesInFolder(sourceFs, file, null, folderTarget, mkDirTree, filesToTransfer);
            } else if (file.getType() == FileType.FILE) {
                filesToTransfer.add(new FileInfoHolder(file, mkDirTree));
                totalSize += file.getSize();
            }
        }
    }
    
    private void processMkDirs(MkDirTree mkDirTree, FileSystem targetFs) throws OperationCancelledException, TauonOperationException, InterruptedException, SessionClosedException {
        if(mkDirTree.parent != null && !mkDirTree.parent.processed)
            processMkDirs(mkDirTree.parent, targetFs);
        targetFs.mkdir(mkDirTree.path);
        mkDirTree.processed = true;
    }
    
    protected synchronized void copyFile(
            FileInfoHolder sourceFile,
            FileSystem targetFs,
            InputTransferChannel sourceTransferChannel,
            OutputTransferChannel targetTransferChannel
    ) throws Exception {
        
        if(!sourceFile.mkDirTree.processed)
            processMkDirs(sourceFile.mkDirTree, targetFs);
        
        String outPath = PathUtils.combine(
                sourceFile.mkDirTree.path,
                sourceFile.proposedName == null ? sourceFile.info.getName() : sourceFile.proposedName,
                targetTransferChannel.getSeparator()
        );
        
        String inPath = sourceFile.info.getPath();
        
        System.out.println("Copying -- " + inPath + " to " + outPath);
        
        try (InputStream in = sourceTransferChannel.getInputStream(inPath); OutputStream out = targetTransferChannel.getOutputStream(outPath)) {
            long len = sourceTransferChannel.getSize(inPath);
            System.out.println("Initiate write");
            
            int bufferCapacity = DEFAULT_BUF_SIZE;
            if (in instanceof SSHRemoteFileInputStream && out instanceof SSHRemoteFileOutputStream) {
                bufferCapacity = Math.min(((SSHRemoteFileInputStream) in).getBufferCapacity(),
                        ((SSHRemoteFileOutputStream) out).getBufferCapacity());
            } else if (in instanceof SSHRemoteFileInputStream) {
                bufferCapacity = ((SSHRemoteFileInputStream) in).getBufferCapacity();
            } else if (out instanceof SSHRemoteFileOutputStream) {
                bufferCapacity = ((SSHRemoteFileOutputStream) out).getBufferCapacity();
            }
            
            byte[] buf = new byte[bufferCapacity];
            
            while (len > 0) {
                // File transfer is stopped by interrupting the thread
                int x = in.read(buf);
                if (x == -1)
                    throw new IOException("Unexpected EOF");
                out.write(buf, 0, x);
                len -= x;
                incrementProcessedBytes(x);
            }
            System.out.println("Copy done before stream closing");
            out.flush();
        }
        System.out.println("Copy done");
    }
    
    public boolean stop() {
        return false;
    }
    
    public boolean isPrepared() {
        return filesToTransfer != null;
    }
    
    private static class TargetHandler{
        
        private final List<FileInfo> currentTargetFiles;
        
        public TargetHandler(List<FileInfo> currentTargetFiles){
            this.currentTargetFiles = currentTargetFiles;
        }
        
        public boolean exists(String name){
            for (FileInfo file1 : currentTargetFiles) {
                if (name.equals(file1.getName())) {
                    return true;
                }
            }
            return false;
        }
        
        public void proposeNewNameFor(FileInfoDeduplicate fileInfoDeduplicate, List<FileInfoDeduplicate> currentSourceFiles) throws TauonOperationException.NotImplemented {
            String name = fileInfoDeduplicate.info.getName();
            
            int dotPosition = name.lastIndexOf('.');
            if(dotPosition == -1 || dotPosition == 0){
                proposeNewNameFor(fileInfoDeduplicate, name.length(), currentSourceFiles);
            } else {
                String extension = name.substring(dotPosition);
                if(extension.equals(".gz") || extension.equals(".zip") || extension.equals(".bz")) {
                    // Look to see if there is another extension there
                    int dotPosition2 = name.lastIndexOf('.', dotPosition-1);
                    if (dotPosition2 != -1 && dotPosition2 != 0) {
                        // There is a dot not at the beginning, so assume it's another extension
                        proposeNewNameFor(fileInfoDeduplicate, dotPosition2, currentSourceFiles);
                        return;
                    }
                }
                proposeNewNameFor(fileInfoDeduplicate, dotPosition, currentSourceFiles);
            }
        }
        
        private void proposeNewNameFor(FileInfoDeduplicate fileInfoDeduplicate, int splitPosition, List<FileInfoDeduplicate> currentSourceFiles) throws TauonOperationException.NotImplemented {
            String name = fileInfoDeduplicate.info.getName();
            
            int n = 1;
            int padding = 0;
            
            String begin = name.substring(0, splitPosition);
            String end = name.substring(splitPosition);
            
            for(FileInfo currentTargetFile: currentTargetFiles){
                String currentTargetFileName = currentTargetFile.getName();
                if(currentTargetFileName.startsWith(begin) && currentTargetFileName.endsWith(end)){
                    int currentTargetFileNameSplitPosition = currentTargetFileName.length()-end.length();
                    int matchStart = findMatch(currentTargetFileName, currentTargetFileNameSplitPosition-1);
                    if(matchStart > 0){
                        // Skip the underscore and parse the integer
                        int numberBegin = matchStart+1;
                        int numberEnd = currentTargetFileNameSplitPosition;
                        boolean isZeroPadded = currentTargetFileName.charAt(numberBegin) == '0';
                        int val = Integer.parseInt(currentTargetFileName, numberBegin, numberEnd, 10);
                        // The val is a positive integer, the findMatch function matches only numbers
                        if(val == Integer.MAX_VALUE){
                            throw new TauonOperationException.NotImplemented("Auto-renaming is limited to the Integer.MAX_VALUE");
                        }
                        n = Math.max(n, val+1);
                        if(isZeroPadded)
                            padding = Math.max(padding, numberEnd - numberBegin);
                    }
                    // If not match, it has another string that will not collide with what we are going to generate.
                }
            }
            
            // Make sure that the number we will generate will not collide with any of the files we are copying
            for(FileInfoDeduplicate currentSourceFile: currentSourceFiles){
                if(currentSourceFile == fileInfoDeduplicate)
                    continue;
                
                String currentSourceFileName = currentSourceFile.info.getName();
                if(currentSourceFileName.startsWith(begin) && currentSourceFileName.endsWith(end)){
                    int currentTargetFileNameSplitPosition = currentSourceFileName.length()-end.length();
                    int matchStart = findMatch(name, currentTargetFileNameSplitPosition);
                    if(matchStart > 0){
                        // Skip the underscore and parse the integer
                        int numberBegin = matchStart+1;
                        int numberEnd = currentTargetFileNameSplitPosition;
                        boolean isZeroPadded = currentSourceFileName.charAt(numberBegin) == '0';
                        int val = Integer.parseInt(currentSourceFileName, numberBegin, numberEnd, 10);
                        // The val is a positive integer, the findMatch function matches only numbers
                        if(val == Integer.MAX_VALUE){
                            throw new TauonOperationException.NotImplemented("Auto-renaming is limited to the Integer.MAX_VALUE");
                        }
                        n = Math.max(n, val+1);
                        if(isZeroPadded)
                            padding = Math.max(padding, numberEnd - numberBegin);
                    }
                    // If not match, it has another string that will not collide with what we are going to generate.
                }
            }
            
            StringBuilder sb = new StringBuilder(begin);
            sb.append('_');
            String vs = String.valueOf(n);
            if(padding > vs.length()) {
                // Don't use String.repeat() it's inefficient
                //noinspection StringRepeatCanBeUsed
                for (int i = 0; i < padding - vs.length(); i++) {
                    sb.append('0');
                }
            }
            sb.append(vs);
            sb.append(end);
            
            fileInfoDeduplicate.proposedName = sb.toString();
        }
        
        private int findMatch(String name, int splitPosition) {
            boolean numbersFound = false;
            for (int i = splitPosition; i >= 0; i--) {
                char chart = name.charAt(i);
                if(chart >= '0' && chart <= '9'){
                    // A number found
                    numbersFound = true;
                }else if(chart == '_'){
                    // Underscore found
                    if(numbersFound) {
                        // If the underscore is the first character, treat the characters as the file name
                        return i == 0 ? -1 : i;
                    }else{
                        // Underscore without numbers
                        return -1;
                    }
                }else{
                    return -1;
                }
            }
            return -1;
        }
    }
    
    protected static class MkDirTree{
        public String path;
        MkDirTree parent;
        boolean processed = false;
        
        public MkDirTree(String path, MkDirTree parent) {
            this.path = path;
            this.parent = parent;
        }
    }
    
    private static class FileInfoDeduplicate {
        final FileInfo info;
        final String targetPath;
        final boolean duplicated;
        String proposedName;
        
        public FileInfoDeduplicate(FileInfo info, String targetPath, boolean duplicated) {
            this.info = info;
            this.targetPath = targetPath;
            this.duplicated = duplicated;
        }
        
    }
    
    protected static class FileInfoHolder {
        final FileInfo info;
        final String proposedName;
        final MkDirTree mkDirTree;
        
        public FileInfoHolder(FileInfo info, MkDirTree mkDirTree) {
            this.info = info;
            this.proposedName = null;
            this.mkDirTree = Objects.requireNonNull(mkDirTree);
        }
        
        private FileInfoHolder(FileInfoDeduplicate file, MkDirTree mkDirTree) {
            this.info = file.info;
            this.proposedName = file.proposedName;
            this.mkDirTree = Objects.requireNonNull(mkDirTree);
        }
    }
    
}

package tauon.app.ui.containers.session.pages.files.view;

import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ui.containers.session.pages.files.AbstractFileBrowserView;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

public class DndTransferData implements Serializable {
    
    private transient AbstractFileBrowserView sourceFileBrowser;
    private UUID sourceFileBrowserSessionId;
    private UUID sourceFileBrowserId;
    
    private FileInfo[] files;
//    private String currentDirectory;
    private transient AbstractFileBrowserView destinationFileBrowser;
    private UUID destinationFileBrowserSessionId;
    private UUID destinationFileBrowserId;
    
    private TransferAction transferAction = TransferAction.DragDrop;
//    private final DndSourceType sourceType;
    
    public DndTransferData(
            AbstractFileBrowserView sourceFileBrowser,
            FileInfo[] files,
//            String currentDirectory,
            AbstractFileBrowserView destinationFileBrowser
//            DndSourceType sourceType
    ) {
        this.sourceFileBrowser = sourceFileBrowser;
        this.sourceFileBrowserSessionId = sourceFileBrowser != null ? sourceFileBrowser.getFileBrowser().getHolder().getUUID() : null;
        this.sourceFileBrowserId = sourceFileBrowser != null ? sourceFileBrowser.getUUID() : null;
        
        this.files = files;
//        this.currentDirectory = currentDirectory;
        
        this.destinationFileBrowser = destinationFileBrowser;
        this.destinationFileBrowserSessionId = destinationFileBrowser != null ? destinationFileBrowser.getFileBrowser().getHolder().getUUID() : null;
        this.destinationFileBrowserId = destinationFileBrowser != null ? destinationFileBrowser.getUUID() : null;

//        this.sourceType = sourceType;
    }
//
//    @Override
//    public String toString() {
//        return "DndTransferData{" + "sessionHashcode=" + sessionOwner
//                + ", files=" + Arrays.toString(files) + ", currentDirectory='"
//                + currentDirectory + '\'' + '}';
//    }
    
    

    public AbstractFileBrowserView getSource() {
        return sourceFileBrowser;
    }

    public void setSource(AbstractFileBrowserView sourceFileBrowser) {
        this.sourceFileBrowser = sourceFileBrowser;
    }

    public FileInfo[] getFiles() {
        return files;
    }

    public void setFiles(FileInfo[] files) {
        this.files = files;
    }

//    public String getCurrentDirectory() {
//        return currentDirectory;
//    }
//
//    public void setCurrentDirectory(String currentDirectory) {
//        this.currentDirectory = currentDirectory;
//    }

    public AbstractFileBrowserView getDestination() {
        return destinationFileBrowser;
    }

    public void setDestination(AbstractFileBrowserView destinationFileBrowser) {
        this.destinationFileBrowser = destinationFileBrowser;
    }

    public TransferAction getTransferAction() {
        return transferAction;
    }

    public void setTransferAction(TransferAction transferAction) {
        this.transferAction = transferAction;
    }

//    public DndSourceType getSourceType() {
//        return sourceType;
//    }
    
    
    @Override
    public String toString() {
        return "DndTransferData{" +
                "sourceFileBrowser=" + sourceFileBrowser +
                ", files=" + Arrays.toString(files) +
                ", destinationFileBrowser=" + destinationFileBrowser +
                ", transferAction=" + transferAction +
                '}';
    }
    
    public UUID getSourceFileBrowserSessionId() {
        return sourceFileBrowserSessionId;
    }
    
    public void setSourceFileBrowserSessionId(UUID sourceFileBrowserSessionId) {
        this.sourceFileBrowserSessionId = sourceFileBrowserSessionId;
    }
    
    public UUID getSourceFileBrowserId() {
        return sourceFileBrowserId;
    }
    
    public void setSourceFileBrowserId(UUID sourceFileBrowserId) {
        this.sourceFileBrowserId = sourceFileBrowserId;
    }
    
    public UUID getDestinationFileBrowserSessionId() {
        return destinationFileBrowserSessionId;
    }
    
    public void setDestinationFileBrowserSessionId(UUID destinationFileBrowserSessionId) {
        this.destinationFileBrowserSessionId = destinationFileBrowserSessionId;
    }
    
    public UUID getDestinationFileBrowserId() {
        return destinationFileBrowserId;
    }
    
    public void setDestinationFileBrowserId(UUID destinationFileBrowserId) {
        this.destinationFileBrowserId = destinationFileBrowserId;
    }
    
    public enum DndSourceType {
        SSH, LOCAL
    }

    public enum TransferAction {
        DragDrop, Cut, Copy
    }
}

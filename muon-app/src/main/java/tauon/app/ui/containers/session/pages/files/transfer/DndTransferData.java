package tauon.app.ui.containers.session.pages.files.transfer;

import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ui.containers.session.pages.files.AbstractFileBrowserView;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

public class DndTransferData implements Serializable {
    
    private transient AbstractFileBrowserView sourceFileBrowser;
    private final UUID sourceFileBrowserSessionId;
    private final UUID sourceFileBrowserId;
    
    private FileInfo[] files;

    private transient AbstractFileBrowserView destinationFileBrowser;
    private final UUID destinationFileBrowserSessionId;
    private final UUID destinationFileBrowserId;
    
    private TransferAction transferAction = TransferAction.DragDrop;
    
    public DndTransferData(
            AbstractFileBrowserView sourceFileBrowser,
            FileInfo[] files,
            AbstractFileBrowserView destinationFileBrowser
    ) {
        this.sourceFileBrowser = sourceFileBrowser;
        this.sourceFileBrowserSessionId = sourceFileBrowser != null ? sourceFileBrowser.getFileBrowser().getHolder().getUUID() : null;
        this.sourceFileBrowserId = sourceFileBrowser != null ? sourceFileBrowser.getUUID() : null;
        
        this.files = files;
        
        this.destinationFileBrowser = destinationFileBrowser;
        this.destinationFileBrowserSessionId = destinationFileBrowser != null ? destinationFileBrowser.getFileBrowser().getHolder().getUUID() : null;
        this.destinationFileBrowserId = destinationFileBrowser != null ? destinationFileBrowser.getUUID() : null;

    }
    
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
    
    public UUID getSourceFileBrowserId() {
        return sourceFileBrowserId;
    }
    
    public UUID getDestinationFileBrowserSessionId() {
        return destinationFileBrowserSessionId;
    }
    
    public UUID getDestinationFileBrowserId() {
        return destinationFileBrowserId;
    }
    
    public enum TransferAction {
        DragDrop, Cut, Copy
    }
}

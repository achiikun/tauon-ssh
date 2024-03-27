package tauon.app.ui.containers.session.pages.files.view;

import org.jetbrains.annotations.NotNull;
import tauon.app.App;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileType;
import tauon.app.ssh.filesystem.LocalFileSystem;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.containers.session.pages.files.AbstractFileBrowserView;
import tauon.app.util.misc.PlatformUtils;
import tauon.app.util.misc.Win32DragHandler;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DndTransferHandler extends TransferHandler implements Transferable {
    
    public static final DataFlavor DATA_FLAVOR_DATA_FILE = new DataFlavor(DndTransferData.class, "data-file");
    public static final DataFlavor DATA_FLAVOR_FILE_LIST = DataFlavor.javaFileListFlavor;
    
//    private final FolderView folderView;
//    private final SessionContentPanel sessionOwner;
    private final AbstractFileBrowserView fileBrowserView;
//    private final DndTransferData.DndSourceType sourceType;
    
    private DndTransferData transferData;
    
    private Win32DragHandler win32DragHandler;
    private File tempDir;
//    private final FileBrowser fileBrowser;

    public DndTransferHandler(
//            FolderView folderView,
//            SessionContentPanel sessionOwner,
            @NotNull AbstractFileBrowserView destinationFileBrowser
//            DndTransferData.DndSourceType sourceType,
//            FileBrowser fileBrowser
    ) {
//        this.folderView = folderView;
//        this.fileBrowser = fileBrowser;
//        this.sessionOwner = sessionOwner;
        this.fileBrowserView = destinationFileBrowser;
//        this.sourceType = sourceType;
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        if (fileBrowserView.getFileSystem().isRemote()) {
            if (PlatformUtils.IS_WINDOWS) {
                try {
                    this.tempDir = Files.createTempDirectory(App.APP_INSTANCE_ID).toFile();
                    System.out.println("New monitor");
                    this.win32DragHandler = new Win32DragHandler();
                    this.win32DragHandler.listenForDrop(tempDir.getName(), file -> {
                        System.err.println("Dropped on " + file.getParent());
                        this.fileBrowserView.getFileBrowser()
                                .handleLocalDrop(transferData, new LocalFileSystem(), file.getParent());
                    });
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        DndTransferData data = new DndTransferData(fileBrowserView, fileBrowserView.getSelectedFiles(), null);
//                this.fileBrowserView.getCurrentDirectory(), this.fileBrowserView, sourceType);
        System.out.println("Exporting drag " + data + " hashcode: " + data.hashCode());
        this.transferData = data;
        super.exportAsDrag(comp, e, action);
    }

    @Override
    public boolean canImport(TransferSupport support) {

        System.out.println("Data flavors: " + support.getDataFlavors().length);
        boolean isDataFile = false, isJavaFileList = false;
        for (DataFlavor f : support.getDataFlavors()) {
            System.out.println("Data flavor: " + f);
            if (f.isFlavorJavaFileListType()) {
                isJavaFileList = this.fileBrowserView.getFileSystem().isRemote();
            }
            if (DATA_FLAVOR_DATA_FILE.equals(f)) {
                isDataFile = true;
            }
        }

        try {
            System.out.println("Dropped java file list: " + isJavaFileList);
            if (isDataFile) {
                if (support.isDataFlavorSupported(DATA_FLAVOR_DATA_FILE)) {
                    return (
                            support.getTransferable()
                            .getTransferData(DATA_FLAVOR_DATA_FILE) instanceof DndTransferData
                    );
                }
            } else if (isJavaFileList) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("drop not supported");
        return false;

    }

    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY;
    }

    protected void exportDone(JComponent c, Transferable data, int action) {
        System.out.println("Export complete: " + action + " " + Arrays.asList(data.getTransferDataFlavors()));
        if (this.win32DragHandler != null) {
            this.win32DragHandler.dispose();
        }
    }

    /**
     * When importing always DATA_FLAVOR_DATA_FILE will be preferred over file list
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }

        boolean isDataFile = false, isJavaFileList = false;
        for (DataFlavor f : info.getDataFlavors()) {
            System.out.println("Data flavor: " + f);
            if (f.isFlavorJavaFileListType()) {
                isJavaFileList = this.fileBrowserView.getFileSystem().isRemote();
            }
            if (DATA_FLAVOR_DATA_FILE.equals(f)) {
                isDataFile = true;
            }
        }

        Transferable t = info.getTransferable();

        if (isDataFile) {
            try {
                DndTransferData transferData = (DndTransferData) t.getTransferData(DATA_FLAVOR_DATA_FILE);
                
                UUID sourceSessionId = transferData.getSourceFileBrowserSessionId();
                UUID sourceId = transferData.getSourceFileBrowserId();
                
                if(sourceSessionId != null){
                    
                    SessionContentPanel sessionContentPanel = fileBrowserView.getFileBrowser().getHolder().getAppWindow()
                            .findSessionById(sourceSessionId);
                    
                    if(sessionContentPanel == null || sourceId == null)
                        return false;
                    
                    AbstractFileBrowserView view = sessionContentPanel.getFileBrowser().findViewById(sourceId);
                    if(view == null)
                        return false;
                    
                    transferData.setSource(view);
                    
                }
                
                
                UUID destinationSessionId = transferData.getDestinationFileBrowserSessionId();
                UUID destinationId = transferData.getDestinationFileBrowserId();
                
                if(destinationSessionId != null){
                    
                    SessionContentPanel sessionContentPanel = fileBrowserView.getFileBrowser().getHolder().getAppWindow()
                            .findSessionById(destinationSessionId);
                    
                    if(sessionContentPanel == null || destinationId == null)
                        return false;
                    
                    AbstractFileBrowserView view = sessionContentPanel.getFileBrowser().findViewById(destinationId);
                    if(view == null)
                        return false;
                    
                    transferData.setDestination(view);
                    
                }
                
                
                return this.fileBrowserView.handleDrop(transferData);
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (isJavaFileList) {
            try {
                List<File> fileList = ((List<File>) t.getTransferData(DataFlavor.javaFileListFlavor));
                if (fileList != null) {
                    FileInfo[] infoArr = new FileInfo[fileList.size()];
                    int c = 0;
                    for (File file : fileList) {

                        if (file.getName().startsWith(App.APP_INSTANCE_ID)) {
                            System.out.println("Internal fake folder dropped");
                            return false;
                        }

                        Path p = file.toPath();
                        BasicFileAttributes attrs = null;
                        try {
                            attrs = Files.readAttributes(p, BasicFileAttributes.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        FileInfo finfo = new FileInfo(file.getName(), file.getAbsolutePath(), file.length(),
                                file.isDirectory() ? FileType.Directory : FileType.File, file.lastModified(), -1,
                                LocalFileSystem.PROTO_LOCAL_FILE, "",
                                attrs != null ? attrs.creationTime().toMillis() : file.lastModified(), "",
                                file.isHidden());
                        infoArr[c++] = finfo;
                    }

                    DndTransferData data = new DndTransferData(null, infoArr, this.fileBrowserView);
                    System.out.println("Exporting drag " + data + " hashcode: " + data.hashCode());
                    return this.fileBrowserView.handleDrop(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DATA_FLAVOR_DATA_FILE, DATA_FLAVOR_FILE_LIST};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        if (this.fileBrowserView.getFileSystem().isRemote()) {
            return DATA_FLAVOR_DATA_FILE.equals(flavor) || DATA_FLAVOR_FILE_LIST.equals(flavor);
        } else {
            return DATA_FLAVOR_DATA_FILE.equals(flavor);
        }
    }

    @Override
    public @NotNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (DATA_FLAVOR_DATA_FILE.equals(flavor)) {
            return this.transferData;
        }

        if (DATA_FLAVOR_FILE_LIST.equals(flavor)) {
            if (PlatformUtils.IS_WINDOWS && tempDir != null) {
                return Arrays.asList(tempDir);
            }
        }
        
        throw new UnsupportedFlavorException(flavor);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        return this;
    }
}

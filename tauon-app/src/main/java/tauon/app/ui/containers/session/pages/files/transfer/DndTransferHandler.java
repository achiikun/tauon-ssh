package tauon.app.ui.containers.session.pages.files.transfer;

import com.sun.jna.platform.dnd.DragHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.RemoteOperationException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileType;
import tauon.app.ssh.filesystem.LocalFileSystem;
import tauon.app.ui.containers.session.AbstractSessionContentPanel;
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
    
    public static final Logger LOG = LoggerFactory.getLogger(DndTransferHandler.class);
    
    public static final DataFlavor DATA_FLAVOR_DATA_FILE = new DataFlavor(DndTransferData.class, "data-file");
    public static final DataFlavor DATA_FLAVOR_FILE_LIST = DataFlavor.javaFileListFlavor;
    
    private final AbstractFileBrowserView fileBrowserView;
    
    private DndTransferData transferData;
    
    private Win32DragHandler win32DragHandler;
    private File tempDir;

    public DndTransferHandler(
            @NotNull AbstractFileBrowserView destinationFileBrowser
    ) {
        this.fileBrowserView = destinationFileBrowser;
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        try {
            if (fileBrowserView.getFileSystem().isRemote()) {
                if (PlatformUtils.IS_WINDOWS) {
                    try {
                        this.tempDir = Files.createTempDirectory(App.APP_INSTANCE_ID).toFile();
                        LOG.debug("Create monitor in windows to know where to copy remote file.");
                        this.win32DragHandler = new Win32DragHandler();
                        this.win32DragHandler.listenForDrop(tempDir.getName(), file -> {
                            LOG.debug("File dropped on: " + file.getParent());
                            this.fileBrowserView.getFileBrowser().handleLocalDrop(
                                    transferData,
                                    LocalFileSystem.getInstance(),
                                    file.getParent()
                            );
                        });
                    } catch (IOException e1) {
                        LOG.debug("Error while creating monitor: ", e1);
                        return;
                    }
                }else{
                    // Download file to a temporary folder
                    // Return that file in getTransferData()
                    // Only move supported, linux will move the whole file until the temporary one is closed.
                    // In case of copy, linux will only copy the bytes already downloaded, not the whole file.
                    
                }
            }
        } catch (OperationCancelledException ex) {
            throw new RuntimeException(ex);
        } catch (RemoteOperationException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (SessionClosedException ex) {
            throw new RuntimeException(ex);
        }
        
        DndTransferData data = new DndTransferData(fileBrowserView, fileBrowserView.getSelectedFiles(), null);
        LOG.debug("Creating data to export: " + data);
        this.transferData = data;
        
        super.exportAsDrag(comp, e, action);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        LOG.debug("Preparing to import data. len(supported_flavours): " + support.getDataFlavors().length);
        
        boolean isInternalFile = false, isJavaFileList = false;
        for (DataFlavor f : support.getDataFlavors()) {
            LOG.debug("Checking flavour: " + f);
            if (f.isFlavorJavaFileListType()) {
                try {
                    isJavaFileList = this.fileBrowserView.getFileSystem().isRemote();
                } catch (OperationCancelledException e) {
                    throw new RuntimeException(e);
                } catch (RemoteOperationException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (SessionClosedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (DATA_FLAVOR_DATA_FILE.equals(f)) {
                isInternalFile = true;
            }
        }

        if (isInternalFile) {
            if (support.isDataFlavorSupported(DATA_FLAVOR_DATA_FILE)) {
                LOG.debug("Is internal file. (Supported)");
                try {
                    return (
                            support.getTransferable()
                            .getTransferData(DATA_FLAVOR_DATA_FILE) instanceof DndTransferData
                    );
                } catch (UnsupportedFlavorException e) {
                    LOG.warn("You found a bug!", e);
                } catch (IOException e) {
                    LOG.error("Error while retrieving data object.", e);
                    return false;
                }
            }
        } else if (isJavaFileList) {
            return true;
        }
        
        LOG.debug("Data is not supported.");
        return false;

    }

    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY;
    }

    protected void exportDone(JComponent c, Transferable data, int action) {
        LOG.debug("Export complete. action: " + action + " flavors: " + Arrays.asList(data.getTransferDataFlavors()));
        if (this.win32DragHandler != null) {
            this.win32DragHandler.dispose();
            this.win32DragHandler = null;
        }
    }

    /**
     * When importing always DATA_FLAVOR_DATA_FILE will be preferred over file list
     */
    @Override
    public boolean importData(TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }

        boolean isDataFile = false, isJavaFileList = false;
        for (DataFlavor f : info.getDataFlavors()) {
            System.out.println("Data flavor: " + f);
            if (f.isFlavorJavaFileListType()) {
                try {
                    isJavaFileList = this.fileBrowserView.getFileSystem().isRemote();
                } catch (OperationCancelledException e) {
                    throw new RuntimeException(e);
                } catch (RemoteOperationException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (SessionClosedException e) {
                    throw new RuntimeException(e);
                }
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
                    
                    AbstractSessionContentPanel sessionContentPanel = fileBrowserView.getFileBrowser().getHolder().getAppWindow()
                            .findSessionById(sourceSessionId);
                    
                    if(sourceId == null || !(sessionContentPanel instanceof SessionContentPanel))
                        return false;
                    
                    AbstractFileBrowserView view = ((SessionContentPanel) sessionContentPanel).getFileBrowser().findViewById(sourceId);
                    if(view == null)
                        return false;
                    
                    transferData.setSource(view);
                    
                }
                
                
                UUID destinationSessionId = transferData.getDestinationFileBrowserSessionId();
                UUID destinationId = transferData.getDestinationFileBrowserId();
                
                if(destinationSessionId != null){
                    
                    SessionContentPanel sessionContentPanel = (SessionContentPanel) fileBrowserView.getFileBrowser()
                            .getHolder().getAppWindow().findSessionById(destinationSessionId);
                    
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
                @SuppressWarnings("unchecked")
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
                                file.isDirectory() ? FileType.DIR : FileType.FILE, file.lastModified(), -1,
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
        try {
            if (this.fileBrowserView.getFileSystem().isRemote()) {
                return DATA_FLAVOR_DATA_FILE.equals(flavor) || DATA_FLAVOR_FILE_LIST.equals(flavor);
            } else {
                return DATA_FLAVOR_DATA_FILE.equals(flavor);
            }
        } catch (OperationCancelledException e) {
            throw new RuntimeException(e);
        } catch (RemoteOperationException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (SessionClosedException e) {
            throw new RuntimeException(e);
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

//package tauon.app.ui.containers.main;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import tauon.app.ui.containers.session.pages.files.transfer.FileTransfer;
//
//import javax.swing.*;
//
//import static tauon.app.services.LanguageService.getBundle;
//
//public class FileTransferManager {
//
//    private static final Logger LOG = LoggerFactory.getLogger(FileTransferManager.class);
//
//    private final AppWindow appWindow;
//    private final BackgroundTransferPanel upload;
//    private final BackgroundTransferPanel download;
//
//    public FileTransferManager(AppWindow appWindow, BackgroundTransferPanel upload, BackgroundTransferPanel download) {
//        this.appWindow = appWindow;
//        this.upload = upload;
//        this.download = download;
//    }
//
//    public void startFileTransfer(FileTransfer fileTransfer, boolean background, FileTransferProgress callback) {
//
//        FileTransferProgress uiFileTransfer;
//
//        if (background) {
//            if (fileTransfer.isUpload()) {
//                uiFileTransfer = upload.addNewBackgroundTransfer(fileTransfer);
//            } else if (fileTransfer.isDownload()) {
//                uiFileTransfer = download.addNewBackgroundTransfer(fileTransfer);
//            } else {
//                // TODO To another server
//                uiFileTransfer = download.addNewBackgroundTransfer(fileTransfer);
//            }
//        } else {
//
//            uiFileTransfer = fileTransfer.getSession().startFileTransferModal(onUserStop -> {
//                fileTransfer.close();
//            });
//
//        }
//
//        fileTransfer.getSession().getFileBrowser().getBackgroundTransferPool().submit(
//                () -> fileTransfer.run(
//                        new FileTransferProgress.Compose(
//                                uiFileTransfer,
//                                new FileTransferProgress.Adapter() {
//
//                                    @Override
//                                    public void error(String cause, Exception e, FileTransfer fileTransfer) {
//                                        if (!fileTransfer.getSession().isSessionClosed()) {
//                                            JOptionPane.showMessageDialog(appWindow, getBundle().getString("general.message.operation_failed"));
//                                        }
//                                    }
//
//                                    @Override
//                                    public void done(FileTransfer fileTransfer) {
//                                        // This code is unfocusing the terminal, prefer to update manually than unfocusing the terminal
////                                        Component focus = fileTransfer.getSession().getAppWindow().getFocusOwner();
////                                        fileTransfer.getSession().getFileBrowser().notifyTransferDone(fileTransfer);
////                                        if(focus != null) {
////                                            SwingUtilities.invokeLater(() -> {
////                                                Component focus2 = fileTransfer.getSession().getAppWindow().getFocusOwner();
////                                                fileTransfer.getSession().getTerminalHolder().focusTerminal();
////                                            });
////                                        }
//                                    }
//                                },
//                                callback
//                        )
//                )
//        );
//
//    }
//
//
//}

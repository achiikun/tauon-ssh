package tauon.app.ui.containers.main;

import tauon.app.App;
import tauon.app.ui.components.misc.FontAwesomeContants;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.containers.session.pages.files.transfer.FileTransfer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class BackgroundTransferPanel extends JPanel {
    private final Box verticalBox;
    private final AtomicInteger transferCount = new AtomicInteger(0);
    private final Consumer<Integer> callback;
    
    /**
     * @param activeUpdater callback for notifying number of active transfers
     */
    public BackgroundTransferPanel(Consumer<Integer> activeUpdater) {
        super(new BorderLayout());
        this.callback = activeUpdater;
        verticalBox = Box.createVerticalBox();
        JScrollPane jsp = new JScrollPane(verticalBox);
        jsp.setBorder(null);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(jsp);
    }
    
    public FileTransferProgress addNewBackgroundTransfer(FileTransfer transfer) {
        TransferPanelItem item = new TransferPanelItem(transfer);
        item.setAlignmentX(Box.LEFT_ALIGNMENT);
        this.verticalBox.add(item);
        this.verticalBox.revalidate();
        this.verticalBox.repaint();
        return item;
    }
    
    public void removePendingTransfers(SessionContentPanel sessionId) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> stopSession(sessionId));
            } catch (InvocationTargetException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            stopSession(sessionId);
        }
    }
    
    private void stopSession(SessionContentPanel sessionId) {
        for (int i = 0; i < this.verticalBox.getComponentCount(); i++) {
            Component c = this.verticalBox.getComponent(i);
            if (c instanceof TransferPanelItem) {
                TransferPanelItem tpi = (TransferPanelItem) c;
                if (tpi.fileTransfer.getSession() == sessionId) {
                    tpi.stop();
                }
            }
        }
    }
    
    class TransferPanelItem extends JPanel implements FileTransferProgress {
        private final FileTransfer fileTransfer;
        private final JProgressBar progressBar;
        private final JLabel progressLabel;
//        private Future<?> handle;
        
        public TransferPanelItem(FileTransfer transfer) {
            super(new BorderLayout());
            transferCount.incrementAndGet();
            callback.accept(transferCount.get());
            this.fileTransfer = transfer;

            progressBar = new JProgressBar();
            progressLabel = new JLabel("Waiting...");
            progressLabel.setBorder(new EmptyBorder(5, 0, 5, 5));
            JLabel removeLabel = new JLabel();
            removeLabel.setFont(App.skin.getIconFont());
            removeLabel.setText(FontAwesomeContants.FA_TRASH);
            
            removeLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    stop();
                }
            });
            
            setBorder(new EmptyBorder(10, 10, 10, 10));
            Box topBox = Box.createHorizontalBox();
            topBox.add(progressLabel);
            topBox.add(Box.createHorizontalGlue());
            topBox.add(removeLabel);
            
            add(topBox);
            add(progressBar, BorderLayout.SOUTH);
            
            setMaximumSize(new Dimension(getMaximumSize().width, getPreferredSize().height));
        }
        
        public void stop() {
            if(!fileTransfer.stop()){
                done(fileTransfer);
            }
        }
        
        @Override
        public void init(long totalSize, long files, FileTransfer fileTransfer) {
                progressLabel.setText(
                        String.format("Copying %s to %s", fileTransfer.getSourceName(), fileTransfer.getTargetName()));
                progressBar.setValue(0);
        }
        
        @Override
        public void progress(long processedBytes, long totalBytes, long processedCount, long totalCount,
                             FileTransfer fileTransfer) {
                    progressBar.setValue(totalBytes > 0 ? ((int) ((processedBytes * 100) / totalBytes)) : 0);
        }
        
        @Override
        public void error(String cause, FileTransfer fileTransfer) {
            transferCount.decrementAndGet();
            callback.accept(transferCount.get());
                    progressLabel.setText(String.format("Error while copying from %s to %s", fileTransfer.getSourceName(),
                    fileTransfer.getTargetName()));
        }
        
        @Override
        public void done(FileTransfer fileTransfer) {
            transferCount.decrementAndGet();
            callback.accept(transferCount.get());
            System.out.println("done transfer");
                BackgroundTransferPanel.this.verticalBox.remove(this);
                BackgroundTransferPanel.this.revalidate();
                BackgroundTransferPanel.this.repaint();
        }
    }
}

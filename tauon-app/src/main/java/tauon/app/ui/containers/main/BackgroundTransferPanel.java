package tauon.app.ui.containers.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.ssh.filesystem.transfer.FileTransfer;
import tauon.app.ui.components.misc.FontAwesomeContants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class BackgroundTransferPanel extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(BackgroundTransferPanel.class);
    
    private final Box verticalBox;
    
    private final AtomicInteger itemsCount = new AtomicInteger(0);
    private final AtomicInteger activeItemsCount = new AtomicInteger(0);
    
    private final Consumer<BackgroundTransferPanel> onItemsCountChanged;
    
    /**
     * @param onItemsCountChanged callback for notifying number of active transfers
     */
    public BackgroundTransferPanel(Consumer<BackgroundTransferPanel> onItemsCountChanged) {
        super(new BorderLayout());
        this.onItemsCountChanged = onItemsCountChanged;
        verticalBox = Box.createVerticalBox();
        JScrollPane jsp = new JScrollPane(verticalBox);
        jsp.setBorder(null);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(jsp);
    }
    
    public FileTransferProgress addNewBackgroundTransfer(FileTransfer transfer) {
        itemsCount.incrementAndGet();
        onItemsCountChanged.accept(this);
        
        TransferPanelItem item = new TransferPanelItem(transfer);
        item.setAlignmentX(Box.LEFT_ALIGNMENT);
        this.verticalBox.add(item);
        this.verticalBox.revalidate();
        this.verticalBox.repaint();
        return item;
    }
    
//    public void removePendingTransfers(SessionContentPanel sessionId) {
//        if (!SwingUtilities.isEventDispatchThread()) {
//            try {
//                SwingUtilities.invokeAndWait(() -> stopSession(sessionId));
//            } catch (InvocationTargetException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        } else {
//            stopSession(sessionId);
//        }
//    }
//
//    private void stopSession(SessionContentPanel sessionId) {
//        for (int i = 0; i < this.verticalBox.getComponentCount(); i++) {
//            Component c = this.verticalBox.getComponent(i);
//            if (c instanceof TransferPanelItem) {
//                TransferPanelItem tpi = (TransferPanelItem) c;
//                if (tpi.fileTransfer.getSession() == sessionId) {
//                    tpi.stop();
//                }
//            }
//        }
//    }
    
    private void incrementActive(){
        activeItemsCount.incrementAndGet();
        onItemsCountChanged.accept(this);
    }
    
    private void decrementActive(){
        activeItemsCount.decrementAndGet();
        onItemsCountChanged.accept(this);
    }
    
    public int getActiveItemsCount() {
        return activeItemsCount.get();
    }
    
    public int getItemsCount() {
        return itemsCount.get();
    }
    
    private void removeItem(TransferPanelItem transferPanelItem) {
        itemsCount.decrementAndGet();
        onItemsCountChanged.accept(this);
        System.out.println("done transfer");
        BackgroundTransferPanel.this.verticalBox.remove(transferPanelItem);
        BackgroundTransferPanel.this.revalidate();
        BackgroundTransferPanel.this.repaint();
    }
    
    class TransferPanelItem extends JPanel implements FileTransferProgress {
        private final FileTransfer fileTransfer;
        private final JProgressBar progressBar;
        private final JLabel progressLabel;
//        private Future<?> handle;
        
        public TransferPanelItem(FileTransfer transfer) {
            super(new BorderLayout());
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
                    if(!fileTransfer.stop()){
                        BackgroundTransferPanel.this.removeItem(TransferPanelItem.this);
                    }
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
        
        @Override
        public void init(long totalSize, long files) {
//                progressLabel.setText(
//                        String.format("Copying %s to %s", fileTransfer.getSourceName(), fileTransfer.getTargetName()));
            SwingUtilities.invokeLater(() -> {
                incrementActive();
                progressLabel.setText("Preparing copy...");
                progressBar.setValue(0);
            });
        }
        
        @Override
        public void progress(long processedBytes, long totalBytes, long processedCount, long totalCount) {
            SwingUtilities.invokeLater(() -> {
                progressLabel.setText(String.format("Copying %d out of %d.", processedCount + 1, totalCount));
                progressBar.setValue(totalBytes > 0 ? ((int) ((processedBytes * 100) / totalBytes)) : 0);
            });
        }
        
        @Override
        public void error(String cause, Exception e) {
            SwingUtilities.invokeLater(() -> {
                decrementActive();
                progressLabel.setText(String.format("Error while copying: %s", e.getMessage()));
            });
        }
        
        @Override
        public void done() {
            decrementActive();
            BackgroundTransferPanel.this.removeItem(this);
        }
    }
    
}

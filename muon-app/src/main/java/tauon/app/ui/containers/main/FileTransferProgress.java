package tauon.app.ui.containers.main;

import tauon.app.ui.containers.session.pages.files.transfer.FileTransfer;

import javax.swing.*;

public interface FileTransferProgress {
    void init(long totalSize, long files, FileTransfer fileTransfer);

    void progress(long processedBytes, long totalBytes, long processedCount, long totalCount, FileTransfer fileTransfer);

    void error(String cause, FileTransfer fileTransfer);

    void done(FileTransfer fileTransfer);
    
    class Delegate implements FileTransferProgress {
        
        private final FileTransferProgress delagator;
        
        public Delegate(FileTransferProgress delagator) {
            this.delagator = delagator;
        }
        
        @Override
        public void init(long totalSize, long files, FileTransfer fileTransfer) {
            delagator.init(totalSize, files, fileTransfer);
        }
        
        @Override
        public void progress(long processedBytes, long totalBytes, long processedCount, long totalCount, FileTransfer fileTransfer) {
            delagator.progress(processedBytes, totalBytes, processedCount, totalCount, fileTransfer);
        }
        
        @Override
        public void error(String cause, FileTransfer fileTransfer) {
            delagator.error(cause, fileTransfer);
        }
        
        @Override
        public void done(FileTransfer fileTransfer) {
            delagator.done(fileTransfer);
        }
    }
    
    class DelegateToSwing implements FileTransferProgress {
        
        private final FileTransferProgress delagator;
        
        public DelegateToSwing(FileTransferProgress delagator) {
            this.delagator = delagator;
        }
        
        @Override
        public void init(long totalSize, long files, FileTransfer fileTransfer) {
            SwingUtilities.invokeLater(() -> delagator.init(totalSize, files, fileTransfer));
        }
        
        @Override
        public void progress(long processedBytes, long totalBytes, long processedCount, long totalCount, FileTransfer fileTransfer) {
            SwingUtilities.invokeLater(() -> delagator.progress(processedBytes, totalBytes, processedCount, totalCount, fileTransfer));
        }
        
        @Override
        public void error(String cause, FileTransfer fileTransfer) {
            SwingUtilities.invokeLater(() -> delagator.error(cause, fileTransfer));
        }
        
        @Override
        public void done(FileTransfer fileTransfer) {
            SwingUtilities.invokeLater(() -> delagator.done(fileTransfer));
        }
    }
    
    
    class Compose implements FileTransferProgress {
        
        private final FileTransferProgress[] delegator;
        
        public Compose(FileTransferProgress... delegator) {
            this.delegator = delegator;
        }
        
        @Override
        public void init(long totalSize, long files, FileTransfer fileTransfer) {
            for(FileTransferProgress ftp: delegator)
                if(ftp != null)
                    ftp.init(totalSize, files, fileTransfer);
        }
        
        @Override
        public void progress(long processedBytes, long totalBytes, long processedCount, long totalCount, FileTransfer fileTransfer) {
            for(FileTransferProgress ftp: delegator)
                if(ftp != null)
                    ftp.progress(processedBytes, totalBytes, processedCount, totalCount, fileTransfer);
        }
        
        @Override
        public void error(String cause, FileTransfer fileTransfer) {
            for(FileTransferProgress ftp: delegator)
                if(ftp != null)
                    ftp.error(cause, fileTransfer);
        }
        
        @Override
        public void done(FileTransfer fileTransfer) {
            for(FileTransferProgress ftp: delegator)
                if(ftp != null)
                    ftp.done(fileTransfer);
        }
    }
    
    
    public class Adapter implements FileTransferProgress {
        
        @Override
        public void init(long totalSize, long files, FileTransfer fileTransfer) {
        }
        
        @Override
        public void progress(long processedBytes, long totalBytes, long processedCount, long totalCount, FileTransfer fileTransfer) {
        }
        
        @Override
        public void error(String cause, FileTransfer fileTransfer) {
        }
        
        @Override
        public void done(FileTransfer fileTransfer) {
        }
    }
}

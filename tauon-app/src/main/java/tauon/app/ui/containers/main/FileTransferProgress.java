package tauon.app.ui.containers.main;

public interface FileTransferProgress {
    void init(long totalSize, long files);

    void progress(long processedBytes, long totalBytes, long processedCount, long totalCount);

    void error(String cause, Exception e);

    void done();
    
    class Delegate implements FileTransferProgress {
        
        private final FileTransferProgress delagator;
        
        public Delegate(FileTransferProgress delagator) {
            this.delagator = delagator;
        }
        
        @Override
        public void init(long totalSize, long files) {
            delagator.init(totalSize, files);
        }
        
        @Override
        public void progress(long processedBytes, long totalBytes, long processedCount, long totalCount) {
            delagator.progress(processedBytes, totalBytes, processedCount, totalCount);
        }
        
        @Override
        public void error(String cause, Exception e) {
            delagator.error(cause, e);
        }
        
        @Override
        public void done() {
            delagator.done();
        }
        
    }
    
    class Compose implements FileTransferProgress {
        
        private final FileTransferProgress[] delegator;
        
        public Compose(FileTransferProgress... delegator) {
            this.delegator = delegator;
        }
        
        @Override
        public void init(long totalSize, long files) {
            for(FileTransferProgress ftp: delegator)
                if(ftp != null)
                    ftp.init(totalSize, files);
        }
        
        @Override
        public void progress(long processedBytes, long totalBytes, long processedCount, long totalCount) {
            for(FileTransferProgress ftp: delegator)
                if(ftp != null)
                    ftp.progress(processedBytes, totalBytes, processedCount, totalCount);
        }
        
        @Override
        public void error(String cause, Exception e) {
            for(FileTransferProgress ftp: delegator)
                if(ftp != null)
                    ftp.error(cause, e);
        }
        
        @Override
        public void done() {
            for(FileTransferProgress ftp: delegator)
                if(ftp != null)
                    ftp.done();
        }
    }
    
    
    public class Adapter implements FileTransferProgress {
        
        @Override
        public void init(long totalSize, long files) {
        }
        
        @Override
        public void progress(long processedBytes, long totalBytes, long processedCount, long totalCount) {
        }
        
        @Override
        public void error(String cause, Exception e) {
        }
        
        @Override
        public void done() {
        }
    }
}

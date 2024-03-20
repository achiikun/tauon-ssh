package tauon.app.ui.containers.main;

import tauon.app.ui.containers.session.pages.files.transfer.FileTransfer;

public interface FileTransferProgress {
    void init(long totalSize, long files, FileTransfer fileTransfer);

    void progress(long processedBytes, long totalBytes, long processedCount, long totalCount, FileTransfer fileTransfer);

    void error(String cause, FileTransfer fileTransfer);

    void done(FileTransfer fileTransfer);
}

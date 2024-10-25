package tauon.app.ssh.filesystem;

import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.exceptions.TauonOperationException;

import java.io.OutputStream;

public interface OutputTransferChannel {
    OutputStream getOutputStream(String path) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException;

    String getSeparator();
}

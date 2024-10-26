package tauon.app.ssh.filesystem;

import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.exceptions.TauonOperationException;

import java.io.InputStream;

public interface InputTransferChannel {
    InputStream getInputStream(String path) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException;

    String getSeparator();

    long getSize(String path) throws Exception;
}

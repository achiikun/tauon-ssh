package tauon.app.ssh;

import tauon.app.exceptions.*;

import java.io.IOException;

public interface ISSHOperator {
    void operate(GuiHandle guiHandle, SSHConnectionHandler instance) throws TauonOperationException, OperationCancelledException, AlreadyFailedException, SessionClosedException, InterruptedException, IOException;
}

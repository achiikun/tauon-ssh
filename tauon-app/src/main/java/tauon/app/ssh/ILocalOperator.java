package tauon.app.ssh;

import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.exceptions.LocalOperationException;
import tauon.app.exceptions.OperationCancelledException;

public interface ILocalOperator {
    void operate() throws LocalOperationException, AlreadyFailedException, OperationCancelledException, InterruptedException;
}

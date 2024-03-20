package tauon.app.ui.containers.session.pages.terminal.ssh;

import com.jediterm.terminal.TtyConnector;

public interface DisposableTtyConnector extends TtyConnector {
    void stop();

    boolean isCancelled();

    boolean isBusy();

    boolean isRunning();

    int getExitStatus();

    boolean isInitialized();
}

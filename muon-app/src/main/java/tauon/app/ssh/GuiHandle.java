package tauon.app.ssh;

import tauon.app.settings.PortForwardingRule;
import tauon.app.settings.SessionInfo;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public interface GuiHandle {
    
    void reportException(Throwable cause);
    
    char[] promptPassword(SessionInfo info, String user, AtomicBoolean remember);
    
    void reportPortForwardingFailed(PortForwardingRule portForwardingState, IOException e);
    
    interface BlockHandle{
        void unblock();
    }
    
    interface UserCancellable {
        void userCancelled(BlockHandle blockHandle);
    }
    
    BlockHandle blockUi(UserCancellable userCancellable);
    
    String promptUser(SessionInfo info, AtomicBoolean remember);
    
    
}

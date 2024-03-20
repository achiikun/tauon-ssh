package tauon.app.ssh;

import tauon.app.settings.PortForwardingRule;
import tauon.app.settings.SessionInfo;
import tauon.app.ui.dialogs.sessions.HopEntry;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public interface GuiHandle<C> {
    
    void reportException(Throwable cause);
    
    void reportPortForwardingFailed(PortForwardingRule portForwardingState, IOException e);
    
    BlockHandle blockUi(C client, UserCancellable userCancellable);
    
    String promptUser(HopEntry info, AtomicBoolean remember);
    
    char[] promptPassword(HopEntry info, String user, AtomicBoolean remember);
    
    interface BlockHandle{
        void unblock();
    }
    
    interface UserCancellable {
        void userCancelled(BlockHandle blockHandle);
    }
    
    abstract class Delegate<C> implements GuiHandle<C>{
        
        private final GuiHandle<?> delagator;
        
        public Delegate(GuiHandle<?> delagator) {
            this.delagator = delagator;
        }
        
        public void reportException(Throwable cause) {
            delagator.reportException(cause);
        }
        
        public char[] promptPassword(HopEntry info, String user, AtomicBoolean remember) {
            return delagator.promptPassword(info, user, remember);
        }
        
        public void reportPortForwardingFailed(PortForwardingRule portForwardingState, IOException e) {
            delagator.reportPortForwardingFailed(portForwardingState, e);
        }
        
        public String promptUser(HopEntry info, AtomicBoolean remember) {
            return delagator.promptUser(info, remember);
        }
        
    }
    
}

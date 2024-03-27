package tauon.app.ssh;

import tauon.app.settings.PortForwardingRule;
import tauon.app.settings.HopEntry;
import tauon.app.settings.SessionInfo;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public interface GuiHandle<C> {
    
    void reportException(Throwable cause);
    
    void reportPortForwardingFailed(PortForwardingRule portForwardingState, IOException e);
    
    BlockHandle blockUi(C client, UserCancelHandle userCancelHandle);
    
    String promptUser(HopEntry info, AtomicBoolean remember);
    
    char[] promptPassword(HopEntry info, String user, AtomicBoolean remember, boolean isRetrying);
    
    void showMessage(String name, String instruction);
    
    String promptInput(String prompt, boolean echo);
    
    void saveInfo(SessionInfo info);
    
    interface BlockHandle{
        void unblock();
    }
    
    interface UserCancelHandle {
        void userCancelled(BlockHandle blockHandle);
    }
    
    abstract class Delegate<C> implements GuiHandle<C>{
        
        private final GuiHandle<?> delagator;
        
        public Delegate(GuiHandle<?> delagator) {
            this.delagator = delagator;
        }
        
        @Override
        public void showMessage(String name, String instruction) {
            delagator.showMessage(name, instruction);
        }
        
        public void reportException(Throwable cause) {
            delagator.reportException(cause);
        }
        
        public char[] promptPassword(HopEntry info, String user, AtomicBoolean remember, boolean isRetrying) {
            return delagator.promptPassword(info, user, remember, isRetrying);
        }
        
        public void reportPortForwardingFailed(PortForwardingRule portForwardingState, IOException e) {
            delagator.reportPortForwardingFailed(portForwardingState, e);
        }
        
        public String promptUser(HopEntry info, AtomicBoolean remember) {
            return delagator.promptUser(info, remember);
        }
        
        @Override
        public String promptInput(String prompt, boolean echo) {
            return delagator.promptInput(prompt, echo);
        }
        
        @Override
        public void saveInfo(SessionInfo info) {
            delagator.saveInfo(info);
        }
    }
    
}

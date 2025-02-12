package tauon.app.ssh;

import tauon.app.exceptions.OperationCancelledException;
import tauon.app.settings.PortForwardingRule;
import tauon.app.settings.HopEntry;
import tauon.app.settings.SiteInfo;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public interface GuiHandle extends SSHCommandRunner.SudoPasswordPrompter {
    
    void reportException(Throwable cause);
    
    void reportPortForwardingFailed(PortForwardingRule portForwardingState, IOException e);
    
    BlockHandle blockUi(Object client, UserCancelHandle userCancelHandle);
    
    String promptUser(HopEntry info, AtomicBoolean remember);
    
    boolean promptReconnect(String name, String host);
    
    char[] promptPassword(HopEntry info, String user, AtomicBoolean remember, boolean isRetrying) throws OperationCancelledException;
    
    void showMessage(String name, String instruction);
    
    String promptInput(String prompt, boolean echo);
    
    void saveInfo(SiteInfo info);
    
    interface BlockHandle{
        void unblock();
    }
    
    interface UserCancelHandle {
        void userCancelled(BlockHandle blockHandle);
    }
    
    abstract class Delegate implements GuiHandle {
        
        private final GuiHandle delagator;
        
        public Delegate(GuiHandle delagator) {
            this.delagator = delagator;
        }
        
        @Override
        public void showMessage(String name, String instruction) {
            delagator.showMessage(name, instruction);
        }
        
        public void reportException(Throwable cause) {
            delagator.reportException(cause);
        }
        
        @Override
        public boolean promptReconnect(String name, String host) {
            return delagator.promptReconnect(name, host);
        }
        
        public char[] promptPassword(HopEntry info, String user, AtomicBoolean remember, boolean isRetrying) throws OperationCancelledException {
            return delagator.promptPassword(info, user, remember, isRetrying);
        }
        
        @Override
        public char[] promptSudoPassword(boolean isRetrying) throws OperationCancelledException {
            return delagator.promptSudoPassword(isRetrying);
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
        public void saveInfo(SiteInfo info) {
            delagator.saveInfo(info);
        }
    }
    
}

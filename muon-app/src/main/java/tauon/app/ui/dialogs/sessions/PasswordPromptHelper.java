package tauon.app.ui.dialogs.sessions;

import tauon.app.services.SessionService;
import tauon.app.exceptions.OperationCancelledException;

import javax.swing.*;
import java.awt.*;

import static tauon.app.services.LanguageService.getBundle;

public class PasswordPromptHelper implements SessionService.PasswordPromptConsumer {
    private final Component parent;
    
    public PasswordPromptHelper(Component parent) {
        this.parent = parent;
    }
    
    @Override
    public char[] promptMasterPassword(boolean creatingPassword, boolean promptTryAgain) throws OperationCancelledException {
        
        JPasswordField pass1 = new JPasswordField(30);
        
        if(creatingPassword){
            
            JPasswordField pass2 = new JPasswordField(30);
            
            while (JOptionPane.showOptionDialog(parent,
                    new Object[]{
                            getBundle().getString("dialog.master_password.new"), pass1,
                            getBundle().getString("dialog.master_password.reenter"), pass2
                    },
                    getBundle().getString("dialog.master_password.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null
            ) == JOptionPane.OK_OPTION) {
                
                char[] password1 = pass1.getPassword();
                char[] password2 = pass2.getPassword();
                
                String reason = "";
                
                boolean passwordOK = false;
                if (password1.length == password2.length && password1.length > 0) {
                    passwordOK = true;
                    for (int i = 0; i < password1.length; i++) {
                        if (password1[i] != password2[i]) {
                            passwordOK = false;
                            reason = getBundle().getString("dialog.master_password.no_match");
                            break;
                        }
                    }
                } else {
                    reason = getBundle().getString("dialog.master_password.no_match");
                }
                
                if (!passwordOK) {
                    JOptionPane.showMessageDialog(parent, reason);
                } else {
                    return password1;
                }
                
                pass1.setText("");
                pass2.setText("");
                
            }
            
        }else{
            
            Object[] content = promptTryAgain ? new Object[]{
                    getBundle().getString("dialog.master_password.incorrect_password"),
                    getBundle().getString("dialog.master_password.enter"),
                    pass1,
            } : new Object[]{
                    getBundle().getString("dialog.master_password.enter"), pass1,
            };
            
            if (JOptionPane.showOptionDialog(parent,
                    content,
                    getBundle().getString("dialog.master_password.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null
            ) == JOptionPane.OK_OPTION) {
                return pass1.getPassword();
            }
            
        }
        
        throw new OperationCancelledException();
        
    }
}

package tauon.app.ui.utils;

import tauon.app.services.LanguageService;

import javax.swing.*;
import java.awt.*;

import static tauon.app.services.LanguageService.getBundle;

public class AlertDialogUtils {
    
    public static void showError(Component parent, Object message){
        JOptionPane.showMessageDialog(
                parent,
                message,
                LanguageService.getBundle().getString("utils.alert.show_error.title"),
                JOptionPane.ERROR_MESSAGE
        );
    }
    
    public static void showSuccess(Component parent, Object message){
        JOptionPane.showMessageDialog(
                parent,
                message,
                LanguageService.getBundle().getString("utils.alert.show_success.title"),
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    public static void showInfo(Component parent, Object message){
        JOptionPane.showMessageDialog(
                parent,
                message,
                LanguageService.getBundle().getString("utils.alert.show_info.title"),
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    public static String promptString(Component parent, String title, String message, OnEmpty onEmpty) {
        
        JTextField pass2 = new JTextField(30);
        
        while (JOptionPane.showOptionDialog(parent,
                new Object[]{
                        message, pass2,
                },
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null
        ) == JOptionPane.OK_OPTION) {
            
            String text = pass2.getText();
            if(text.isEmpty()){
                switch (onEmpty){
                    case CANCEL:
                        return null;
                    case ASK_AGAIN:
                        showError(
                                parent,
                                LanguageService.getBundle().getString("utils.alert.prompt_text.error_cannot_be_empty")
                        );
                        break;
                    case RETURN:
                        return text;
                }
            }else{
                return text;
            }
        }
        
        return null;
    }
    
    public enum OnEmpty {
        CANCEL,
        ASK_AGAIN,
        RETURN
    }
}

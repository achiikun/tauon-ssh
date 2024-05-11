package tauon.app.ui.utils;

import tauon.app.services.LanguageService;

import javax.swing.*;
import java.awt.*;

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
    
}

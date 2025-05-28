package tauon.app.services;

import java.util.Locale;
import java.util.ResourceBundle;

import static tauon.app.util.misc.Constants.PATH_MESSAGES_FILE;

public class LanguageService {
    private static ResourceBundle bundle;
    
    public static ResourceBundle getBundle(){
        if(bundle == null) {
            Locale locale = new Locale.Builder().setLanguage("en").build();
            bundle = ResourceBundle.getBundle(PATH_MESSAGES_FILE, locale);
        }
        
        return bundle;
    }
    
}

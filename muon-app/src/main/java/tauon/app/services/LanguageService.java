package tauon.app.services;

import tauon.app.settings.Settings;
import util.Language;

import java.util.Locale;
import java.util.ResourceBundle;

import static util.Constants.PATH_MESSAGES_FILE;

public class LanguageService {
    private static ResourceBundle bundle;
    
    public static ResourceBundle getBundle(){
        if(bundle == null) {
            Settings settings = SettingsService.getSettings();
            
            Language language = Language.ENGLISH;
            if (settings.getLanguage() != null) {
                language = settings.getLanguage();
            }
            
            Locale locale = new Locale.Builder().setLanguage(language.getLangAbbr()).build();
            bundle = ResourceBundle.getBundle(PATH_MESSAGES_FILE, locale);
        }
        
        return bundle;
    }
    
}

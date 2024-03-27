package tauon.app;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.InitializationException;
import tauon.app.services.ConfigFilesService;
import tauon.app.services.SettingsService;
import tauon.app.ui.containers.main.AppWindow;
import tauon.app.ui.containers.main.GraphicalHostKeyVerifier;
import tauon.app.ui.dialogs.settings.SettingsPageName;
import tauon.app.ui.laf.AppSkin;
import tauon.app.ui.laf.AppSkinDark;
import tauon.app.ui.laf.AppSkinLight;
import tauon.app.util.externaleditor.ExternalEditorHandler;
import tauon.app.util.misc.PlatformUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static tauon.app.services.LanguageService.getBundle;
import static tauon.app.services.SettingsService.getSettings;
import static tauon.app.util.misc.Constants.CONFIG_DIR;

/**
 * Hello world!
 */
public class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    
    public static final String APP_INSTANCE_ID = UUID.randomUUID().toString();
    
    public static AppSkin skin;
    
//    private static Settings settings;
    private static ExternalEditorHandler externalEditorHandler;
//    private static AppWindow mw;

    static {
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("log4j.debug", "true");
        
    }
    
    private App(){
    
    }

    public static void main(String[] args) throws UnsupportedLookAndFeelException, IOException, InitializationException {
        
        LOG.info("Hello!");
        LOG.debug("Java version : ".concat(System.getProperty("java.version")));

//        if (Boolean.parseBoolean(System.getProperty("debugMuon"))) {
//            Logger.getRootLogger().setLevel(Level.DEBUG);
//        }
        
        Security.addProvider(new BouncyCastleProvider());
        Security.setProperty("networkaddress.cache.ttl", "1");
        Security.setProperty("networkaddress.cache.negative.ttl", "1");
        Security.setProperty("crypto.policy", "unlimited");
        
        ConfigFilesService.getInstance().initialize();
        SettingsService.getInstance().initialize();
        getBundle();
        
//        validateCustomMuonPath();
//        boolean importOnFirstRun = validateConfigPath();

//        setBundleLanguage();
//        loadSettings();
        
//        if (importOnFirstRun) {
//            SessionExportImport.importOnFirstRun();
        
        /*File appDir = new File(CONFIG_DIR);
       if (!appDir.exists()) {
            //Validate if the config directory can be created
            if(!appDir.mkdirs()){
                System.err.println("The config directory for muon cannot be created: "+ CONFIG_DIR);
                System.exit(1);
            }
            firstRun = true;
            
        }*/
        
        if (getSettings().isManualScaling()) {
            System.setProperty("sun.java2d.uiScale.enabled", "true");
            System.setProperty("sun.java2d.uiScale", String.format("%.2f", getSettings().getUiScaling()));
        }
        
        if (getSettings().getEditors().isEmpty()) {
            LOG.info("Searching for known editors...");
            SettingsService.getInstance().setAndSave(
                    settings -> settings.setEditors(PlatformUtils.getKnownEditors())
            );
            LOG.info("Searching for known editors...done");
        }
        
        // TODO out of here
        skin = getSettings().isUseGlobalDarkTheme() ? new AppSkinDark() : new AppSkinLight();
        UIManager.setLookAndFeel(skin.getLaf());
        
        validateMaxKeySize();
        
        // JediTerm seems to take a long time to load, this might make UI more
        // responsive
        App.EXECUTOR.submit(() -> {
            try {
                Class.forName("com.jediterm.terminal.ui.JediTermWidget");
            } catch (ClassNotFoundException e) {
                LOG.error(e.getMessage(), e);
            }
        });
        
        AppWindow mw = new AppWindow();
        externalEditorHandler = new ExternalEditorHandler(mw);
        SwingUtilities.invokeLater(() -> mw.setVisible(true));

        mw.createFirstSessionPanel();
    }

    private static void validateMaxKeySize() {
        try {
            int maxKeySize = javax.crypto.Cipher.getMaxAllowedKeyLength("AES");
            LOG.info("maxKeySize: " + maxKeySize);
            if (maxKeySize < Integer.MAX_VALUE) {
                JOptionPane.showMessageDialog(null, getBundle().getString("unlimited_cryptography"));
            }
        } catch (NoSuchAlgorithmException e1) {
            LOG.error(e1.getMessage(), e1);
        }
    }

    /**
     * @return the externalEditorHandler
     */
    public static ExternalEditorHandler getExternalEditorHandler() {
        return externalEditorHandler;
    }

//    public static synchronized void openSettings(SettingsPageName page) {
//        mw.openSettings(page);
//    }

//    public static synchronized AppWindow getAppWindow() {
//        return mw;
//    }

}

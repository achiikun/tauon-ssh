package tauon.app;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.InitializationException;
import tauon.app.services.ConfigFilesService;
import tauon.app.services.SettingsService;
import tauon.app.ui.containers.main.AppWindow;
import tauon.app.ui.laf.AppSkin;
import tauon.app.ui.laf.AppSkinDark;
import tauon.app.ui.laf.AppSkinLight;
import tauon.app.util.externaleditor.ExternalEditorHandler;
import tauon.app.util.misc.PlatformUtils;

import javax.swing.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static tauon.app.services.LanguageService.getBundle;
import static tauon.app.services.SettingsService.getSettings;

/**
 * Hello world!
 */
public class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    
    public static final String APP_INSTANCE_ID = UUID.randomUUID().toString();
    
    public static AppSkin skin;
    
    private static ExternalEditorHandler externalEditorHandler;

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
                JOptionPane.showMessageDialog(null, getBundle().getString("app.ui.message.unlimited_cryptography_not_enabled"));
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

}

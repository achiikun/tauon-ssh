package tauon.app.settings.importers;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.settings.SiteInfo;
import tauon.app.ui.utils.AlertDialogUtils;
import tauon.app.util.misc.RegUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tauon.app.services.LanguageService.getBundle;


public class PuttyImporter {
    private static final Logger LOG = LoggerFactory.getLogger(PuttyImporter.class);
    
    private static final String PUTTY_REG_KEY = "Software\\SimonTatham\\PuTTY\\Sessions";

    public static Map<String, String> getKeyNames(Component parent) throws AlreadyFailedException {
        Map<String, String> map = new HashMap<>();
        try {
            String[] keys = Advapi32Util
                    .registryGetKeys(WinReg.HKEY_CURRENT_USER, PUTTY_REG_KEY);
            for (String key : keys) {
                String decodedKey = key.replace("%20", " ");
                map.put(key, decodedKey);
            }
        } catch (Throwable e) {
            LOG.error("Error while reading sessions from Putty.", e);
            AlertDialogUtils.showError(parent, getBundle().getString("app.sessions.message.error_reading_putty"));
            throw new AlreadyFailedException();
        }

        return map;
    }

    public static void importSessions(DefaultMutableTreeNode node,
                                      List<String> keys) {

        for (String key : keys) {
            if ("ssh".equals(RegUtil.regGetStr(WinReg.HKEY_CURRENT_USER,
                    PUTTY_REG_KEY + "\\" + key, "Protocol"))) {
                String host = RegUtil.regGetStr(WinReg.HKEY_CURRENT_USER,
                        PUTTY_REG_KEY + "\\" + key, "HostName");
                int port = RegUtil.regGetInt(WinReg.HKEY_CURRENT_USER,
                        PUTTY_REG_KEY + "\\" + key, "PortNumber");
                String user = RegUtil.regGetStr(WinReg.HKEY_CURRENT_USER,
                        PUTTY_REG_KEY + "\\" + key, "UserName");
                String keyfile = RegUtil.regGetStr(WinReg.HKEY_CURRENT_USER,
                        PUTTY_REG_KEY + "\\" + key, "PublicKeyFile");

                String proxyHost = RegUtil.regGetStr(WinReg.HKEY_CURRENT_USER,
                        PUTTY_REG_KEY + "\\" + key, "ProxyHost");
                int proxyPort = RegUtil.regGetInt(WinReg.HKEY_CURRENT_USER,
                        PUTTY_REG_KEY + "\\" + key, "ProxyPort");
                String proxyUser = RegUtil.regGetStr(WinReg.HKEY_CURRENT_USER,
                        PUTTY_REG_KEY + "\\" + key, "ProxyUsername");

                String proxyPass = RegUtil.regGetStr(WinReg.HKEY_CURRENT_USER,
                        PUTTY_REG_KEY + "\\" + key, "ProxyPassword");

                int proxyType = RegUtil.regGetInt(WinReg.HKEY_CURRENT_USER,
                        PUTTY_REG_KEY + "\\" + key, "ProxyMethod");
                if (proxyType == 1) {
                    proxyType = 2;
                } else if (proxyType == 2) {
                    proxyType = 3;
                } else if (proxyType == 3) {
                    proxyType = 1;
                } else {
                    proxyType = 0;
                }
                SiteInfo info = new SiteInfo();
                info.setName(key);
                info.setHost(host);
                info.setPort(port);
                info.setUser(user);
                info.setPrivateKeyFile(keyfile);
                info.setProxyHost(proxyHost);
                info.setProxyPort(proxyPort);
                info.setProxyUser(proxyUser);
                info.setProxyPassword(proxyPass);
                info.setProxyType(proxyType);

                DefaultMutableTreeNode node1 = new DefaultMutableTreeNode(info);
                node1.setAllowsChildren(false);
                node.add(node1);
            }
        }
    }

}

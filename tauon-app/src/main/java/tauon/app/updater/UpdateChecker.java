package tauon.app.updater;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.markusbernhardt.proxy.ProxySearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.ui.dialogs.sessions.TreeTransferHandler;
import tauon.app.util.misc.Constants;

import java.net.ProxySelector;
import java.net.URL;

import static tauon.app.util.misc.Constants.API_UPDATE_URL;

public class UpdateChecker {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class);

    static {
        CertificateValidator.registerCertificateHook();
    }

    public static boolean isNewUpdateAvailable() {
        try {
            ProxySearch proxySearch = ProxySearch.getDefaultProxySearch();
            ProxySelector myProxySelector = proxySearch.getProxySelector();

            ProxySelector.setDefault(myProxySelector);

            System.out.println("Checking for url");
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            VersionEntry latestRelease = objectMapper.readValue(new URL(API_UPDATE_URL).openStream(),
                    new TypeReference<VersionEntry>() {
                    });
            System.out.println("Latest release: " + latestRelease);
            return latestRelease.compareTo(Constants.VERSION) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

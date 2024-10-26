package tauon.app.util.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificateValidator {
    
    private static final Logger LOG = LoggerFactory.getLogger(CertificateValidator.class);
    
    public interface TrustQuestion {
        boolean trust(X509Certificate[] chain, String authType);
    }
    
    public static synchronized boolean registerCertificateHook(TrustQuestion trustQuestion) {
        SSLContext sslContext = null;
        try {
            try {
                sslContext = SSLContext.getInstance("TLS");
            } catch (Exception e) {
                LOG.error("Could not set TLS protocol. Defaulting to SSL.", e);
                sslContext = SSLContext.getInstance("SSL");
            }
        } catch (Exception e) {
            LOG.error("Exception while setting SSL.", e);
            return false;
        }
        
        TrustManager[] trustAllCerts = new TrustManager[]{new X509ExtendedTrustManager() {
            
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            
            }
            
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                try {
                    for (X509Certificate cert : chain) {
                        cert.checkValidity();
                    }
                } catch (CertificateException e) {
                    LOG.error("Exception while validating certificate.", e);
                    if (!trustQuestion.trust(chain, authType)) {
                        throw e;
                    }
                }
            }
            
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                    throws CertificateException {
                
            }
            
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                    throws CertificateException {
                
            }
            
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                    throws CertificateException {
                try {
                    for (X509Certificate cert : chain) {
                        cert.checkValidity();
                    }
                } catch (CertificateException e) {
                    LOG.error("Exception while validating certificate.", e);
                    if (!trustQuestion.trust(chain, authType)) {
                        throw e;
                    }
                }
            }
            
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                    throws CertificateException {
                try {
                    for (X509Certificate cert : chain) {
                        cert.checkValidity();
                    }
                } catch (CertificateException e) {
                    LOG.error("Exception while validating certificate.", e);
                    if (!trustQuestion.trust(chain, authType)) {
                        throw e;
                    }
                }
            }
        }};
        try {
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (KeyManagementException e) {
            LOG.error("Exception while setting ssl context.", e);
            return false;
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        
        return true;
    }
    
}

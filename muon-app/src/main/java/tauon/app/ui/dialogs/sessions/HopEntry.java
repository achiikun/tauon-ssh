package tauon.app.ui.dialogs.sessions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import tauon.app.settings.NamedItem;

public class HopEntry extends NamedItem {
    private String id, host, user, password, keypath;
    private int port;

    public HopEntry(String id, String host, int port, String user, String password, String keypath) {
        super();
        this.id = id;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.keypath = keypath;
    }
    public HopEntry() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
    
    /**
     * @return the password
     */
    @JsonIgnore
    public String getPassword() {
        return password;
    }
    
    /**
     * @param password the password to set
     */
    @JsonIgnore
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * @return the privateKeyFile
     */
    public String getPrivateKeyFile() {
        return keypath;
    }
    
    /**
     * @param privateKeyFile the privateKeyFile to set
     */
    public void setPrivateKeyFile(String privateKeyFile) {
        this.keypath = privateKeyFile;
    }

    public String getKeypath() {
        return keypath;
    }

    public void setKeypath(String keypath) {
        this.keypath = keypath;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return host != null ? (user != null ? user + "@" + host : host) : "";
    }
}

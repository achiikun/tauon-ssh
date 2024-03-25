package tauon.app.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SessionInfo extends HopEntry implements Serializable {
    
    private String localFolder;
    private String remoteFolder;
    
    private List<String> favouriteRemoteFolders = new ArrayList<>();
    private List<String> favouriteLocalFolders = new ArrayList<>();

    private int proxyPort = 8080;
    private String proxyHost;
    private String proxyUser;
    private String proxyPassword; // TODO encrypt
    private int proxyType = 0;
    
    private boolean XForwardingEnabled = false;
    
    private boolean useJumpHosts = false;
    private JumpType jumpType = JumpType.TcpForwarding;
    private List<HopEntry> jumpHosts = new ArrayList<>();
    private List<PortForwardingRule> portForwardingRules = new ArrayList<>();
    
    public SessionInfo(String id, String host, int port, String user, String password, String keypath) {
        super(id, host, port, user, password, keypath);
    }
    public SessionInfo() {
    
    }
    
    @Override
    public String toString() {
        return name;
    }

    /**
     * @return the localFolder
     */
    public String getLocalFolder() {
        return localFolder;
    }

    /**
     * @param localFolder the localFolder to set
     */
    public void setLocalFolder(String localFolder) {
        this.localFolder = localFolder;
    }

    /**
     * @return the remoteFolder
     */
    public String getRemoteFolder() {
        return remoteFolder;
    }

    /**
     * @param remoteFolder the remoteFolder to set
     */
    public void setRemoteFolder(String remoteFolder) {
        this.remoteFolder = remoteFolder;
    }

    /**
     * @return the favouriteFolders
     */
    public List<String> getFavouriteRemoteFolders() {
        return favouriteRemoteFolders;
    }

    /**
     * @param favouriteFolders the favouriteFolders to set
     */
    public void setFavouriteRemoteFolders(List<String> favouriteFolders) {
        this.favouriteRemoteFolders = favouriteFolders;
    }

    public SessionInfo copy() {
        SessionInfo info = (SessionInfo) super.copyTo(new SessionInfo());
        info.setLocalFolder(this.localFolder);
        info.setRemoteFolder(this.remoteFolder);
        
        info.getFavouriteRemoteFolders().addAll(favouriteRemoteFolders);
        info.getFavouriteLocalFolders().addAll(favouriteLocalFolders);
        
        info.setProxyPort(this.getProxyPort());
        info.setProxyHost(this.getProxyHost());
        info.setProxyUser(this.getProxyUser());
        info.setProxyPassword(this.getProxyPassword());
        info.setProxyType(this.getProxyType());
        
        info.setXForwardingEnabled(this.isXForwardingEnabled());
        
        info.setUseJumpHosts(this.isUseJumpHosts());
        getJumpHosts().stream().map(jh -> jh.copyTo(new HopEntry())).forEach(info.getJumpHosts()::add);
        
        getPortForwardingRules().stream()
                .map(jh -> jh.copyTo(new PortForwardingRule()))
                .forEach(info.getPortForwardingRules()::add);
        
        return info;
    }

    /**
     * @return the favouriteLocalFolders
     */
    public List<String> getFavouriteLocalFolders() {
        return favouriteLocalFolders;
    }

    /**
     * @param favouriteLocalFolders the favouriteLocalFolders to set
     */
    public void setFavouriteLocalFolders(List<String> favouriteLocalFolders) {
        this.favouriteLocalFolders = favouriteLocalFolders;
    }

    /**
     * @return the proxyPort
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * @param proxyPort the proxyPort to set
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * @return the proxyHost
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * @param proxyHost the proxyHost to set
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**
     * @return the proxyUser
     */
    public String getProxyUser() {
        return proxyUser;
    }

    /**
     * @param proxyUser the proxyUser to set
     */
    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    /**
     * @return the proxyPassword
     */
    public String getProxyPassword() {
        return proxyPassword;
    }

    /**
     * @param proxyPassword the proxyPassword to set
     */
    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    /**
     * @return the proxyType
     */
    public int getProxyType() {
        return proxyType;
    }

    /**
     * @param proxyType the proxyType to set
     */
    public void setProxyType(int proxyType) {
        this.proxyType = proxyType;
    }

    public boolean isUseJumpHosts() {
        return useJumpHosts;
    }

    public void setUseJumpHosts(boolean useJumpHosts) {
        this.useJumpHosts = useJumpHosts;
    }

    public JumpType getJumpType() {
        return jumpType;
    }

    public void setJumpType(JumpType jumpType) {
        this.jumpType = jumpType;
    }

    public List<HopEntry> getJumpHosts() {
        return jumpHosts;
    }

    public void setJumpHosts(List<HopEntry> jumpHosts) {
        this.jumpHosts = jumpHosts;
    }

    public List<PortForwardingRule> getPortForwardingRules() {
        return portForwardingRules;
    }

    public void setPortForwardingRules(List<PortForwardingRule> portForwardingRules) {
        this.portForwardingRules = portForwardingRules;
    }
    
    public boolean isXForwardingEnabled() {
        return XForwardingEnabled;
    }
    
    public void setXForwardingEnabled(boolean xForwardingEnabled) {
        this.XForwardingEnabled = xForwardingEnabled;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionInfo that = (SessionInfo) o;
        return Objects.equals(getHost(), that.getHost());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getHost(), this.getUser(), localFolder, remoteFolder, this.getPort(),
                favouriteRemoteFolders, favouriteLocalFolders, this.getPrivateKeyFile(),
                proxyPort, proxyHost, proxyUser, proxyPassword, proxyType,
                XForwardingEnabled, useJumpHosts, jumpType, jumpHosts, portForwardingRules,
                this.getPassword());
    }
    
    public String getSudoPassword() {
        // TODO ask user for password
        return getPassword();
    }
    
    public char[][] getHopPasswords() {
        List<char[]> pass = new ArrayList<>();
        for(HopEntry h: jumpHosts){
            String password = h.getPassword();
            pass.add(password == null || password.isBlank() ? null : password.toCharArray());
        }
        return pass.toArray(new char[0][]);
    }
    
    public enum JumpType {
        TcpForwarding, PortForwarding
    }
}

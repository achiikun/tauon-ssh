package tauon.app.ui.containers.session.pages.files.remote2remote;

import tauon.app.App;
import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ui.dialogs.sessions.NewSessionDlg;
import tauon.app.settings.SessionInfo;
import tauon.app.ui.containers.session.pages.files.FileBrowser;
import util.Constants;

import javax.swing.*;

public class LocalPipeTransfer {
    public void transferFiles(FileBrowser fileBrowser, String currentDirectory, FileInfo[] selectedFiles) {
//        SessionInfo info = new NewSessionDlg(App.getAppWindow()).newSession();
//        if (info != null) {
//            String path = JOptionPane.showInputDialog("Remote path");
//            if (path != null) {
//                TauonRemoteSessionInstance ri = new TauonRemoteSessionInstance(info, App.getInputBlocker(),
//                        new CachedCredentialProvider() {
//                            private char[] cachedPassword;
//                            private char[] cachedPassPhrase;
//                            private String cachedUser;
//
//                            @Override
//                            public synchronized char[] getCachedPassword() {
//                                return cachedPassword;
//                            }
//
//                            @Override
//                            public synchronized void cachePassword(char[] password) {
//                                this.cachedPassword = password;
//                            }
//
//                            @Override
//                            public synchronized char[] getCachedPassPhrase() {
//                                return cachedPassPhrase;
//                            }
//
//                            @Override
//                            public synchronized void setCachedPassPhrase(char[] cachedPassPhrase) {
//                                this.cachedPassPhrase = cachedPassPhrase;
//                            }
//
//                            @Override
//                            public synchronized String getCachedUser() {
//                                return cachedUser;
//                            }
//
//                            @Override
//                            public synchronized void setCachedUser(String cachedUser) {
//                                this.cachedUser = cachedUser;
//                            }
//                        });
//
//                SshFileSystem sshFS = ri.getSshFs();
//                fileBrowser.newFileTransfer(fileBrowser.getSSHFileSystem(), sshFS, selectedFiles, path, this.hashCode(),
//                        Constants.ConflictAction.PROMPT, null);
//            }
//        }
    }
}

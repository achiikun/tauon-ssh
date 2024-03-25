/**
 *
 */
package tauon.app.ui.containers.session.pages.utilpage.keys;

import tauon.app.ui.components.misc.TabbedPanel;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.containers.session.pages.utilpage.UtilPageItemView;

import javax.swing.*;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class KeyPage extends UtilPageItemView {
    private RemoteKeyPanel remoteKeyPanel;
    private LocalKeyPanel localKeyPanel;
    private TabbedPanel tabs;
    private SshKeyHolder keyHolder;

    /**
     *
     */
    public KeyPage(SessionContentPanel content) {
        super(content);
    }

    private void setKeyData(SshKeyHolder holder) {
        System.out.println("Holder: " + holder);
        this.localKeyPanel.setKeyData(holder);
        this.remoteKeyPanel.setKeyData(holder);
    }

    @Override
    protected void createUI() {
        keyHolder = new SshKeyHolder();
        tabs = new TabbedPanel();
        remoteKeyPanel = new RemoteKeyPanel(holder.getInfo(), a -> {
            holder.disableUi();
            holder.executor.submit(() -> {
                try {
                    SshKeyManager.generateKeys(keyHolder,
                            holder.getRemoteSessionInstance(), false);
                    SwingUtilities.invokeLater(() -> setKeyData(keyHolder));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    holder.enableUi();
                }
            });
        }, a -> {
            holder.disableUi();
            holder.executor.submit(() -> {
                try {
                    keyHolder = SshKeyManager.getKeyDetails(holder);
                    SwingUtilities.invokeLater(() -> setKeyData(keyHolder));

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    holder.enableUi();
                }
            });
        }, a -> {
            holder.disableUi();
            holder.executor.submit(() -> {
                try {
                    SshKeyManager.saveAuthorizedKeysFile(a,
                            holder.getRemoteSessionInstance().getSshFs());
                    keyHolder = SshKeyManager.getKeyDetails(holder);
                    SwingUtilities.invokeLater(() -> setKeyData(keyHolder));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    holder.enableUi();
                }
            });
        });
        localKeyPanel = new LocalKeyPanel(holder.getInfo(), a -> {
            holder.disableUi();
            holder.executor.submit(() -> {
                try {
                    SshKeyManager.generateKeys(keyHolder,
                            holder.getRemoteSessionInstance(), true);
                    SwingUtilities.invokeLater(() -> setKeyData(keyHolder));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    holder.enableUi();
                }
            });
        }, a -> {
            holder.disableUi();
            holder.executor.submit(() -> {
                try {
                    keyHolder = SshKeyManager.getKeyDetails(holder);
                    SwingUtilities.invokeLater(() -> setKeyData(keyHolder));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    holder.enableUi();
                }
            });
        });
        tabs.addTab(getBundle().getString("server"), remoteKeyPanel);
        tabs.addTab(getBundle().getString("local_computer"), localKeyPanel);
        this.add(tabs);

        holder.executor.submit(() -> {
            holder.disableUi();
            try {
                keyHolder = SshKeyManager.getKeyDetails(holder);
                SwingUtilities.invokeLater(() -> setKeyData(keyHolder));
            } catch (Exception err) {
                err.printStackTrace();
            } finally {
                holder.enableUi();
            }
        });
    }

    @Override
    protected void onComponentVisible() {

    }

    @Override
    protected void onComponentHide() {

    }
}

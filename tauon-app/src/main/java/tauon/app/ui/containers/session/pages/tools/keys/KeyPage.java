/**
 *
 */
package tauon.app.ui.containers.session.pages.tools.keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.ui.components.misc.TabbedPanel;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.components.page.subpage.Subpage;

import javax.swing.*;

import java.lang.reflect.InvocationTargetException;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class KeyPage extends Subpage {
    private static final Logger LOG = LoggerFactory.getLogger(KeyPage.class);
    
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
            holder.submitSSHOperation((guiHandle, instance) -> {
                SshKeyManager.generateKeys(keyHolder, instance, false);
                try {
                    SwingUtilities.invokeAndWait(() -> setKeyData(keyHolder));
                } catch (InvocationTargetException e) {
                    LOG.error("Exception while rendering results.", e);
                }
            });
        }, a -> {
            holder.submitSSHOperation((guiHandle, instance) -> {
                keyHolder = SshKeyManager.getKeyDetails(holder, instance);
                try {
                    SwingUtilities.invokeAndWait(() -> setKeyData(keyHolder));
                } catch (InvocationTargetException e) {
                    LOG.error("Exception while rendering results.", e);
                }
            });
        }, a -> {
            holder.submitSSHOperation((guiHandle, instance) -> {
                    SshKeyManager.saveAuthorizedKeysFile(a, instance.getSshFileSystem());
                    keyHolder = SshKeyManager.getKeyDetails(holder, instance);
                try {
                    SwingUtilities.invokeAndWait(() -> setKeyData(keyHolder));
                } catch (InvocationTargetException e) {
                    LOG.error("Exception while rendering results.", e);
                }
            });
        });
        localKeyPanel = new LocalKeyPanel(holder.getInfo(), a -> {
            holder.submitSSHOperation((guiHandle, instance) -> {
                    SshKeyManager.generateKeys(keyHolder, instance, true);
                try {
                    SwingUtilities.invokeAndWait(() -> setKeyData(keyHolder));
                } catch (InvocationTargetException e) {
                    LOG.error("Exception while rendering results.", e);
                }
            });
        }, a -> {
            holder.submitSSHOperation((guiHandle, instance) -> {
                    keyHolder = SshKeyManager.getKeyDetails(holder, instance);
                try {
                    SwingUtilities.invokeAndWait(() -> setKeyData(keyHolder));
                } catch (InvocationTargetException e) {
                    LOG.error("Exception while rendering results.", e);
                }
            });
        });
        tabs.addTab(getBundle().getString("app.tools_ssh_keys.label.server"), remoteKeyPanel);
        tabs.addTab(getBundle().getString("app.tools_ssh_keys.label.local_computer"), localKeyPanel);
        this.add(tabs);

        holder.submitSSHOperation((guiHandle, instance) -> {
            keyHolder = SshKeyManager.getKeyDetails(holder, instance);
            try {
                SwingUtilities.invokeAndWait(() -> setKeyData(keyHolder));
            } catch (InvocationTargetException e) {
                LOG.error("Exception while rendering results.", e);
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

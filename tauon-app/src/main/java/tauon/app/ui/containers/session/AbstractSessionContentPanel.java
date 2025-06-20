/**
 *
 */
package tauon.app.ui.containers.session;

import com.intellij.util.ui.UIUtil;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.TauonOperationException;
import tauon.app.services.SitesConfigManager;
import tauon.app.settings.HopEntry;
import tauon.app.settings.SiteInfo;
import tauon.app.ssh.GuiHandle;
import tauon.app.ssh.ILocalOperator;
import tauon.app.ssh.IStopper;
import tauon.app.ui.components.glasspanes.SessionInputBlocker;
import tauon.app.ui.components.misc.SkinnedTextField;
import tauon.app.ui.components.misc.TabbedPage;
import tauon.app.ui.components.page.Page;
import tauon.app.ui.components.page.PageHolder;
import tauon.app.ui.containers.main.AppWindow;
import tauon.app.ui.dialogs.sessions.PasswordPromptHelper;
import tauon.app.ui.utils.AlertDialogUtils;
import tauon.app.util.misc.FormatUtils;
import tauon.app.util.misc.LayoutUtilities;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static tauon.app.services.LanguageService.getBundle;

public abstract class AbstractSessionContentPanel extends JPanel implements PageHolder, GuiHandle {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSessionContentPanel.class);
    
    public final ExecutorService executor = Executors.newSingleThreadExecutor();
    public final AppWindow appWindow;
    
    public final UUID uuid = UUID.randomUUID();
    
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final JRootPane rootPane;
    private final JPanel contentPane;
    private final SessionInputBlocker sessionInputBlocker = new SessionInputBlocker();

//    private final ProgressGlasspane progressPanel = new ProgressGlasspane();
    
    private TabbedPage[] pages;
    
    /**
     *
     */
    public AbstractSessionContentPanel(AppWindow appWindow) {
        super(new BorderLayout());
        this.appWindow = appWindow;
        
        this.cardLayout = new CardLayout();
        this.cardPanel = new JPanel(this.cardLayout);
        
        this.contentPane = new JPanel(new BorderLayout(), true);
        this.rootPane = new JRootPane();
    }
    
    protected void createUi() {
        
        Box contentTabs = Box.createHorizontalBox();
        contentTabs.setBorder(new MatteBorder(0, 0, 1, 0, App.skin.getDefaultBorderColor()));
        
        Page[] pageArr = createPages();
        
        this.pages = new TabbedPage[pageArr.length];
        for (int i = 0; i < pageArr.length; i++) {
            TabbedPage tabbedPage = new TabbedPage(pageArr[i], this);
            this.pages[i] = tabbedPage;
            this.cardPanel.add(tabbedPage.getPage(), tabbedPage.getId());
            pageArr[i].putClientProperty("pageId", tabbedPage.getId());
        }
        
        LayoutUtilities.equalizeSize(this.pages);
        
        for (TabbedPage item : this.pages) {
            contentTabs.add(item);
        }
        
        contentTabs.add(Box.createHorizontalGlue());
        
        this.contentPane.add(contentTabs, BorderLayout.NORTH);
        this.contentPane.add(this.cardPanel);
        
        this.rootPane.setContentPane(this.contentPane);
        
        this.add(this.rootPane);
        
        showPage(this.pages[0].getId());
        
    }
    
    protected abstract Page[] createPages();
    
    public AppWindow getAppWindow() {
        return appWindow;
    }
    
    @Override
    public void showPage(String pageId) {
        TabbedPage selectedPage = null;
        for (TabbedPage item : this.pages) {
            if (pageId.equals(item.getId())) {
                selectedPage = item;
            }
            item.setSelected(false);
        }
        assert selectedPage != null : "Page Id not existing.";
        selectedPage.setSelected(true);
        this.cardLayout.show(this.cardPanel, pageId);
        this.revalidate();
        this.repaint();
        selectedPage.getPage().onLoad();
    }
    
    protected void disableUi() throws InterruptedException, InvocationTargetException {
        UIUtil.invokeAndWaitIfNeeded(() -> {
            this.sessionInputBlocker.startAnimation(null);
            this.rootPane.setGlassPane(this.sessionInputBlocker);
            this.sessionInputBlocker.setVisible(true);
        });
    }
    
    protected void disableUi(IStopper.Handle stopFlag) throws InterruptedException, InvocationTargetException {
        UIUtil.invokeAndWaitIfNeeded(() -> {
            this.sessionInputBlocker.startAnimation(stopFlag);
            this.rootPane.setGlassPane(this.sessionInputBlocker);
            this.sessionInputBlocker.setVisible(true);
        });
    }
    
    protected void enableUi() {
        UIUtil.invokeLaterIfNeeded(() -> {
            this.sessionInputBlocker.stopAnimation();
            this.sessionInputBlocker.setVisible(false);
        });
    }
    
    @Override
    public void reportException(Throwable cause) {
    
    }
    
    @Override
    public boolean promptReconnect(String name, String host) {
        return JOptionPane.showConfirmDialog(appWindow,
                FormatUtils.$$(
                        getBundle().getString("app.session.message.unable_to_connect_retry"),
                        Map.of(
                                "SERVER_NAME", name,
                                "SERVER_HOST", host
                        )
                )
        ) != JOptionPane.YES_OPTION;
    }
    
    @Override
    public char[] promptPassword(HopEntry info, String user, AtomicBoolean rememberPassword, boolean isRetrying) throws OperationCancelledException {
        JPasswordField passwordField = new JPasswordField(30);
        int ret;
        if (rememberPassword != null) {
            JCheckBox rememberCheckBox = new JCheckBox(getBundle().getString("app.session.action.remember_password"));
            ret = JOptionPane.showOptionDialog(
                    appWindow,
                    new Object[]{
                            FormatUtils.$$(
                                    getBundle().getString("app.session.type_password_for_user.message"),
                                    Map.of(
                                            "USER", user
                                    )
                            ),
                            passwordField,
                            rememberCheckBox
                    },
                    getBundle().getString("app.session.type_password_for_user.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null
            );
            rememberPassword.set(rememberCheckBox.isSelected());
        } else {
            ret = JOptionPane.showOptionDialog(
                    appWindow,
                    new Object[]{
                            FormatUtils.$$(
                                    getBundle().getString("app.session.type_password_for_user.message"),
                                    Map.of(
                                            "USER", user
                                    )
                            ),
                            passwordField
                    },
                    getBundle().getString("app.session.type_password_for_user.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null
            );
        }
        
        if (ret == JOptionPane.OK_OPTION) {
            return passwordField.getPassword();
        }
        
        throw new OperationCancelledException();
    }
    
    @Override
    public String promptUser(HopEntry info, AtomicBoolean remember) {
        
        JTextField txtUser = new SkinnedTextField(30);
        JCheckBox chkCacheUser = new JCheckBox(getBundle().getString("app.session.action.remember_username"));
        try {
            int ret = UIUtil.invokeAndWaitIfNeeded(() ->
                    JOptionPane.showOptionDialog(
                            appWindow,
                            new Object[]{getBundle().getString("app.ui.label.username"), txtUser, chkCacheUser},
                            getBundle().getString("app.ui.label.user"),
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            null
                    )
            );
            
            if (ret == JOptionPane.OK_OPTION) {
                String user = txtUser.getText();
                if (chkCacheUser.isSelected()) {
                    remember.set(true);
                }
                return user;
            }
            
            return null;
            
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    public UUID getUUID() {
        return uuid;
    }
    
    public void submitLocalOperation(ILocalOperator consumer) {
        executor.submit(() -> {
            
            try {
                disableUi();
            } catch (InterruptedException | InvocationTargetException e) {
                LOG.error("Error while disabling the Ui. This error should never be thrown.", e);
            }
            
            try {
                consumer.operate();
            } catch (OperationCancelledException | AlreadyFailedException | InterruptedException ignored) {
                // Do nothing
            } catch (TauonOperationException exception) {
                LOG.error("Going to show the exception to the user.", exception);
                AlertDialogUtils.showError(this, exception.getUserMessage());
            } finally {
                enableUi();
            }
        });
    }
    
    public void runLocalOperation(ILocalOperator consumer) {
        
        try {
            consumer.operate();
        } catch (OperationCancelledException | AlreadyFailedException | InterruptedException ignored) {
            // Do nothing
        } catch (TauonOperationException exception) {
            LOG.error("Going to show the exception to the user.", exception);
            AlertDialogUtils.showError(this, exception.getUserMessage());
        }
        
    }
    
    public class MyPasswordFinder implements PasswordFinder {
        
        @Override
        public char[] reqPassword(Resource<?> resource) {
            try {
                return UIUtil.invokeAndWaitIfNeeded(this::showReqPasswordDialog, resource);
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        
        private boolean retryPassword = true;
        
        public char[] showReqPasswordDialog(Resource<?> resource) {
            JPasswordField txtPass = new JPasswordField();
//        JCheckBox chkUseCache = new JCheckBox(getBundle().getString("remember_session"));
            
            // TODO i18n
            int ret = JOptionPane.showOptionDialog(AbstractSessionContentPanel.this,
                    new Object[]{resource != null ? resource.toString() : "Private key passphrase:", txtPass},//, chkUseCache},
                    "Passphrase", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (ret == JOptionPane.OK_OPTION) {
                return txtPass.getPassword();
            } else {
                retryPassword = false;
            }
            
            return null;
        }
        
        @Override
        public boolean shouldRetry(Resource<?> resource) {
            boolean old = retryPassword;
            retryPassword = true;
            return old;
        }
        
    }
    
    @Override
    public void showMessage(String name, String instruction) {
        JOptionPane.showMessageDialog(this, instruction, name, JOptionPane.PLAIN_MESSAGE);
    }
    
    @Override
    public boolean promptConfirmation(String name, String instruction) {
        int r = JOptionPane.showConfirmDialog(this, instruction, name, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        return r == JOptionPane.OK_OPTION;
    }
    
    @Override
    public String promptInput(String prompt, boolean echo) {
        if (echo) {
            return JOptionPane.showInputDialog(this, prompt);
        } else {
            JPasswordField passwordField = new JPasswordField(30);
            int ret = JOptionPane.showOptionDialog(
                    this,
                    new Object[]{prompt, passwordField},
                    "Input", // TODO i18n
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null
            );
            if (ret == JOptionPane.OK_OPTION) {
                return String.valueOf(passwordField.getPassword());
            }
            return null;
        }
    }
    
    @Override
    public void saveInfo(SiteInfo info) {
        SitesConfigManager.getInstance().setPasswordsFrom(info);
        try {
            SitesConfigManager.getInstance().save(new PasswordPromptHelper(this));
        } catch (OperationCancelledException ignored) {
        
        }
    }
    
    public abstract void closeAsync(Consumer<Boolean> onClosed);
    
}

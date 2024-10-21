/**
 *
 */
package tauon.app.ui.containers.session.pages.logviewer;

import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ui.components.closabletabs.ClosableTabbedPanel;
import tauon.app.ui.components.closabletabs.ClosableTabbedPanel.TabTitle;
import tauon.app.ui.components.misc.SkinnedTextField;
import tauon.app.ui.components.page.Page;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.components.misc.FontAwesomeContants;
import tauon.app.util.misc.PathUtils;

import javax.swing.*;
import java.util.LinkedHashSet;
import java.util.Set;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class LogViewer extends Page {
    private final ClosableTabbedPanel tabs;
    private final StartPage startPage;
    private final JPanel content;
    private final SessionContentPanel sessionContent;
    private final Set<String> openLogs = new LinkedHashSet<>();

    /**
     *
     */
    public LogViewer(SessionContentPanel sessionContent) {
        this.sessionContent = sessionContent;
        startPage = new StartPage(this::openLog, sessionContent.getInfo().getId());
        content = new JPanel();
        tabs = new ClosableTabbedPanel(e -> {
            String path = promptLogPath();
            if (path != null) {
                openLog(path);
            }
        });

        TabTitle tabTitle = new TabTitle();
        tabs.addTab(tabTitle, startPage);
        this.add(tabs);
        tabTitle.getCallback().accept("Pinned logs");
    }

    @Override
    public void onLoad() {

    }

    @Override
    public String getIcon() {
        return FontAwesomeContants.FA_STICKY_NOTE;
    }

    @Override
    public String getText() {
        return getBundle().getString("app.logs.title");
    }

    public void openLog(FileInfo remotePath) {
        openLog(remotePath.getPath());
    }

    public void openLog(String remotePath) {
        if (openLogs.contains(remotePath)) {
            int index = 0;
            for (String logPath : openLogs) {
                if (logPath.equals(remotePath)) {
                    tabs.setSelectedIndex(index + 1);
                    return;
                }
                index++;
            }
        }
        LogContent logContent = new LogContent(sessionContent, remotePath,
                startPage, e -> openLogs.remove(remotePath));
        TabTitle title = new TabTitle();
        tabs.addTab(title, logContent);
        title.getCallback().accept(PathUtils.getFileName(remotePath));
        openLogs.add(remotePath);
    }

    private String promptLogPath() {
        JTextField txt = new SkinnedTextField(30);
        if (JOptionPane.showOptionDialog(this,
                new Object[]{"Please provide full path of the log file",
                        txt},
                "Input", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, null,
                null) == JOptionPane.OK_OPTION && txt.getText().length() > 0) {
            return txt.getText();
        }
        return null;
    }

}

/**
 *
 */
package tauon.app.ui.containers.session.pages.info.sysinfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.components.misc.SkinnedTextArea;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.components.page.subpage.Subpage;
import tauon.app.ui.containers.session.pages.logviewer.LogContent;
import tauon.app.util.misc.ScriptLoader;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author subhro
 *
 */
public class SysInfoPanel extends Subpage {
    
    private static final Logger LOG = LoggerFactory.getLogger(SysInfoPanel.class);

    private JTextArea textArea;

    public SysInfoPanel(SessionContentPanel holder) {
        super(holder);
    }

    @Override
    protected void createUI() {
        textArea = new SkinnedTextArea();
        textArea.setFont(new Font("Noto Mono", Font.PLAIN, 14));
        JScrollPane scrollPane = new SkinnedScrollPane(textArea);
        this.add(scrollPane);

        AtomicBoolean stopFlag = new AtomicBoolean(false);
//        holder.disableUi(stopFlag);
        holder.submitSSHOperationStoppable(instance -> {
//            try {
                StringBuilder output = new StringBuilder();
                int ret = instance.exec(ScriptLoader.loadShellScript("/scripts/linux-sysinfo.sh"), stopFlag, output);
                if (ret == 0) {
                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            textArea.setText(output.toString());
                            textArea.setCaretPosition(0);
                        });
                    } catch (InvocationTargetException e) {
                        LOG.error("Exception while rendering info.", e);
                    }
                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                holder.enableUi();
//            }
        }, stopFlag);
    }

    @Override
    protected void onComponentVisible() {

    }

    @Override
    protected void onComponentHide() {

    }
}

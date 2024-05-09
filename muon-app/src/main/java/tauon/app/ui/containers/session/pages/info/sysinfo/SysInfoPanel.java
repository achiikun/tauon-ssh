/**
 *
 */
package tauon.app.ui.containers.session.pages.info.sysinfo;

import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.components.misc.SkinnedTextArea;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.components.page.subpage.Subpage;
import tauon.app.util.misc.ScriptLoader;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author subhro
 *
 */
public class SysInfoPanel extends Subpage {
    /**
     *
     */

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
                int ret = instance.exec(
                                ScriptLoader.loadShellScript(
                                        "/scripts/linux-sysinfo.sh"),
                                stopFlag, output);
                if (ret == 0) {
                    SwingUtilities.invokeAndWait(() -> {
                        textArea.setText(output.toString());
                        textArea.setCaretPosition(0);
                    });
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

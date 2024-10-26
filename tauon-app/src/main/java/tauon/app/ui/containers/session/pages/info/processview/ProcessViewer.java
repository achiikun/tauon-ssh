/**
 *
 */
package tauon.app.ui.containers.session.pages.info.processview;

import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.RemoteOperationException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ui.components.page.subpage.Subpage;
import tauon.app.ui.components.tablerenderers.ByteCountValue;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.util.misc.ScriptLoader;
import tauon.app.util.ssh.SudoUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class ProcessViewer extends Subpage {
    private final AtomicBoolean processListLoaded = new AtomicBoolean(false);
    private ProcessListPanel processListPanel;

    /**
     *
     */
    public ProcessViewer(SessionContentPanel holder) {
        super(holder);
    }

    /**
     *
     */
    public void createUI() {
        processListPanel = new ProcessListPanel(this::runCommand);
        processListPanel.setMinimumSize(new Dimension(10, 10));
        this.add(processListPanel);
    }
    
    @Override
    protected void onComponentVisible() {
        if (!processListLoaded.get()) {
            processListLoaded.set(true);
            runCommand(null, ProcessListPanel.CommandMode.LIST_PROCESS);
        }
    }
    
    @Override
    protected void onComponentHide() {
    
    }
    
    private void updateProcessList(TauonRemoteSessionInstance instance, AtomicBoolean stopFlag) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        List<ProcessTableEntry> list = getProcessList(instance, stopFlag);
        SwingUtilities.invokeLater(() -> {
            // update ui ps
            processListPanel.setProcessList(list);
        });
    }

    private void runCommand(String cmd, ProcessListPanel.CommandMode mode) {
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        switch (mode) {
            case KILL_AS_USER:
                holder.submitSSHOperationStoppable(instance -> {
                    if (instance.exec(cmd, stopFlag, null, null) != 0) {
                        if (!holder.isSessionClosed()) {
                            JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
                        }
                    } else {
                        updateProcessList(instance, stopFlag);
                    }
                }, stopFlag);
                break;
            case KILL_AS_ROOT:
                holder.submitSSHOperationStoppable(instance -> {
                    if (SudoUtils.runSudo(cmd, instance) != 0) {
                        if (!holder.isSessionClosed()) {
                            JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
                        }
                    } else {
                        updateProcessList(instance, stopFlag);
                    }
                }, stopFlag);
                break;
            case LIST_PROCESS:
                holder.submitSSHOperationStoppable(instance -> {
                    updateProcessList(instance, stopFlag);
                }, stopFlag);
                break;
        }
    }

    public List<ProcessTableEntry> getProcessList(TauonRemoteSessionInstance instance, AtomicBoolean stopFlag) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        int ret = instance.exec(ScriptLoader.loadShellScript("/scripts/ps.sh"),
                // "ps -e -o pid=pid -o pcpu -o rss -o etime -o ppid -o user -o nice -o args -ww
                // --sort pid",
                stopFlag, out, err);
        if (ret != 0)
            throw new RemoteOperationException.ErrorReturnCode("ps.sh", ret, "Error while getting metrics");
        return parseProcessList(out.toString());
    }

    private List<ProcessTableEntry> parseProcessList(String text) {
        List<ProcessTableEntry> list = new ArrayList<>();
        String[] lines = text.split("\n");
        boolean first = true;
        for (String line : lines) {
            if (first) {
                first = false;
                continue;
            }
            String[] p = line.trim().split("\\s+");
            if (p.length < 8) {
                continue;
            }

            ProcessTableEntry ent = new ProcessTableEntry();
            try {
                ent.setPid(Integer.parseInt(p[0].trim()));
            } catch (Exception e) {
            }
            try {
                ent.setCpu(Float.parseFloat(p[1].trim()));
            } catch (Exception e) {
            }
            try {
                ent.setMemory(new ByteCountValue((long) Float.parseFloat(p[2].trim()) * 1024));
            } catch (Exception e) {
            }
            ent.setTime(p[3]);
            try {
                ent.setPpid(Integer.parseInt(p[4].trim()));
            } catch (Exception e) {
            }
            ent.setUser(p[5]);
            try {
                ent.setNice(Integer.parseInt(p[6].trim()));
            } catch (Exception e) {
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 7; i < p.length; i++) {
                sb.append(p[i] + " ");
            }
            ent.setArgs(sb.toString().trim());
            list.add(ent);
        }
        return list;
    }

}

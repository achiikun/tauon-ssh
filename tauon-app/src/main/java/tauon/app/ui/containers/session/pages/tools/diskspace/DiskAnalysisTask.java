package tauon.app.ui.containers.session.pages.tools.diskspace;

import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.util.ssh.SudoUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public class DiskAnalysisTask implements Runnable {
    private final TauonRemoteSessionInstance client;
    private final String folder;
    private final AtomicBoolean stopFlag;
    private final Consumer<DiskUsageEntry> callback;
    private final boolean sudo;
    private final SessionContentPanel holder;
    
    public DiskAnalysisTask(String folder, boolean sudo, AtomicBoolean stopFlag,
                            Consumer<DiskUsageEntry> callback, TauonRemoteSessionInstance client, SessionContentPanel holder) {
        this.callback = callback;
        this.folder = folder;
        this.stopFlag = stopFlag;
        this.client = client;
        this.sudo = sudo;
        this.holder = holder;
    }

    public void run() {
        DiskUsageEntry root = null;
        try {
            StringBuilder output = new StringBuilder();
            if(sudo){
                SudoUtils.runSudoWithOutput(
                        "export POSIXLY_CORRECT=1;", "du '" + folder + "'",
                        client, output, null, holder.getSudoPassword()
                );
            }else{
                client.exec("export POSIXLY_CORRECT=1; " + "du '" + folder + "'", stopFlag, output);
            }
            List<String> lines = Arrays.asList(output.toString().split("\n"));
            DuOutputParser duOutputParser = new DuOutputParser(folder);
            int prefixLen = folder.endsWith("/") ? folder.length() - 1
                    : folder.length();
            root = duOutputParser.parseList(lines, prefixLen);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            callback.accept(root);
        }
    }

    public void close() {
    }
}

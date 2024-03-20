package tauon.app.ui.containers.session.pages.diskspace;

import tauon.app.ssh.TauonRemoteSessionInstance;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public class DiskAnalysisTask implements Runnable {
    private final TauonRemoteSessionInstance client;
    private final String folder;
    private final AtomicBoolean stopFlag;
    private final Consumer<DiskUsageEntry> callback;

    public DiskAnalysisTask(String folder, AtomicBoolean stopFlag,
                            Consumer<DiskUsageEntry> callback, TauonRemoteSessionInstance client) {
        this.callback = callback;
        this.folder = folder;
        this.stopFlag = stopFlag;
        this.client = client;
    }

    public void run() {
        DiskUsageEntry root = null;
        try {
            StringBuilder scriptBuffer = new StringBuilder(
                    "export POSIXLY_CORRECT=1; " + "du '" + folder + "'");
            StringBuilder output = new StringBuilder();
            client.exec(scriptBuffer.toString(), stopFlag, output);
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

package tauon.app.ui.containers.session.pages.tools.diskspace;

import tauon.app.ssh.GuiHandle;
import tauon.app.ssh.IStopper;
import tauon.app.ssh.SSHCommandRunner;
import tauon.app.ssh.SSHConnectionHandler;

import java.util.Arrays;
import java.util.List;


public class DiskAnalysisTask implements Runnable {
    private final GuiHandle guiHandle;
    private final SSHConnectionHandler client;
    private final String folder;
    private final IStopper stopFlag;
    private final boolean sudo;
    
    private DiskUsageEntry root = null;
    
    public DiskAnalysisTask(String folder, boolean sudo, IStopper stopFlag, GuiHandle guiHandle, SSHConnectionHandler client) {
        this.folder = folder;
        this.stopFlag = stopFlag;
        this.guiHandle = guiHandle;
        this.client = client;
        this.sudo = sudo;
    }
    
    public DiskUsageEntry getRoot() {
        return root;
    }
    
    public void run() {
        try {
            StringBuilder output = new StringBuilder();
            
            SSHCommandRunner commandRunner = new SSHCommandRunner()
                    .withCommand("du '" + folder + "'")
                    .withStdoutAppendable(output)
                    .withEnvVar("POSIXLY_CORRECT", "1")
                    .withStopper(stopFlag)
                    .withSudo(sudo ? guiHandle : null);
            
            client.exec(commandRunner);
            
//            if(sudo){
//                SudoUtils.runSudoWithOutput(
//                        "export POSIXLY_CORRECT=1;",
//                        "du '" + folder + "'",
//                        stopFlag,
//                        client, output, null
//                );
//            }else{
//                client.exec("export POSIXLY_CORRECT=1; " + "du '" + folder + "'", stopFlag, output);
//            }
            List<String> lines = Arrays.asList(output.toString().split("\n"));
            DuOutputParser duOutputParser = new DuOutputParser(folder);
            int prefixLen = folder.endsWith("/") ? folder.length() - 1 : folder.length();
            // TODO use stopper here as well
            root = duOutputParser.parseList(lines, prefixLen);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
    }
}

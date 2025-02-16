package tauon.app.ssh.filesystem.transfer;

import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.exceptions.TauonOperationException;
import tauon.app.ssh.GuiHandle;
import tauon.app.ssh.SSHConnectionHandler;
import tauon.app.ssh.filesystem.*;
import tauon.app.util.misc.Constants;

public class FileTransferLocalToRemote extends FileTransfer{
    
    private final SSHConnectionHandler sshConnectionHandler;
    
    public FileTransferLocalToRemote(
            FileInfo[] files,
            String targetFolder,
            GuiHandle guiHandle,
            SSHConnectionHandler sshConnectionHandler,
            Constants.ConflictAction defaultConflictAction
    ){
        super(guiHandle, files, targetFolder, defaultConflictAction);
        this.sshConnectionHandler = sshConnectionHandler;
    }
    
    public void prepareTransfer(SshFileSystem targetFs) throws OperationCancelledException, AlreadyFailedException, TauonOperationException, InterruptedException, SessionClosedException {
        prepareTransfer(LocalFileSystem.getInstance(), targetFs);
    }
    
    public void run(){
        SSHConnectionHandler.TempSshFileSystem targetFs = null;
        try {
             targetFs = sshConnectionHandler.openTempSshFileSystem();
             
             if(progressListener != null)
                progressListener.init(totalSize, filesToTransfer.size());
             
            InputTransferChannel inc = LocalFileSystem.getInstance().inputTransferChannel();
            OutputTransferChannel outc = targetFs.outputTransferChannel();
            
            for (FileInfoHolder file : filesToTransfer) {
                System.out.println("Copying: " + file.info.getPath());
                
                copyFile(file, targetFs, inc, outc);
                System.out.println("Copying done: " + file.info.getPath());
                incrementProcessedFiles();
            }
            
            if(progressListener != null)
                progressListener.done();
            
        } catch (Exception e) {
            if(progressListener != null)
                progressListener.error(null, e);
        } finally {
            if(targetFs != null){
                targetFs.dispose();
            }
        }
    }
    
}

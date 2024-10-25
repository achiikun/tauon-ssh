/**
 *
 */
package tauon.app.exceptions;

import tauon.app.util.misc.FormatUtils;

import java.io.IOException;
import java.util.Map;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class RemoteOperationException extends TauonOperationException {
    
    public RemoteOperationException(Exception e) {
        super(e);
    }
    
    private RemoteOperationException() {

    }
    
    public static class NotConnected extends RemoteOperationException {
        
        public NotConnected(Exception e) {
            super(e);
        }
        
        public NotConnected(){
        
        }
        
    }
    
    public static class RealIOException extends RemoteOperationException {
        public RealIOException(java.io.IOException e) {
            super(e);
        }
        
        @Override
        public String getUserMessage() {
            return getCause() != null ? FormatUtils.$$(
                    getBundle().getString("general.remote_operation_exception.message.real_io_exception"),
                    Map.of("MESSAGE", getCause().getMessage())
            ) : getBundle().getString("general.remote_operation_exception.message.real_io_exception_unknown");
        }
    }
    
    public static class FileNotFound extends RemoteOperationException {
        private final String path;
        
        public FileNotFound(IOException e, String path) {
            super(e);
            this.path = path;
        }
        
        public String getPath() {
            return path;
        }
        
        @Override
        public String getUserMessage() {
            return FormatUtils.$$(
                    getBundle().getString("general.remote_operation_exception.message.path_not_found"),
                    Map.of("PATH", path)
            );
        }
    }
    
    public static class PermissionDenied extends RemoteOperationException {
        public PermissionDenied(net.schmizz.sshj.sftp.SFTPException e) {
            super(e);
        }
        
        @Override
        public String getUserMessage() {
            return getBundle().getString("general.remote_operation_exception.message.permission_denied");
        }
        
    }
    
    public static class SFTPException extends RemoteOperationException {
        public SFTPException(net.schmizz.sshj.sftp.SFTPException e) {
            super(e);
        }
        
        @Override
        public String getUserMessage() {
            return getCause() != null ? FormatUtils.$$(
                    getBundle().getString("general.remote_operation_exception.message.real_io_exception"),
                    Map.of("MESSAGE", getCause().getMessage())
            ) : getBundle().getString("general.remote_operation_exception.message.real_io_exception_unknown");
        }
        
    }
    
    public static class NotImplemented extends RemoteOperationException {
        private final String message;
        
        public NotImplemented(String message) {
            this.message = message;
        }
        
        @Override
        public String getUserMessage() {
            return getCause() != null ? FormatUtils.$$(
                    getBundle().getString("general.remote_operation_exception.message.not_implemented"),
                    Map.of("MESSAGE", message)
            ) : getBundle().getString("general.remote_operation_exception.message.not_implemented_unknown");
        }
        
    }
    
    public static class ErrorReturnCode extends RemoteOperationException {
        private final String cmd;
        private final int ret;
        private final String message;
        
        public ErrorReturnCode(String cmd, int ret) {
            this.cmd = cmd;
            this.ret = ret;
            this.message = null;
        }
        
        public ErrorReturnCode(String cmd, int ret, String message) {
            this.cmd = cmd;
            this.ret = ret;
            this.message = message;
        }
        
        @Override
        public String getUserMessage() {
            return message != null ? FormatUtils.$$(
                    getBundle().getString("general.remote_operation_exception.message.unexpected_return_code"),
                    Map.of("MESSAGE", message)
            ) : getBundle().getString("general.remote_operation_exception.message.unexpected_return_code_unknown");
        }
        
    }
    
    @Override
    public String getUserMessage() {
        return getCause() != null ? FormatUtils.$$(
                getBundle().getString("general.remote_operation_exception.message.remote_exception"),
                Map.of("MESSAGE", getCause().getMessage())
        ) : getBundle().getString("general.remote_operation_exception.message.remote_exception_unknown");
    }
    
}

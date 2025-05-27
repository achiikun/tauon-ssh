/**
 *
 */
package tauon.app.exceptions;

import tauon.app.util.misc.FormatUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class LocalOperationException extends TauonOperationException {
    
    protected LocalOperationException(Exception e) {
        super(e);
    }
    
    public LocalOperationException() {

    }
    
    public static class NotAFolder extends LocalOperationException {
        private final File file;
        
        public NotAFolder(File file) {
            this.file = file;
        }
        
        @Override
        public String getUserMessage() {
            return FormatUtils.$$(
                    getBundle().getString("general.local_operation_exception.message.not_a_folder"),
                    Map.of("MESSAGE", file)
            );
        }
        
    }
    
    public static class FileNotFound extends LocalOperationException {
        private final String path;
        
        public FileNotFound(IOException e, String path) {
            super(e);
            this.path = path;
        }
        
        public FileNotFound(String path) {
            super();
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
    
    public static class RealIOException extends LocalOperationException {
        public RealIOException(java.io.IOException e) {
            super(e);
        }
        
        @Override
        public String getUserMessage() {
            return getCause() != null ? FormatUtils.$$(
                    getBundle().getString("general.local_operation_exception.message.real_io_exception"),
                    Map.of("MESSAGE", getCause().getMessage())
            ) : getBundle().getString("general.local_operation_exception.message.real_io_exception_unknown");
        }
    }
    
    public static class UnsupportedFlavorException extends LocalOperationException {
        public UnsupportedFlavorException(java.awt.datatransfer.UnsupportedFlavorException e) {
            super(e);
        }
        
        @Override
        public String getUserMessage() {
            return getCause() != null ? FormatUtils.$$(
                    getBundle().getString("general.local_operation_exception.message.unsupported_flavor_exception"),
                    Map.of("MESSAGE", getCause().getMessage())
            ) : getBundle().getString("general.local_operation_exception.message.unsupported_flavor_exception_unknown");
        }
        
    }
    
    public static class NotImplemented extends LocalOperationException {
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
    
    @Override
    public String getUserMessage() {
        return getCause() != null ? FormatUtils.$$(
                getBundle().getString("general.remote_operation_exception.message.local_exception"),
                Map.of("MESSAGE", getCause().getMessage())
        ) : getBundle().getString("general.remote_operation_exception.message.local_exception_unknown");
    }
    
}

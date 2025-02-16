/**
 *
 */
package tauon.app.exceptions;

/**
 * @author subhro
 *
 */
public abstract class TauonOperationException extends Exception {
    
    protected TauonOperationException() {
    
    }
    
    protected TauonOperationException(Exception e) {
        super(e);
    }
    
    public abstract String getUserMessage();
    
    public static class NotImplemented extends TauonOperationException {
        
        private final String userMessage;
        
        public NotImplemented(String userMessage){
            this.userMessage = userMessage;
        }
        
        @Override
        public String getUserMessage() {
            return userMessage;
        }
    }
    
}

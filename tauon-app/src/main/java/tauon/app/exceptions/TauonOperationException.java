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
    
}

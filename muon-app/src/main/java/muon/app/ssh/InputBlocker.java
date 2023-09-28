/**
 *
 */
package muon.app.ssh;

/**
 * @author subhro
 *
 */
public interface InputBlocker {
    
    void blockInput(Runnable cancellable);

    void unblockInput();
}

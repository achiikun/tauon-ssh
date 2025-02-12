/**
 *
 */
package tauon.app.ui.components.glasspanes;

/**
 * @author subhro
 *
 */
public interface InputBlocker {
    
    void blockInput(String label, Runnable cancellable);

    void unblockInput();
}

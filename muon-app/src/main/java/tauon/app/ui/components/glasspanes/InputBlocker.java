/**
 *
 */
package tauon.app.ui.components.glasspanes;

/**
 * @author subhro
 *
 */
public interface InputBlocker {
    
    void blockInput(Runnable cancellable);

    void unblockInput();
}

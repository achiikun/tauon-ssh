/**
 *
 */
package tauon.app.ui.components.closabletabs;

import java.util.function.Supplier;

public interface TabHandle {
    void setTitle(String title);
    void setClosable(Supplier<Boolean> onCloseCallback);
}

/**
 *
 */
package tauon.app.ui.laf.theme;

import com.jediterm.terminal.TextStyle;

/**
 * @author subhro
 *
 */
public interface TerminalTheme {
    String getName();

    TextStyle getDefaultStyle();

    TextStyle getSelectionColor();

    TextStyle getFoundPatternColor();

    TextStyle getHyperlinkColor();

    String toString();
}

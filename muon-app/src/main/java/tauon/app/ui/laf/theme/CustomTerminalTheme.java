/**
 *
 */
package tauon.app.ui.laf.theme;

import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import tauon.app.App;

import com.jediterm.core.Color;

import static tauon.app.services.SettingsService.getSettings;

/**
 * @author subhro
 *
 */
public class CustomTerminalTheme implements TerminalTheme {

    public static final TerminalColor getTerminalColor(int rgb) {
        return TerminalColor.fromColor(new Color(rgb));
    }

    @Override
    public String getName() {
        return "Custom";
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public TextStyle getDefaultStyle() {
        return new TextStyle(
                getTerminalColor(getSettings().getDefaultColorFg()),
                getTerminalColor(getSettings().getDefaultColorBg()));
    }

    @Override
    public TextStyle getSelectionColor() {
        return new TextStyle(
                getTerminalColor(
                        getSettings().getDefaultSelectionFg()),
                getTerminalColor(
                        getSettings().getDefaultSelectionBg()));
    }

    @Override
    public TextStyle getFoundPatternColor() {
        return new TextStyle(
                getTerminalColor(getSettings().getDefaultFoundFg()),
                getTerminalColor(getSettings().getDefaultFoundFg()));
    }

    @Override
    public TextStyle getHyperlinkColor() {
        return new TextStyle(
                getTerminalColor(getSettings().getDefaultHrefFg()),
                getTerminalColor(getSettings().getDefaultHrefBg()));
    }
}

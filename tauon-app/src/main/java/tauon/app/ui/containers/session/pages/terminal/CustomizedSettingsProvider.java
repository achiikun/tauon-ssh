/**
 *
 */
package tauon.app.ui.containers.session.pages.terminal;

import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.emulator.ColorPalette;
import com.jediterm.terminal.ui.TerminalActionPresentation;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.services.SettingsService;
import tauon.app.settings.Settings;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.util.misc.FontUtils;

import javax.swing.*;
import java.awt.*;

import com.jediterm.core.Color;

/**
 * @author subhro
 *
 */
public class CustomizedSettingsProvider extends DefaultSettingsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CustomizedSettingsProvider.class);
    
    private final ColorPalette palette;
    
    private final SessionContentPanel session;
    
    /**
     *
     */
    public CustomizedSettingsProvider(SessionContentPanel session) {
        this.session = session;
        
        Color[] colors = new Color[16];
        int[] colorArr = SettingsService.getSettings().getPalleteColors();
        for (int i = 0; i < 16; i++) {
            colors[i] = new Color(colorArr[i]);
        }

        //palette = this.getTerminalColorPalette;
        palette = new ColorPalette() {

            public Color[] getIndexColors() {
                return colors;
            }

            @Override
            protected Color getBackgroundByColorIndex(int colorIndex) {
                return colors[colorIndex];
            }
            @Override
              public Color getForegroundByColorIndex(int colorIndex) {
                return colors[colorIndex];
              }
        };
    }

    /*
     * (non-Javadoc)
     *
     * @see com.jediterm.terminal.ui.settings.DefaultSettingsProvider#
     * getTerminalColorPalette()
     */
    @Override
    public ColorPalette getTerminalColorPalette() {
        return palette;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.jediterm.terminal.ui.settings.DefaultSettingsProvider#
     * useAntialiasing()
     */
    @Override
    public boolean useAntialiasing() {
        return true;
    }

    @Override
    public TextStyle getDefaultStyle() {
        return new TextStyle(getTerminalColor(SettingsService.getSettings().getDefaultColorFg()),
                getTerminalColor(SettingsService.getSettings().getDefaultColorBg()));
    }

    @Override
    public TextStyle getFoundPatternColor() {
        return new TextStyle(getTerminalColor(SettingsService.getSettings().getDefaultFoundFg()),
                getTerminalColor(SettingsService.getSettings().getDefaultFoundBg()));
    }

    @Override
    public TextStyle getSelectionColor() {
        return new TextStyle(getTerminalColor(SettingsService.getSettings().getDefaultSelectionFg()),
                getTerminalColor(SettingsService.getSettings().getDefaultSelectionBg()));
        //
    }

    @Override
    public TextStyle getHyperlinkColor() {
        return new TextStyle(getTerminalColor(SettingsService.getSettings().getDefaultHrefFg()),
                getTerminalColor(SettingsService.getSettings().getDefaultHrefBg()));

    }

    @Override
    public boolean emulateX11CopyPaste() {
        return SettingsService.getSettings().isPuttyLikeCopyPaste();
    }

    @Override
    public boolean enableMouseReporting() {
        return true;
    }

    @Override
    public boolean pasteOnMiddleMouseClick() {
        return SettingsService.getSettings().isPuttyLikeCopyPaste();
    }

    @Override
    public boolean copyOnSelect() {
        return SettingsService.getSettings().isPuttyLikeCopyPaste();
    }

    @Override
    public Font getTerminalFont() {
        System.out.println("Called terminal font: " + SettingsService.getSettings().getTerminalFontName());
        return FontUtils.loadTerminalFont(SettingsService.getSettings().getTerminalFontName()).deriveFont(Font.PLAIN,
                SettingsService.getSettings().getTerminalFontSize());
    }

    @Override
    public float getTerminalFontSize() {
        return SettingsService.getSettings().getTerminalFontSize();
    }

    @Override
    public boolean audibleBell() {
        return SettingsService.getSettings().isTerminalBell();
    }

    public final TerminalColor getTerminalColor(int rgb) {
        return TerminalColor.fromColor(new Color(rgb));
    }
    
    
    @Override
    public @NotNull TerminalActionPresentation getCopyActionPresentation() {
        return new TerminalActionPresentation("Copy", getKeyStroke(Settings.COPY_KEY));
    }
    
    @Override
    public @NotNull TerminalActionPresentation getPasteActionPresentation() {
        return new TerminalActionPresentation("Paste", getKeyStroke(Settings.PASTE_KEY));
    }
    
    @Override
    public @NotNull TerminalActionPresentation getClearBufferActionPresentation() {
        return new TerminalActionPresentation("Clear Buffer", getKeyStroke(Settings.CLEAR_BUFFER));
    }
    
    @Override
    public @NotNull TerminalActionPresentation getFindActionPresentation() {
        return new TerminalActionPresentation("Find", getKeyStroke(Settings.FIND_KEY));
    }
    
    @Override
    public @NotNull TerminalActionPresentation getTypeSudoPasswordActionPresentation() {
        return new TerminalActionPresentation("Type SUDO Password", getKeyStroke(Settings.TYPE_SUDO_PASSWORD));
    }
    
    private KeyStroke getKeyStroke(String key) {
        return KeyStroke.getKeyStroke(
                SettingsService.getSettings().getKeyCodeMap().get(key),
                SettingsService.getSettings().getKeyModifierMap().get(key)
        );
    }
    
    @Override
    public String getSudoPassword() {
        return session.getSudoPassword();
    }
}

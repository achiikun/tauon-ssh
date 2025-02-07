package tauon.app.ui.containers.session.pages.terminal;

import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.emulator.charset.CharacterSets;
import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalTextBuffer;
import com.jediterm.terminal.ui.TerminalPanel;
import com.jediterm.terminal.ui.settings.SettingsProvider;
import org.jetbrains.annotations.NotNull;
import tauon.app.util.misc.FontUtils;

import java.awt.*;

public class CustomTerminalPanel extends TerminalPanel {
    
    private final @NotNull SettingsProvider settingsProvider;
    
    private Font myNormalFont;
    private Font myItalicFont;
    private Font myBoldFont;
    private Font myBoldItalicFont;
    
    public CustomTerminalPanel(@NotNull SettingsProvider settingsProvider, @NotNull TerminalTextBuffer terminalTextBuffer, @NotNull StyleState styleState) {
        super(settingsProvider, terminalTextBuffer, styleState);
        this.settingsProvider = settingsProvider;
    }
    
    @Override
    protected void initFont() {
        super.initFont();
        
        FontUtils.TerminalFont terminalFont = ((CustomizedSettingsProvider)settingsProvider).getCustomTerminalFont();
        float terminalFontSize = settingsProvider.getTerminalFontSize();
        
        myNormalFont = terminalFont.getFont().deriveFont(terminalFontSize);
        myBoldFont = terminalFont.getFontBoldOrRegular().deriveFont(terminalFontSize);
        myItalicFont = terminalFont.getFontItalicOrRegular().deriveFont(terminalFontSize);
        myBoldItalicFont = terminalFont.getFontBoldItalicOrRegular().deriveFont(terminalFontSize);
    }
    
    protected @NotNull Font getFontToDisplay(char[] text, int start, int end, @NotNull TextStyle style) {
        boolean bold = style.hasOption(TextStyle.Option.BOLD);
        boolean italic = style.hasOption(TextStyle.Option.ITALIC);
        // workaround to fix Swing bad rendering of bold special chars on Linux
        if (bold && settingsProvider.DECCompatibilityMode() && CharacterSets.isDecBoxChar(text[start])) {
            return myNormalFont;
        }
        return bold ? (italic ? myBoldItalicFont : myBoldFont)
                : (italic ? myItalicFont : myNormalFont);
    }
    
}

/**
 *
 */
package tauon.app.util.misc;

import tauon.app.ui.laf.AppSkin;

import java.awt.*;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

/**
 * @author subhro
 *
 */
public class FontUtils {
    
    public static class TerminalFont{
        
        private final String regularTtf;
        private String boldTtf;
        private String italicTtf;
        private String boldItalicTtf;
        
        private Font regularFont;
        private Font boldFont;
        private Font italicFont;
        private Font boldItalicFont;
        
        public TerminalFont(String regularTtf){
            this.regularTtf = regularTtf;
        }
        
        public TerminalFont(String regularTtf, String boldTtf, String italicTtf, String boldItalicTtf){
            this.regularTtf = regularTtf;
            this.boldTtf = boldTtf;
            this.italicTtf = italicTtf;
            this.boldItalicTtf = boldItalicTtf;
        }
        
        public Font getFont() {
            if(regularFont == null)
                regularFont = loadTerminalFont(regularTtf);
            return regularFont;
        }
        
        public Font getFontBoldOrRegular() {
            if(boldFont == null && boldTtf != null)
                boldFont = loadTerminalFont(boldTtf);
            return boldFont != null ? boldFont : regularFont;
        }
        
        public Font getFontItalicOrRegular() {
            if(italicFont == null && italicTtf != null)
                italicFont = loadTerminalFont(italicTtf);
            return italicFont != null ? italicFont : regularFont;
        }
        
        public Font getFontBoldItalicOrRegular() {
            if(boldItalicFont == null && boldItalicTtf != null)
                boldItalicFont = loadTerminalFont(boldItalicTtf);
            return boldItalicFont != null ? boldItalicFont : regularFont;
        }
        
        public static Font loadTerminalFont(String name) {
            System.out.println("Loading font: " + name);
            try (InputStream is = AppSkin.class.getResourceAsStream(String.format("/fonts/terminal/%s.ttf", name))) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);
                System.out.println("Font loaded: " + font.getFontName() + " of family: " + font.getFamily());
                return font;//.deriveFont(Font.PLAIN, 12.0f);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        
    }
    
    public static final Map<String, TerminalFont> TERMINAL_FONTS = new CollectionHelper.OrderedDict<String, TerminalFont>()
            .putItem("JetBrains Mono", new TerminalFont("JetBrainsMono-Regular", "JetBrainsMono-Bold", "JetBrainsMono-Italic", "JetBrainsMono-BoldItalic"))
            .putItem("Hack", new TerminalFont("Hack-Regular","Hack-Bold", "Hack-Italic", "Hack-BoldItalic"))
            .putItem("DejaVu Sans Mono", new TerminalFont("DejaVuSansMono"))
            .putItem("Fira Code Regular", new TerminalFont("FiraCode-Regular"))
            .putItem("Inconsolata Regular", new TerminalFont("Inconsolata-Regular"))
            .putItem("Noto Mono", new TerminalFont("NotoMono-Regular"));

    public static Font loadFont(String path) {
        try (InputStream is = AppSkin.class.getResourceAsStream(path)) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);
            System.out.println("Font loaded: " + font.getFontName() + " of family: " + font.getFamily());
            return font.deriveFont(Font.PLAIN, 12.0f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static TerminalFont loadTerminalFont(String name) {
        return Objects.requireNonNullElseGet(TERMINAL_FONTS.get(name), () -> TERMINAL_FONTS.values().iterator().next());
    }
}

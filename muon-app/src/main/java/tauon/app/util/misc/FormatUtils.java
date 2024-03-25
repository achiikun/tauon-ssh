package tauon.app.util.misc;

import tauon.app.App;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tauon.app.services.SettingsService.getSettings;

public class FormatUtils {
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyyMMdd");

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
                + (si ? "" : "i");
        return String.format("%.1f %s", bytes / Math.pow(unit, exp), pre);
    }

    public static String formatDate(LocalDateTime dateTime) {

        if (getSettings().isShowActualDateOnlyHour()) {
            Date actualDate = java.util.Date
                    .from(LocalDateTime.now().atZone(ZoneId.systemDefault())
                            .toInstant());

            Date objectDate = java.util.Date
                    .from(dateTime.atZone(ZoneId.systemDefault())
                            .toInstant());

            if (FMT.format(actualDate).equals(FMT.format(objectDate))) {
                return dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            }
        }

        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public static final String nameString = "(?<name>[a-zA-Z][a-zA-Z0-9\\.\\-\\_\\@\\#\\$\\%\\&\\!\\?]*)";
    public static final String nameStringWithOptionalBrackets = "(\\{(?<name2>[a-zA-Z0-9\\.\\-\\_\\@\\#\\$\\%\\&\\!\\?]+)\\})";
    public static final String formatString = "(:(?<type>%[(\\-#.\\s0-9]*[abcdefghnostxX]))";
    public static final Pattern p2 = Pattern.compile("\\{(" + nameString + "|" + nameStringWithOptionalBrackets + ")?" + formatString + "?}");
    
    /**
     * <p>
     * Finds all "{...}" occurrences in the string and replace it with the objects
     * in the map.
     *
     * @param what String to return formatted with braces {}
     * @param params Map of objects to substitute braces.
     * @return
     */
    public static String $$(String what, Map<String, Object> params) {
        if (what == null)
            return "";
        
        if (params == null || params.size() == 0)
            return what;
        
        Matcher m = p2.matcher(what);
        StringBuilder newString = new StringBuilder();
        
        while (m.find()) {
            
            String name = null;
            try {
                name = m.group("name");
            } catch (Exception e) {
            
            }
            if(name == null){
                try {
                    name = m.group("name2");
                } catch (Exception e) {
                
                }
            }
            
            String type = null;
            try {
                type = m.group("type");
            } catch (Exception e) {
            
            }
            
            if(name != null) {
                m.appendReplacement(newString, Matcher.quoteReplacement(type == null ? String.valueOf(params.get(name)) : String.format(type, params.get(name))));
            }else {
                m.appendReplacement(newString, m.group());
            }
            
        }
        
        m.appendTail(newString);
        
        return newString.toString();
        
    }
    
    private static Integer grow(ArrayList<Object> list, Iterator<Object> iterator, int size, Integer paramIdx) {
        if(paramIdx >= list.size() || paramIdx < -size)
            return null;
        if(paramIdx < 0)
            paramIdx = size+paramIdx;
        while(list.size() <= paramIdx && iterator.hasNext()){
            list.add(iterator.next());
        }
        return list.size() >= paramIdx ? paramIdx : null;
    }
    
}

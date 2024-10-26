package tauon.app.ssh.filesystem;

import static tauon.app.services.LanguageService.getBundle;

public enum FileType {
    FILE, DIR, DIR_LINK, FILE_LINK;
    
    private String name = name();
    
    public static void update() {
        FILE.name = getBundle().getString("app.files.enum.file_type.file");
        DIR.name = getBundle().getString("app.files.enum.file_type.dir");
        FILE_LINK.name = getBundle().getString("app.files.enum.file_type.file_link");
        DIR_LINK.name = getBundle().getString("app.files.enum.file_type.dir_link");
    }
    
    @Override
    public String toString() {
        return name;
    }
}

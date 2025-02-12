package tauon.app.settings.importers;

import tauon.app.settings.SiteInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class SSHConfigImporter {


    static final String HOST_TEXT = "Host";
    static final String IP_TEXT = "HostName";
    static final String PORT_TEXT = "Port";
    static final String IDENTITY_FILE_TEXT = "IdentityFile";
    static final String USER_TEXT = "User";

    public static List<SiteInfo> getSessionFromFile(File file) throws FileNotFoundException {
        List<SiteInfo> siteInfoList = new ArrayList<>();
        Scanner myReader = new Scanner(file);
        String linea = myReader.hasNextLine() ? myReader.nextLine() : null;
        SiteInfo info = new SiteInfo();
        if (linea.contains(HOST_TEXT)) {
            info.setName(sanitizeString(linea, HOST_TEXT));
        }
        while (myReader.hasNextLine()) {
            linea = myReader.nextLine();
            if (linea.contains(IP_TEXT)) {
                info.setHost(sanitizeString(linea, IP_TEXT));
            } else if (linea.contains(USER_TEXT)) {
                info.setUser(sanitizeString(linea, USER_TEXT));
            } else if (linea.contains(PORT_TEXT)) {
                info.setPort(Integer.parseInt(sanitizeString(linea, PORT_TEXT)));
            } else if (linea.contains(IDENTITY_FILE_TEXT)) {
                info.setPrivateKeyFile(sanitizeString(linea, IDENTITY_FILE_TEXT));
            } else if (linea.contains(HOST_TEXT)) {
                if (info.getName() != null) {
                    siteInfoList.add(info);
                }
                info = new SiteInfo();
                info.setName(sanitizeString(linea, HOST_TEXT));
            }
        }
        if (info.getName() != null) {
            siteInfoList.add(info);
        }

        return siteInfoList;
    }

    public static String sanitizeString(String line, String key) {
        return line.trim().replace(key, "").replaceAll("\"", "").replaceAll("\t", "").trim();
    }

}

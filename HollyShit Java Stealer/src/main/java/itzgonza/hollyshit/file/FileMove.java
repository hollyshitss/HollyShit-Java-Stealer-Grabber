package itzgonza.hollyshit.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import itzgonza.hollyshit.utils.utilities;

public class FileMove extends utilities {

    public void initialize() throws Exception {
        List<File> list = new ArrayList<>();
        listFilesRecursively(new File(getPath()), list);
        
        FileOutputStream fos = new FileOutputStream(getPath() + ".zip");
        ZipOutputStream zos = new ZipOutputStream(fos);

        for (File file : list) {
            if (file.isDirectory()) continue;
            try {
                addFileToZip(new File(getPath()), file, zos);
            } catch (Exception ignored) {}
        }
        
        zos.close();
        fos.close();
    }

    private void listFilesRecursively(File listDirectory, List<File> list) {
        File[] files = listDirectory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) 
                listFilesRecursively(file, list);
            else 
                list.add(file);
        }
    }

    private void addFileToZip(File directoryToZip, File file, ZipOutputStream zos) throws Exception {
        FileInputStream inputStream = new FileInputStream(file);
        String path = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1);
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = inputStream.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        zos.closeEntry();
        inputStream.close();
    }
}
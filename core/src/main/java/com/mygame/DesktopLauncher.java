package com.mygame;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;

public class DesktopLauncher {
    public static void main(String[] args) {
        // Загружаем иконки
        BufferedImage[] icons = null;
        try {
            icons = new BufferedImage[]{
                ImageIO.read(DesktopLauncher.class.getResourceAsStream("/icon16.png")),
                ImageIO.read(DesktopLauncher.class.getResourceAsStream("/icon32.png")),
                ImageIO.read(DesktopLauncher.class.getResourceAsStream("/icon64.png")),
                ImageIO.read(DesktopLauncher.class.getResourceAsStream("/icon128.png"))
            };
            System.out.println("[DesktopLauncher] Icons loaded");
        } catch (Exception e) {
            System.err.println("[DesktopLauncher] Failed to load icons: " + e.getMessage());
        }

        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Exorcist");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setVSync(true);
        settings.setSamples(4);
        settings.setUseInput(true);
        if (icons != null) {
            settings.setIcons(icons);
        }
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start(JmeContext.Type.Display);
    }
}
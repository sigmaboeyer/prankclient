package com.prank;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("prankclient.properties");
    }

    private static boolean persistsEnabled(Module m) {
        return m != Modules.ELYTRASWAP && m != Modules.WAYPOINT_ADD && m != Modules.FREECAM;
    }

    public static void load() {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file())) {
            p.load(in);
        } catch (Exception e) {
            return;
        }
        for (Module m : Modules.ALL) {
            String k = m.id();
            try { m.key = Integer.parseInt(p.getProperty(k + ".key", String.valueOf(m.key))); } catch (Exception ignored) { }
            try { if (m.hasColor) m.color = Integer.parseInt(p.getProperty(k + ".color", String.valueOf(m.color))); } catch (Exception ignored) { }
            for (Module.Setting s : m.settings) {
                try {
                    s.value = Double.parseDouble(p.getProperty(k + ".s." + s.id(), String.valueOf(s.value)));
                } catch (Exception ignored) { }
            }
            if (m.movable) {
                try {
                    String hx = p.getProperty(k + ".hx");
                    if (hx != null) {
                        m.hx = Integer.parseInt(hx);
                        m.hy = Integer.parseInt(p.getProperty(k + ".hy", "0"));
                    }
                } catch (Exception ignored) { }
            }
            if (persistsEnabled(m)) {
                m.enabled = Boolean.parseBoolean(p.getProperty(k + ".on", String.valueOf(m.enabled)));
                m.anim = m.enabled ? 1f : 0f;
                m.hudAnim = m.anim;
            }
        }
        try {
            ModuleScreen.offX = Integer.parseInt(p.getProperty("gui.x", "0"));
            ModuleScreen.offY = Integer.parseInt(p.getProperty("gui.y", "0"));
        } catch (Exception ignored) { }
        Friends.load(p.getProperty("friends", ""));
        Waypoints.load(p.getProperty("waypoints", ""));
    }

    public static void save() {
        Properties p = new Properties();
        for (Module m : Modules.ALL) {
            String k = m.id();
            p.setProperty(k + ".key", String.valueOf(m.key));
            if (m.hasColor) p.setProperty(k + ".color", String.valueOf(m.color));
            for (Module.Setting s : m.settings) p.setProperty(k + ".s." + s.id(), String.valueOf(s.value));
            if (m.movable && m.hx != Integer.MIN_VALUE) {
                p.setProperty(k + ".hx", String.valueOf(m.hx));
                p.setProperty(k + ".hy", String.valueOf(m.hy));
            }
            if (persistsEnabled(m)) p.setProperty(k + ".on", String.valueOf(m.enabled));
        }
        p.setProperty("gui.x", String.valueOf(ModuleScreen.offX));
        p.setProperty("gui.y", String.valueOf(ModuleScreen.offY));
        p.setProperty("friends", Friends.serialize());
        p.setProperty("waypoints", Waypoints.serialize());
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Kelo Client settings");
        } catch (Exception ignored) { }
    }
}

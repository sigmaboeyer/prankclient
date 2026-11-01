package com.prank;

import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public static class Setting {
        public final String name;
        public final boolean isBool;
        public double value, min, max, step;

        Setting(String name, boolean isBool, double value, double min, double max, double step) {
            this.name = name;
            this.isBool = isBool;
            this.value = value;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public String id() { return name.replaceAll("[^A-Za-z0-9]", ""); }
    }

    public final String name, desc, category;
    public boolean enabled;
    public int key = -1;
    public boolean wasDown;
    public boolean hasColor;
    /** 0 means rainbow. */
    public int color = 0xFFA78BFA;
    public final List<Setting> settings = new ArrayList<>();
    public float anim, hoverAnim, hudAnim;

    // movable HUD elements
    public boolean movable;
    public int hx = Integer.MIN_VALUE, hy;      // saved position (MIN_VALUE = default spot)
    public int dx, dy, dw, dh;                  // where it was drawn last frame

    private int cKey = -2;
    private String cLabel = "None";

    public Module(String name, String desc, String category, boolean enabled) {
        this.name = name;
        this.desc = desc;
        this.category = category;
        this.enabled = enabled;
        this.anim = enabled ? 1f : 0f;
        this.hudAnim = enabled ? 1f : 0f;
    }

    public Module color(int c) { hasColor = true; color = c; return this; }

    public Module movable() { movable = true; return this; }

    public Module bool(String n, boolean v) {
        settings.add(new Setting(n, true, v ? 1 : 0, 0, 1, 1));
        return this;
    }

    public Module num(String n, double v, double min, double max, double step) {
        settings.add(new Setting(n, false, v, min, max, step));
        return this;
    }

    public Setting find(String n) {
        for (Setting s : settings) if (s.name.equals(n)) return s;
        return null;
    }

    public double num(String n) {
        Setting s = find(n);
        return s == null ? 0 : s.value;
    }

    public boolean on(String n) {
        Setting s = find(n);
        return s != null && s.value > 0.5;
    }

    public String keyLabel() {
        if (key != cKey) {
            cKey = key;
            cLabel = key <= 0 ? "None" : InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
        }
        return cLabel;
    }

    public int getColor() { return color == 0 ? rainbow(0f) : color; }

    public static int rainbow(float offset) {
        float h = (float) ((((System.currentTimeMillis() * Access.rainbowSpeed()) % 5000.0) / 5000.0) + offset) % 1f;
        if (h < 0) h += 1f;
        return hsv(h);
    }

    public static int hsv(float h) {
        float s = 0.65f, v = 1f;
        float f = (h % 1f) * 6f;
        int i = (int) f;
        float ff = f - i;
        float p = v * (1 - s), q = v * (1 - s * ff), t = v * (1 - s * (1 - ff));
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public String id() { return name.replaceAll("[^A-Za-z0-9]", ""); }

    public void toggle() {
        enabled = !enabled;
        Notifs.push(name + (enabled ? " enabled" : " disabled"), enabled);
        Sounds.guiToggle(enabled);
        Config.save();
    }
}

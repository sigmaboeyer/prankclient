package com.prank;

import net.minecraft.client.MinecraftClient;

/** Swaps your real username for a fake one in all drawn text. */
public class NameProtect {
    private static final String[] NAMES = {"Kelo", "Player", "Protected", "Anonymous", "Guest", "Hidden"};
    private static String real;

    public static String apply(String s) {
        if (!Modules.NAMEPROTECT.enabled || s == null || s.isEmpty()) return s;
        if (real == null) {
            try { real = MinecraftClient.getInstance().getSession().getUsername(); } catch (Throwable t) { real = ""; }
        }
        if (real.isEmpty() || !s.contains(real)) return s;
        int i = Math.max(1, Math.min(NAMES.length, (int) Modules.NAMEPROTECT.num("Style")));
        return s.replace(real, NAMES[i - 1]);
    }
}

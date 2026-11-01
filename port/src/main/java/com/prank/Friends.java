package com.prank;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;

public class Friends {
    private static final Set<String> NAMES = new HashSet<>();

    public static boolean is(Entity e) {
        return e instanceof PlayerEntity && NAMES.contains(((PlayerEntity) e).getName().getString().toLowerCase());
    }

    public static void toggle(PlayerEntity p) {
        String n = p.getName().getString();
        String k = n.toLowerCase();
        if (NAMES.remove(k)) {
            Notifs.push("Removed friend " + n, false);
        } else {
            NAMES.add(k);
            Notifs.push("Added friend " + n, true);
        }
        Config.save();
    }

    public static String serialize() { return String.join(",", NAMES); }

    public static void load(String s) {
        NAMES.clear();
        if (s == null || s.isEmpty()) return;
        for (String n : s.split(",")) if (!n.isEmpty()) NAMES.add(n);
    }
}

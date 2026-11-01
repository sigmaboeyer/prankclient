package com.prank;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public class Sounds {
    private static SoundEvent ev(String name) {
        return SoundEvent.of(Identifier.tryParse("prankclient:" + name));
    }

    public static void play(String name, float vol, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSoundManager() == null) return;
        mc.getSoundManager().play(PositionedSoundInstance.master(ev(name), pitch, vol));
    }

    public static void playAt(String name, double x, double y, double z, float vol, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSoundManager() == null) return;
        mc.getSoundManager().play(new PositionedSoundInstance(ev(name), SoundCategory.PLAYERS,
            vol, pitch, Random.create(), x, y, z));
    }

    public static void guiToggle(boolean on) {
        if (!Modules.GUISOUNDS.enabled) return;
        play(on ? "on" : "off", (float) Modules.GUISOUNDS.num("Volume"), 1f);
    }

    /** Called for every sound the game plays. Returns true if we replaced it. */
    public static boolean intercept(Identifier id) {
        if (id == null || !"minecraft".equals(id.getNamespace())) return false;
        String p = id.getPath();
        if (Modules.CRITSOUND.enabled && p.equals("entity.player.attack.crit")) {
            play("crit", (float) Modules.CRITSOUND.num("Volume"), (float) Modules.CRITSOUND.num("Pitch"));
            return true;
        }
        if (Modules.HITSOUND.enabled && p.startsWith("entity.player.attack.") && !p.endsWith("crit")) {
            play("hit", (float) Modules.HITSOUND.num("Volume"), (float) Modules.HITSOUND.num("Pitch"));
            return true;
        }
        return false;
    }
}

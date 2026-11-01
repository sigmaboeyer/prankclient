package com.prank;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

/** Saved markers (with death points). Action buttons in the settings reset themselves. */
public class Waypoints {
    private static class W {
        final String name, dim;
        final double x, y, z;
        final int color;

        W(String name, String dim, double x, double y, double z, int color) {
            this.name = name; this.dim = dim; this.x = x; this.y = y; this.z = z; this.color = color;
        }
    }

    private static final List<W> LIST = new ArrayList<>();
    private static final int[] PALETTE = {0xFFFFE066, 0xFF55FFFF, 0xFFFF7BD5, 0xFF55FF55, 0xFFFF9F43, 0xFFA78BFA};
    private static int counter;
    private static boolean wasDead;
    private static double lx, ly, lz;

    private static String dim(MinecraftClient mc) { return mc.world.getRegistryKey().getValue().toString(); }

    private static void add(MinecraftClient mc, String name, double x, double y, double z) {
        LIST.add(new W(name, dim(mc), x, y, z, PALETTE[LIST.size() % PALETTE.length]));
        Modules.WAYPOINTS.enabled = true;
        Config.save();
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        Module m = Modules.WAYPOINTS;
        Module.Setting clr = m.find("Clear All"), rem = m.find("Remove Nearest");
        if (clr != null && clr.value > 0.5) {
            clr.value = 0;
            LIST.clear();
            Config.save();
            Notifs.push("Waypoints cleared", false);
        }
        if (rem != null && rem.value > 0.5) {
            rem.value = 0;
            W best = null;
            double bd = 1e18;
            for (W w : LIST) {
                double d = mc.player.squaredDistanceTo(w.x, w.y, w.z);
                if (w.dim.equals(dim(mc)) && d < bd) { bd = d; best = w; }
            }
            if (best != null) {
                LIST.remove(best);
                Config.save();
                Notifs.push("Removed " + best.name, false);
            }
        }
        if (Modules.WAYPOINT_ADD.enabled) {
            Modules.WAYPOINT_ADD.enabled = false;
            add(mc, "WP" + (++counter), mc.player.getX(), mc.player.getY(), mc.player.getZ());
            Notifs.push("Waypoint saved: WP" + counter, true);
        }
        boolean dead = mc.player.isDead() || mc.player.getHealth() <= 0f;
        if (!dead) {
            lx = mc.player.getX(); ly = mc.player.getY(); lz = mc.player.getZ();
        } else if (!wasDead && m.on("Death Points")) {
            add(mc, "Death", lx, ly, lz);
            Notifs.push("Death point saved", false);
        }
        wasDead = dead;
    }

    public static String serialize() {
        StringBuilder sb = new StringBuilder();
        for (W w : LIST) {
            if (sb.length() > 0) sb.append(';');
            sb.append(w.name).append('|').append(w.dim).append('|').append(w.x).append('|')
              .append(w.y).append('|').append(w.z).append('|').append(w.color);
        }
        return sb.toString();
    }

    public static void load(String s) {
        LIST.clear();
        if (s != null && !s.isEmpty()) {
            for (String e : s.split(";")) {
                String[] a = e.split("\\|");
                if (a.length < 6) continue;
                try {
                    LIST.add(new W(a[0], a[1], Double.parseDouble(a[2]), Double.parseDouble(a[3]),
                        Double.parseDouble(a[4]), Integer.parseInt(a[5])));
                } catch (Exception ignored) { }
            }
        }
        counter = LIST.size();
    }

    public static void world(Esp.Batch L) {
        Module m = Modules.WAYPOINTS;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!m.enabled || !m.on("Beams") || mc.world == null) return;
        String d = dim(mc);
        for (W w : LIST) {
            if (!w.dim.equals(d)) continue;
            L.ln(w.x, w.y - 64, w.z, w.x, w.y + 128, w.z, w.color);
            L.box(new Box(w.x - 0.3, w.y, w.z - 0.3, w.x + 0.3, w.y + 1.8, w.z + 0.3), w.color);
        }
    }

    public static void hud(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh) {
        Module m = Modules.WAYPOINTS;
        if (!m.enabled || mc.world == null) return;
        String d = dim(mc);
        double max = m.num("Max Distance");
        float sc = (float) m.num("Scale");
        for (W w : LIST) {
            if (!w.dim.equals(d)) continue;
            double dist = Math.sqrt(Esp.cam.squaredDistanceTo(w.x, w.y + 1, w.z));
            if (dist > max) continue;
            String label = w.name + "  " + (int) dist + "m";
            float[] s = Esp.project(w.x, w.y + 1.2, w.z, sw, sh);
            if (s != null && s[0] > 8 && s[0] < sw - 8 && s[1] > 8 && s[1] < sh - 8) {
                MatrixStack ms = ctx.getMatrices();
                ms.push();
                ms.translate(s[0], s[1], 0);
                ms.scale(sc, sc, 1f);
                int tw = tr.getWidth(label);
                ctx.fill(-3, -3, 3, 3, w.color);
                ctx.fill(-tw / 2 - 2, 5, tw / 2 + 2, 16, Access.bg());
                ctx.drawText(tr, label, -tw / 2, 7, w.color, false);
                ms.pop();
            } else if (m.on("Arrows")) {
                Esp.edgeMarker(ctx, tr, w.x - Esp.cam.x, w.z - Esp.cam.z, label, w.color, sw, sh);
            }
        }
    }
}

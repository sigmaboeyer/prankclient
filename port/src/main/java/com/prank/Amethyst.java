package com.prank;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds fully grown amethyst clusters in loaded chunks. Geodes only keep growing in loaded chunks,
 * so big, mature geodes are a hint that a player stayed near them for a long time (a base).
 */
public class Amethyst {
    public static class Geode {
        public double x, y, z, dist;
        public int count;
        public final List<BlockPos> blocks = new ArrayList<>();
    }

    private static final Map<Long, List<BlockPos>> CLUSTERS = new HashMap<>();
    public static List<Geode> geodes = new ArrayList<>();
    private static int cursor, known;
    public static int progress;   // 0..100

    private static void scan(MinecraftClient mc, int cx, int cz) {
        long key = ChunkPos.toLong(cx, cz);
        WorldChunk ch = mc.world.getChunkManager().getWorldChunk(cx, cz);
        if (ch == null) { CLUSTERS.remove(key); return; }
        ChunkSection[] secs = ch.getSectionArray();
        int baseY = ch.getBottomY();
        List<BlockPos> list = null;
        for (int i = 0; i < secs.length; i++) {
            ChunkSection s = secs[i];
            if (s == null || s.isEmpty()) continue;
            if (!s.hasAny(st -> st.isOf(Blocks.AMETHYST_CLUSTER))) continue;
            int y0 = baseY + i * 16;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (s.getBlockState(x, y, z).isOf(Blocks.AMETHYST_CLUSTER)) {
                            if (list == null) list = new ArrayList<>();
                            list.add(new BlockPos(cx * 16 + x, y0 + y, cz * 16 + z));
                        }
                    }
                }
            }
        }
        if (list == null) CLUSTERS.remove(key); else CLUSTERS.put(key, list);
    }

    private static void rebuild(MinecraftClient mc, int pcx, int pcz, int R) {
        CLUSTERS.keySet().removeIf(k ->
            Math.abs(ChunkPos.getPackedX(k) - pcx) > R + 1 || Math.abs(ChunkPos.getPackedZ(k) - pcz) > R + 1);

        List<Geode> gs = new ArrayList<>();
        for (List<BlockPos> list : CLUSTERS.values()) {
            for (BlockPos b : list) {
                Geode best = null;
                double bd = 144.0;
                for (Geode g : gs) {
                    double dx = g.x - b.getX(), dy = g.y - b.getY(), dz = g.z - b.getZ();
                    double d = dx * dx + dy * dy + dz * dz;
                    if (d < bd) { bd = d; best = g; }
                }
                if (best == null) {
                    best = new Geode();
                    best.x = b.getX(); best.y = b.getY(); best.z = b.getZ();
                    gs.add(best);
                }
                best.blocks.add(b);
                best.count++;
                best.x += (b.getX() - best.x) / best.count;
                best.y += (b.getY() - best.y) / best.count;
                best.z += (b.getZ() - best.z) / best.count;
            }
        }
        int min = (int) Modules.AMETHYST.num("Min Clusters");
        List<Geode> out = new ArrayList<>();
        for (Geode g : gs) {
            if (g.count < min) continue;
            g.dist = Math.sqrt(mc.player.squaredDistanceTo(g.x, g.y, g.z));
            out.add(g);
        }
        out.sort((a, b) -> Double.compare(a.dist, b.dist));
        int max = (int) Modules.AMETHYST.num("Max Targets");
        while (out.size() > max) out.remove(out.size() - 1);
        if (out.size() > known) {
            Sounds.play("drip", 1f, 1.3f);
            Notifs.push("Amethyst geode found " + (int) out.get(0).dist + "m away", true);
        }
        known = out.size();
        geodes = out;
    }

    public static void tick(MinecraftClient mc) {
        if (!Modules.AMETHYST.enabled || mc.world == null || mc.player == null) {
            if (!CLUSTERS.isEmpty() || !geodes.isEmpty()) {
                CLUSTERS.clear();
                geodes = new ArrayList<>();
                cursor = 0;
                known = 0;
                progress = 0;
            }
            return;
        }
        int R = (int) Modules.AMETHYST.num("Chunks");
        int side = R * 2 + 1, total = side * side;
        int pcx = mc.player.getChunkPos().x, pcz = mc.player.getChunkPos().z;
        for (int n = 0; n < 3; n++) {
            if (cursor >= total) cursor = 0;
            scan(mc, pcx - R + cursor % side, pcz - R + cursor / side);
            cursor++;
            progress = cursor * 100 / total;
            if (cursor >= total) {
                cursor = 0;
                rebuild(mc, pcx, pcz, R);
                break;
            }
        }
    }

    public static void worldLines(Esp.Batch L, Vec3d tracerStart) {
        if (!Modules.AMETHYST.enabled) return;
        int col = Modules.AMETHYST.getColor();
        boolean tracer = Modules.AMETHYST.on("Tracer"), boxes = Modules.AMETHYST.on("Boxes");
        List<Geode> gs = geodes;
        for (Geode g : gs) {
            if (tracer) L.line(tracerStart, new Vec3d(g.x + 0.5, g.y + 0.5, g.z + 0.5), col);
            if (boxes) {
                int n = 0;
                for (BlockPos b : g.blocks) {
                    L.box(new Box(b), col);
                    if (++n > 120) break;
                }
            }
        }
    }

    /** List panel + on-screen markers. */
    public static void hud(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh,
                           boolean editing, boolean dragging) {
        Module m = Modules.AMETHYST;
        if (!m.enabled) return;
        List<Geode> gs = geodes;
        int col = m.getColor();

        int lines = Math.max(1, gs.size());
        String head = gs.isEmpty() ? "Amethyst  scanning " + progress + "%" : "Amethyst Finder";
        int w = tr.getWidth(head) + 12;
        String[] rows = new String[gs.size()];
        for (int i = 0; i < rows.length; i++) {
            Geode g = gs.get(i);
            rows[i] = (int) g.x + " " + (int) g.y + " " + (int) g.z + "  " + (int) g.dist + "m  x" + g.count;
            w = Math.max(w, tr.getWidth(rows[i]) + 12);
        }
        int h = 14 + lines * 10 + 4;
        Hud.begin(ctx, m, 1f, 4, sh / 2 - 40, w, h, sw, sh);
        ctx.fill(0, 0, w, h, Access.bgStrong());
        ctx.fill(0, 0, w, 1, col);
        ctx.drawText(tr, head, 6, 4, col, false);
        for (int i = 0; i < rows.length; i++) ctx.drawText(tr, rows[i], 6, 15 + i * 10, 0xFFFFFFFF, false);
        Hud.end(ctx, m, editing, dragging);

        // markers
        for (int i = 0; i < gs.size() && i < 3; i++) {
            Geode g = gs.get(i);
            float[] s = Esp.project(g.x + 0.5, g.y + 0.5, g.z + 0.5, sw, sh);
            String label = "Amethyst " + (int) g.dist + "m";
            if (s != null && s[0] > 10 && s[0] < sw - 10 && s[1] > 10 && s[1] < sh - 10) {
                int x = (int) s[0], y = (int) s[1];
                ctx.fill(x - 3, y - 3, x + 3, y + 3, col);
                ctx.drawText(tr, label, x - tr.getWidth(label) / 2, y + 6, 0xFFFFFFFF, true);
            } else {
                Esp.edgeMarker(ctx, tr, g.x - Esp.cam.x, g.z - Esp.cam.z, label, col, sw, sh);
            }
        }
    }
}

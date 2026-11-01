package com.prank;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Esp {
    static final Matrix4f MV = new Matrix4f(), PR = new Matrix4f();
    static Vec3d cam = Vec3d.ZERO;
    static float camYaw;
    static final Map<Integer, ArrayDeque<Vec3d>> trails = new HashMap<>();
    static final int PURPLE = 0xFFA78BFA;
    static List<BlockPos> orePos = new ArrayList<>();
    static List<Integer> oreCol = new ArrayList<>();
    static double speed, lastX, lastZ;
    static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final ItemStack[] GEAR = new ItemStack[6];
    private static final List<PlayerEntity> TMP = new ArrayList<>();

    // ---------- batching ----------
    static class Batch {
        final BufferBuilder b;
        final Matrix4f m;
        int n = 0;

        Batch(VertexFormat.DrawMode mode, Matrix4f m) {
            this.b = Tessellator.getInstance().begin(mode, VertexFormats.POSITION_COLOR);
            this.m = m;
        }

        void v(double x, double y, double z, int c) {
            b.vertex(m, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).color(c);
            n++;
        }

        void ln(double x1, double y1, double z1, double x2, double y2, double z2, int c) {
            v(x1, y1, z1, c);
            v(x2, y2, z2, c);
        }

        void line(Vec3d a, Vec3d c2, int c) { ln(a.x, a.y, a.z, c2.x, c2.y, c2.z, c); }

        void box(Box k, int c) {
            double x1 = k.minX, y1 = k.minY, z1 = k.minZ, x2 = k.maxX, y2 = k.maxY, z2 = k.maxZ;
            ln(x1, y1, z1, x2, y1, z1, c); ln(x2, y1, z1, x2, y1, z2, c);
            ln(x2, y1, z2, x1, y1, z2, c); ln(x1, y1, z2, x1, y1, z1, c);
            ln(x1, y2, z1, x2, y2, z1, c); ln(x2, y2, z1, x2, y2, z2, c);
            ln(x2, y2, z2, x1, y2, z2, c); ln(x1, y2, z2, x1, y2, z1, c);
            ln(x1, y1, z1, x1, y2, z1, c); ln(x2, y1, z1, x2, y2, z1, c);
            ln(x2, y1, z2, x2, y2, z2, c); ln(x1, y1, z2, x1, y2, z2, c);
        }

        void fill(Box k, int c) {
            double x1 = k.minX, y1 = k.minY, z1 = k.minZ, x2 = k.maxX, y2 = k.maxY, z2 = k.maxZ;
            v(x1, y1, z1, c); v(x2, y1, z1, c); v(x2, y1, z2, c); v(x1, y1, z2, c);
            v(x1, y2, z1, c); v(x1, y2, z2, c); v(x2, y2, z2, c); v(x2, y2, z1, c);
            v(x1, y1, z1, c); v(x1, y2, z1, c); v(x2, y2, z1, c); v(x2, y1, z1, c);
            v(x1, y1, z2, c); v(x2, y1, z2, c); v(x2, y2, z2, c); v(x1, y2, z2, c);
            v(x1, y1, z1, c); v(x1, y1, z2, c); v(x1, y2, z2, c); v(x1, y2, z1, c);
            v(x2, y1, z1, c); v(x2, y2, z1, c); v(x2, y2, z2, c); v(x2, y1, z2, c);
        }

        void flush() {
            if (n > 0) BufferRenderer.drawWithGlobalProgram(b.end());
        }
    }

    // ---------- helpers ----------
    static Vec3d lerpPos(Entity e, float td) {
        return new Vec3d(MathHelper.lerp(td, e.prevX, e.getX()),
                         MathHelper.lerp(td, e.prevY, e.getY()),
                         MathHelper.lerp(td, e.prevZ, e.getZ()));
    }

    static Box box(Entity e, float td) {
        Vec3d p = lerpPos(e, td);
        return e.getBoundingBox().offset(p.x - e.getX(), p.y - e.getY(), p.z - e.getZ());
    }

    static int hpColor(float t) {
        if (Access.colorblind()) {
            int r = (int) (255 * (1 - t) + 58 * t), g = (int) (159 * (1 - t) + 141 * t), b = (int) (67 * (1 - t) + 255 * t);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        int r = (int) (255 * (1 - t)), g = (int) (255 * t);
        return 0xFF000000 | (r << 16) | (g << 8) | 60;
    }

    static float[] project(double x, double y, double z, int sw, int sh) {
        Vector4f v = new Vector4f((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z), 1f);
        v.mul(MV);
        v.mul(PR);
        if (v.w <= 0.05f) return null;
        return new float[]{(v.x / v.w * 0.5f + 0.5f) * sw, (1f - (v.y / v.w * 0.5f + 0.5f)) * sh};
    }

    static float[] screenRect(Box b, int sw, int sh) {
        float minX = 1e9f, minY = 1e9f, maxX = -1e9f, maxY = -1e9f;
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? b.minX : b.maxX;
            double y = (i & 2) == 0 ? b.minY : b.maxY;
            double z = (i & 4) == 0 ? b.minZ : b.maxZ;
            float[] s = project(x, y, z, sw, sh);
            if (s == null) return null;
            minX = Math.min(minX, s[0]); maxX = Math.max(maxX, s[0]);
            minY = Math.min(minY, s[1]); maxY = Math.max(maxY, s[1]);
        }
        return new float[]{minX, minY, maxX, maxY};
    }

    private static String fmt1(double v) { return String.valueOf(Math.round(v * 10) / 10.0); }

    static int oreColor(Block b) {
        if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) return 0xFF55FFFF;
        if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) return 0xFF55FF55;
        if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE) return 0xFFFFDD33;
        if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) return 0xFFD8AF93;
        if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) return 0xFF4D6BFF;
        if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) return 0xFFFF3333;
        if (b == Blocks.ANCIENT_DEBRIS) return 0xFF8B5A2B;
        if (b == Blocks.NETHER_QUARTZ_ORE) return 0xFFFFFFFF;
        return 0;
    }

    static void scanOres(MinecraftClient mc) {
        List<BlockPos> pos = new ArrayList<>();
        List<Integer> col = new ArrayList<>();
        BlockPos c = mc.player.getBlockPos();
        int R = (int) Modules.ORE_ESP.num("Radius");
        BlockPos.Mutable mp = new BlockPos.Mutable();
        for (int x = -R; x <= R; x++) {
            for (int y = -R; y <= R; y++) {
                for (int z = -R; z <= R; z++) {
                    mp.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                    int oc = oreColor(mc.world.getBlockState(mp).getBlock());
                    if (oc != 0) { pos.add(mp.toImmutable()); col.add(oc); }
                }
            }
        }
        orePos = pos;
        oreCol = col;
    }

    // ---------- tick ----------
    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            trails.clear();
            orePos = new ArrayList<>();
            oreCol = new ArrayList<>();
            return;
        }
        speed = Math.hypot(mc.player.getX() - lastX, mc.player.getZ() - lastZ) * 20.0;
        lastX = mc.player.getX();
        lastZ = mc.player.getZ();

        if (Modules.BREADCRUMBS.enabled) {
            Set<Integer> seen = new HashSet<>();
            for (PlayerEntity p : mc.world.getPlayers()) {
                seen.add(p.getId());
                ArrayDeque<Vec3d> q = trails.computeIfAbsent(p.getId(), k -> new ArrayDeque<>());
                q.addLast(p.getPos());
                while (q.size() > (int) Modules.BREADCRUMBS.num("Length")) q.removeFirst();
            }
            trails.keySet().retainAll(seen);
        } else if (!trails.isEmpty()) {
            trails.clear();
        }

        if (Modules.ORE_ESP.enabled) {
            if (mc.player.age % 20 == 0) scanOres(mc);
        } else if (!orePos.isEmpty()) {
            orePos = new ArrayList<>();
            oreCol = new ArrayList<>();
        }
    }

    // ---------- world rendering ----------
    static void skeleton(Batch L, PlayerEntity p, float td, int c) {
        Vec3d o = lerpPos(p, td);
        double yaw = Math.toRadians(MathHelper.lerp(td, p.prevBodyYaw, p.bodyYaw));
        Vec3d l = new Vec3d(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3d hip = o.add(0, 0.75, 0), neck = o.add(0, 1.4, 0), head = o.add(0, 1.7, 0);
        Vec3d sL = neck.add(l.multiply(0.3)), sR = neck.subtract(l.multiply(0.3));
        Vec3d hL = sL.add(0, -0.65, 0), hR = sR.add(0, -0.65, 0);
        Vec3d pL = hip.add(l.multiply(0.12)), pR = hip.subtract(l.multiply(0.12));
        Vec3d fL = o.add(l.multiply(0.12)), fR = o.subtract(l.multiply(0.12));
        L.line(head, neck, c); L.line(neck, hip, c); L.line(sL, sR, c);
        L.line(sL, hL, c); L.line(sR, hR, c); L.line(pL, pR, c);
        L.line(pL, fL, c); L.line(pR, fR, c);
    }

    public static void world(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        MatrixStack ms = ctx.matrixStack();
        if (ms == null) return;
        Matrix4f m = ms.peek().getPositionMatrix();
        cam = ctx.camera().getPos();
        camYaw = ctx.camera().getYaw();
        // the exact matrices the game uses right now, so 2D elements line up with the world
        MV.set(RenderSystem.getModelViewMatrix()).mul(m);
        PR.set(RenderSystem.getProjectionMatrix());
        float td = mc.getRenderTickCounter().getTickDelta(true);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(Access.lineWidth());

        // ---- lines ----
        Batch L = new Batch(VertexFormat.DrawMode.DEBUG_LINES, m);
        Vec3d tracerStart = cam.add(Vec3d.fromPolar(ctx.camera().getPitch(), camYaw).multiply(0.4));
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            Box bb = box(e, td);
            if (e instanceof PlayerEntity) {
                PlayerEntity p = (PlayerEntity) e;
                if (Modules.TRACERS.enabled) L.line(tracerStart, bb.getCenter(), Modules.TRACERS.getColor());
                if (Modules.THREE_D.enabled) L.box(bb, Modules.THREE_D.getColor());
                if (Modules.SKELETON.enabled) skeleton(L, p, td, Modules.SKELETON.getColor());
            } else if (e instanceof MobEntity) {
                if (Modules.MOB_ESP.enabled) L.box(bb, Modules.MOB_ESP.getColor());
            } else if (e instanceof ItemEntity) {
                if (Modules.ITEM_ESP.enabled) L.box(bb, Modules.ITEM_ESP.getColor());
            } else if (e instanceof ProjectileEntity) {
                if (Modules.PROJECTILE_ESP.enabled) L.box(bb, Modules.PROJECTILE_ESP.getColor());
            }
            if (Modules.HITBOXES.enabled && !(e instanceof PlayerEntity && Modules.THREE_D.enabled))
                L.box(bb, Modules.HITBOXES.getColor());
        }
        if (Freecam.active) {
            L.box(box(mc.player, td), 0xFFFFFFFF);
            if (Freecam.target != null)
                L.box(new Box(Freecam.target.getBlockPos()).expand(0.002),
                    Freecam.allowed ? 0xFF55FF55 : 0xFFFF5555);
        }
        if (Modules.STORAGE_ESP.enabled) {
            int pcx = mc.player.getChunkPos().x, pcz = mc.player.getChunkPos().z;
            int sc = Modules.STORAGE_ESP.getColor();
            for (int cx = pcx - 5; cx <= pcx + 5; cx++) {
                for (int cz = pcz - 5; cz <= pcz + 5; cz++) {
                    WorldChunk ch = mc.world.getChunk(cx, cz);
                    for (BlockEntity be : ch.getBlockEntities().values()) {
                        if (be instanceof LootableContainerBlockEntity || be instanceof EnderChestBlockEntity)
                            L.box(new Box(be.getPos()), sc);
                    }
                }
            }
        }
        if (Modules.ORE_ESP.enabled) {
            List<BlockPos> ps = orePos;
            List<Integer> cs = oreCol;
            for (int i = 0; i < ps.size() && i < cs.size(); i++)
                L.box(new Box(ps.get(i)), cs.get(i));
        }
        Amethyst.worldLines(L, tracerStart);
        Trajectory.draw(mc, L, td);
        Waypoints.world(L);
        if (Modules.BLOCKOUTLINE.enabled && mc.crosshairTarget instanceof BlockHitResult
            && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            L.box(new Box(bp).expand(0.002), Modules.BLOCKOUTLINE.getColor());
        }
        if (Modules.BREADCRUMBS.enabled) {
            int bc = Modules.BREADCRUMBS.getColor();
            for (ArrayDeque<Vec3d> q : trails.values()) {
                Vec3d prev = null;
                for (Vec3d v : q) {
                    if (prev != null)
                        L.ln(prev.x, prev.y + 0.05, prev.z, v.x, v.y + 0.05, v.z, bc);
                    prev = v;
                }
            }
        }
        L.flush();

        // ---- chams (filled boxes) ----
        if (Modules.CHAMS.enabled) {
            Batch Q = new Batch(VertexFormat.DrawMode.QUADS, m);
            int cc = (Access.fillAlpha() << 24) | (Modules.CHAMS.getColor() & 0xFFFFFF);
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                Q.fill(box(p, td), cc);
            }
            Q.flush();
        }

        // ---- china hat ----
        if (Modules.CHINA_HAT.enabled) {
            Batch T = new Batch(VertexFormat.DrawMode.TRIANGLES, m);
            int seg = 16;
            int rgb = Modules.CHINA_HAT.getColor() & 0xFFFFFF;
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                Vec3d o = lerpPos(p, td);
                double h = p.getHeight();
                for (int i = 0; i < seg; i++) {
                    double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
                    T.v(o.x, o.y + h + 0.3, o.z, 0xDD000000 | rgb);
                    T.v(o.x + Math.cos(a0) * 0.5, o.y + h + 0.02, o.z + Math.sin(a0) * 0.5, 0x88000000 | rgb);
                    T.v(o.x + Math.cos(a1) * 0.5, o.y + h + 0.02, o.z + Math.sin(a1) * 0.5, 0x88000000 | rgb);
                }
            }
            T.flush();
        }

        RenderSystem.lineWidth(1f);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    // ---------- HUD ----------
    static void outline(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        c.fill(x1, y1, x2, y1 + 1, col);
        c.fill(x1, y2 - 1, x2, y2, col);
        c.fill(x1, y1, x1 + 1, y2, col);
        c.fill(x2 - 1, y1, x2, y2, col);
    }

    /** A marker on the edge of the screen pointing toward something (dx/dz are world offsets). */
    static void edgeMarker(DrawContext ctx, TextRenderer tr, double dx, double dz, String label,
                           int col, int sw, int sh) {
        double th = Math.toRadians(camYaw);
        double f = dx * -Math.sin(th) + dz * Math.cos(th);
        double r = dx * -Math.cos(th) + dz * -Math.sin(th);
        double len = Math.max(0.001, Math.hypot(f, r));
        double rad = Modules.ARROWS.num("Radius");
        int ax = (int) (sw / 2 + r / len * rad), ay = (int) (sh / 2 - f / len * rad);
        ctx.fill(ax - 3, ay - 3, ax + 3, ay + 3, col);
        ctx.drawText(tr, label, ax - tr.getWidth(label) / 2, ay + 5, 0xFFFFFFFF, false);
    }

    /** Gear row, then a plate with name / ping / health / distance, sitting right on the player's head. */
    static void nametag(DrawContext ctx, TextRenderer tr, MinecraftClient mc, PlayerEntity p, float td, int sw, int sh) {
        Module nt = Modules.NAMETAGS;
        Vec3d o = lerpPos(p, td);
        double dist = cam.distanceTo(o);
        if (dist > nt.num("Max Distance")) return;
        double off = (nt.on("Hide Vanilla") ? 0.15 : 0.65) + nt.num("Y Offset");
        float[] s = project(o.x, o.y + p.getHeight() + off, o.z, sw, sh);
        if (s == null) return;
        float sc = (float) (nt.num("Scale") * MathHelper.clamp(1.9 - dist / 22.0, 0.45, 1.2)) * Access.hudScale();

        int n = 0;
        if (nt.on("Items")) {
            for (EquipmentSlot sl : SLOTS) {
                if ((sl == EquipmentSlot.MAINHAND || sl == EquipmentSlot.OFFHAND) && !nt.on("Held Items")) continue;
                ItemStack st = p.getEquippedStack(sl);
                if (!st.isEmpty()) GEAR[n++] = st;
            }
        }

        String nm = p.getName().getString();
        float hp = p.getHealth() + p.getAbsorptionAmount();
        float t = MathHelper.clamp(hp / p.getMaxHealth(), 0f, 1f);
        String hs = nt.on("Health") ? " " + fmt1(hp) : "";
        String ps = "";
        if (nt.on("Ping") && mc.getNetworkHandler() != null) {
            PlayerListEntry pe = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (pe != null) ps = " " + pe.getLatency() + "ms";
        }
        String ds = nt.on("Distance") ? " " + fmt1(dist) + "m" : "";
        int wName = tr.getWidth(nm), wPing = tr.getWidth(ps), wHp = tr.getWidth(hs), wDs = tr.getWidth(ds);
        int plateW = wName + wPing + wHp + wDs + 10, plateH = 13;
        int rowW = n * 15, rowH = n > 0 ? 20 : 0;
        boolean bg = nt.on("Background");
        boolean shadow = !bg;
        int nameCol = Friends.is(p) ? 0xFF55FF55 : nt.getColor();

        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(s[0], s[1], 0);
        ms.scale(sc, sc, 1f);

        int x0 = -plateW / 2;
        if (bg) {
            ctx.fill(x0, -plateH, x0 + plateW, 0, Access.bgStrong());
            ctx.fill(x0, -plateH, x0 + plateW, -plateH + 1, nameCol);
        }
        int tx = x0 + 5, ty = -plateH + 3;
        ctx.drawText(tr, nm, tx, ty, nameCol, shadow);
        tx += wName;
        if (wPing > 0) { ctx.drawText(tr, ps, tx, ty, 0xFF55FFFF, shadow); tx += wPing; }
        if (wHp > 0) { ctx.drawText(tr, hs, tx, ty, hpColor(t), shadow); tx += wHp; }
        if (wDs > 0) ctx.drawText(tr, ds, tx, ty, 0xFFAAAAAA, shadow);

        if (n > 0) {
            int ix = -rowW / 2, iy = -plateH - rowH;
            boolean dur = nt.on("Durability");
            for (int i = 0; i < n; i++) {
                ItemStack st = GEAR[i];
                ms.push();
                ms.translate(ix + 1, iy + 1, 0);
                ms.scale(0.8f, 0.8f, 1f);
                ctx.drawItem(st, 0, 0);
                ms.pop();
                if (st.isItemBarVisible()) {
                    ctx.fill(ix + 2, iy + 12, ix + 13, iy + 13, 200, 0xFF000000);
                    ctx.fill(ix + 2, iy + 12, ix + 2 + st.getItemBarStep() * 11 / 13, iy + 13, 200,
                        0xFF000000 | st.getItemBarColor());
                }
                if (dur && st.isDamageable()) {
                    String d = String.valueOf(st.getMaxDamage() - st.getDamage());
                    ms.push();
                    ms.translate(ix + 7 - tr.getWidth(d) * 0.25f, iy + 14, 0);
                    ms.scale(0.5f, 0.5f, 1f);
                    ctx.drawText(tr, d, 0, 0, 0xFFFFFFFF, true);
                    ms.pop();
                }
                GEAR[i] = null;
                ix += 15;
            }
        }
        ms.pop();
    }

    public static void hud(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;
        float td = tc.getTickDelta(true);
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        TextRenderer tr = mc.textRenderer;
        boolean cards = Modules.NAMETAGS.enabled;
        boolean rects = Modules.TWO_D.enabled || Modules.HEALTH.enabled;

        if (cards || rects || Modules.ARROWS.enabled) {
            TMP.clear();
            for (PlayerEntity p : mc.world.getPlayers()) if (p != mc.player) TMP.add(p);
            // far players first so near ones draw on top
            TMP.sort((a, b) -> Double.compare(cam.distanceTo(b.getPos()), cam.distanceTo(a.getPos())));
            for (PlayerEntity p : TMP) {
                Box bb = box(p, td);
                Vec3d c = bb.getCenter();
                float[] cs = project(c.x, c.y, c.z, sw, sh);
                boolean on = cs != null && cs[0] > -40 && cs[0] < sw + 40 && cs[1] > -40 && cs[1] < sh + 40;
                if (!on) {
                    if (Modules.ARROWS.enabled && cam.distanceTo(c) > 2)
                        edgeMarker(ctx, tr, c.x - cam.x, c.z - cam.z, (int) cam.distanceTo(c) + "m",
                            Modules.ARROWS.getColor(), sw, sh);
                    continue;
                }
                if (rects) {
                    float[] r = screenRect(bb, sw, sh);
                    if (r != null) {
                        int x1 = (int) r[0], y1 = (int) r[1], x2 = (int) r[2], y2 = (int) r[3];
                        if (Modules.TWO_D.enabled) outline(ctx, x1, y1, x2, y2, Modules.TWO_D.getColor());
                        if (Modules.HEALTH.enabled) {
                            float hp = p.getHealth() + p.getAbsorptionAmount();
                            float t = MathHelper.clamp(hp / p.getMaxHealth(), 0f, 1f);
                            int bx = x1 - 4;
                            ctx.fill(bx, y1, bx + 2, y2, 0xAA000000);
                            ctx.fill(bx, y2 - (int) ((y2 - y1) * t), bx + 2, y2, hpColor(t));
                        }
                    }
                }
                if (cards) nametag(ctx, tr, mc, p, td, sw, sh);
            }
            TMP.clear();
        }
        Overlay.draw(ctx, tr, mc, sw, sh, td);
    }
}

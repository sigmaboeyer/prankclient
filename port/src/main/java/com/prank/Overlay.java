package com.prank;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** All the extra HUD pieces. Movable ones are positioned through Hud.begin/end. */
public class Overlay {
    private static long last;
    private static final float[] K = new float[6];
    private static float thAnim, thHp, thTrail, thFlash;
    private static LivingEntity thEnt, thLast, teEnt;
    private static float teAnim;
    private static long thSeen;

    private static final List<Module> ACT = new ArrayList<>();
    private static final List<Module> KB = new ArrayList<>();
    private static final List<PlayerEntity> PL = new ArrayList<>();
    private static final List<String> ROWS = new ArrayList<>();
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
        EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final EquipmentSlot[] ALL_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
        EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private static float step(float c, float t, float r) {
        float d = t - c;
        if (Math.abs(d) < 0.01f) return t;
        return c + d * Math.min(1f, r);
    }

    private static float ease(float t) {
        float u = 1f - Math.max(0f, Math.min(1f, t));
        return 1f - u * u * u;
    }

    private static int mixA(int a, int b, float t) {
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
            | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** The current target: what you look at, else the nearest player, kept for a moment after. */
    public static LivingEntity target(MinecraftClient mc) {
        LivingEntity t = null;
        if (mc.targetedEntity instanceof LivingEntity) {
            t = (LivingEntity) mc.targetedEntity;
        } else {
            double best = 8;
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                double d = mc.player.distanceTo(p);
                if (d < best) { best = d; t = p; }
            }
        }
        long now = System.currentTimeMillis();
        if (t != null) { thLast = t; thSeen = now; return t; }
        if (thLast != null && thLast.isAlive() && now - thSeen < 1200) return thLast;
        thLast = null;
        return null;
    }

    public static void draw(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh, float td) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0.016f : Math.min(0.1f, (now - last) / 1.0e9f);
        last = now;
        boolean ed = mc.currentScreen instanceof ModuleScreen;
        Module drag = ModuleScreen.dragged;

        watermark(ctx, tr, mc, sw, sh, ed, drag);
        Notifs.draw(ctx, tr, sw);
        arraylist(ctx, tr, sw, sh, dt, ed, drag);
        keystrokes(ctx, tr, mc, sw, sh, dt, ed, drag);
        armorHud(ctx, tr, mc, sw, sh, ed, drag);
        effects(ctx, tr, mc, sw, sh, ed, drag);
        info(ctx, tr, mc, sw, sh, ed, drag);
        playerList(ctx, tr, mc, sw, sh, ed, drag);
        radar(ctx, mc, sw, sh, ed, drag);
        targetHud(ctx, tr, mc, sw, sh, dt, ed, drag);
        keybinds(ctx, tr, sw, sh, ed, drag);
        crosshair(ctx, mc, sw, sh);
        hitMarker(ctx, sw, sh, now);
        targetEsp(ctx, mc, sw, sh, td, dt);
        Fx.popups(ctx, tr, sw, sh);
        damageFlash(ctx, mc, sw, sh, dt);
        compass(ctx, tr, sw, sh, ed, drag);
        Waypoints.hud(ctx, tr, mc, sw, sh);
        Amethyst.hud(ctx, tr, mc, sw, sh, ed, Modules.AMETHYST == drag);
    }

    // ---------------- damage flash ----------------
    private static float lastHp = -1f, flash;

    private static void damageFlash(DrawContext ctx, MinecraftClient mc, int sw, int sh, float dt) {
        Module m = Modules.DAMAGEFLASH;
        if (!m.enabled) { lastHp = -1f; flash = 0f; return; }
        float hp = mc.player.getHealth();
        if (lastHp >= 0f && hp < lastHp - 0.01f) flash = 1f;
        lastHp = hp;
        flash = Access.motion() ? step(flash, 0f, dt * 3f / (float) m.num("Duration")) : (flash > 0.5f ? 0.5f : 0f);
        float low = 0f;
        if (m.on("Low Health") && hp > 0f && hp <= m.num("Threshold"))
            low = Access.motion() ? 0.35f + 0.25f * (float) Math.sin(System.currentTimeMillis() / 180.0) : 0.4f;
        float a = Math.max(flash * 0.6f, low);
        if (a < 0.02f) return;
        int rgb = m.getColor() & 0xFFFFFF;
        int edge = Math.max(12, sh / 8);
        int t = edge / 6 + 1;
        for (int i = 0; i < 6; i++) {
            int al = (int) (255 * a * (1f - i / 6f) * 0.5f);
            if (al < 4) continue;
            int c = (al << 24) | rgb;
            int o = i * edge / 6;
            ctx.fill(0, o, sw, o + t, c);
            ctx.fill(0, sh - o - t, sw, sh - o, c);
            ctx.fill(o, 0, o + t, sh, c);
            ctx.fill(sw - o - t, 0, sw - o, sh, c);
        }
    }

    // ---------------- compass ----------------
    private static final String[] CD = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private static final float[] CA = {0f, 45f, 90f, 135f, 180f, -135f, -90f, -45f};

    private static void compass(DrawContext ctx, TextRenderer tr, int sw, int sh, boolean ed, Module drag) {
        Module m = Modules.COMPASS;
        if (!m.enabled) return;
        int w = 160, h = 18;
        float sc = (float) m.num("Scale");
        Hud.begin(ctx, m, sc, (sw - (int) (w * sc * Access.hudScale())) / 2, sh - 62, w, h, sw, sh);
        ctx.fill(0, 0, w, h, Access.bg());
        int col = m.getColor();
        ctx.fill(w / 2 - 1, 0, w / 2 + 1, 3, col);
        float yaw = Esp.camYaw;
        for (int t = 0; t < 24; t++) {
            float d = MathHelper.wrapDegrees(t * 15f - 180f - yaw);
            int a = (int) (200 * (1f - Math.abs(d) / 60f));
            if (a < 12) continue;
            int x = (int) (w / 2 + d * (w / 120f));
            ctx.fill(x, h - 3, x + 1, h - 1, (a << 24) | 0xAAAAAA);
        }
        for (int i = 0; i < 8; i++) {
            float d = MathHelper.wrapDegrees(CA[i] - yaw);
            int a = (int) (255 * (1f - Math.abs(d) / 60f));
            if (a < 12) continue;
            int x = (int) (w / 2 + d * (w / 120f));
            boolean card = CD[i].length() == 1;
            ctx.drawText(tr, CD[i], x - tr.getWidth(CD[i]) / 2, 5, (a << 24) | (card ? 0xFFFFFF : (col & 0xFFFFFF)), false);
        }
        Hud.end(ctx, m, ed, m == drag);
    }

    // ---------------- watermark ----------------
    private static void watermark(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh,
                                  boolean ed, Module drag) {
        Module m = Modules.WATERMARK;
        if (!m.enabled) return;
        Text title = Text.literal("Kelo Client").setStyle(Style.EMPTY.withBold(true).withColor(m.getColor() & 0xFFFFFF));
        String fps = m.on("Show FPS") ? mc.getCurrentFps() + " fps" : "";
        int w = tr.getWidth(title) + tr.getWidth(fps) + 22, h = 16;
        Hud.begin(ctx, m, 1f, (sw - w) / 2, 3, w, h, sw, sh);
        ctx.fill(0, 0, w, h, Access.bgStrong());
        ctx.fill(0, 0, w, 1, Module.rainbow(0f));
        ctx.fill(0, h - 1, w, h, Module.rainbow(0.5f));
        ctx.drawText(tr, title, 6, 4, 0xFFFFFFFF, true);
        ctx.drawText(tr, fps, 6 + tr.getWidth(title) + 10, 4, 0xFFAAAAAA, false);
        Hud.end(ctx, m, ed, m == drag);
    }

    // ---------------- arraylist ----------------
    private static void arraylist(DrawContext ctx, TextRenderer tr, int sw, int sh, float dt,
                                  boolean ed, Module drag) {
        Module m = Modules.ARRAYLIST;
        if (!m.enabled) return;
        ACT.clear();
        for (Module x : Modules.ALL) {
            float tgt = (x.enabled && !x.category.equals("GUI") && x != m) ? 1f : 0f;
            x.hudAnim = Access.motion() ? step(x.hudAnim, tgt, dt * 10f) : tgt;
            if (x.hudAnim > 0.01f) ACT.add(x);
        }
        ACT.sort((a, b) -> Integer.compare(tr.getWidth(b.name), tr.getWidth(a.name)));
        int w = 40;
        for (Module x : ACT) w = Math.max(w, tr.getWidth(x.name) + 8);
        int h = Math.max(11, ACT.size() * 11);
        float sc = (float) m.num("Scale");
        Hud.begin(ctx, m, sc, sw - (int) (w * sc) - 2, 4, w, h, sw, sh);
        int y = 0, i = 0;
        for (Module x : ACT) {
            float e = ease(x.hudAnim);
            int ww = tr.getWidth(x.name) + 8;
            int xx = w - (int) (ww * e);
            int col = m.on("Rainbow") ? Module.rainbow(i * 0.06f) : 0xFF2EC4FF;
            ctx.fill(xx, y, w, y + 11, Access.bg());
            ctx.fill(w - 2, y, w, y + 11, col);
            ctx.drawText(tr, x.name, xx + 3, y + 2, col, false);
            y += (int) (11 * e);
            i++;
        }
        Hud.end(ctx, m, ed, m == drag);
    }

    // ---------------- keystrokes ----------------
    private static void key(DrawContext ctx, TextRenderer tr, int i, boolean down, String label,
                            int x, int y, int w, int h, float dt) {
        K[i] = step(K[i], down ? 1f : 0f, dt * 18f);
        int accent = Modules.KEYSTROKES.getColor();
        int bg = mixA(Access.bg(), 0xE0000000 | (accent & 0xFFFFFF), K[i]);
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.fill(x, y, x + w, y + 1, accent);
        ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2 + 1,
            K[i] > 0.5f ? 0xFF101018 : 0xFFFFFFFF, false);
    }

    private static void keystrokes(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh,
                                   float dt, boolean ed, Module drag) {
        Module m = Modules.KEYSTROKES;
        if (!m.enabled) return;
        float sc = (float) m.num("Scale");
        Hud.begin(ctx, m, sc, sw - (int) (80 * sc), sh - (int) (72 * sc), 76, 66, sw, sh);
        key(ctx, tr, 0, mc.options.forwardKey.isPressed(), "W", 26, 0, 24, 22, dt);
        key(ctx, tr, 1, mc.options.leftKey.isPressed(), "A", 0, 24, 24, 22, dt);
        key(ctx, tr, 2, mc.options.backKey.isPressed(), "S", 26, 24, 24, 22, dt);
        key(ctx, tr, 3, mc.options.rightKey.isPressed(), "D", 52, 24, 24, 22, dt);
        key(ctx, tr, 4, mc.options.attackKey.isPressed(), "LMB", 0, 48, 37, 18, dt);
        key(ctx, tr, 5, mc.options.useKey.isPressed(), "RMB", 39, 48, 37, 18, dt);
        Hud.end(ctx, m, ed, m == drag);
    }

    // ---------------- armor hud ----------------
    private static void armorHud(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh,
                                 boolean ed, Module drag) {
        Module m = Modules.ARMOR_HUD;
        if (!m.enabled) return;
        int n = 0;
        for (EquipmentSlot sl : ALL_SLOTS) if (!mc.player.getEquippedStack(sl).isEmpty()) n++;
        int w = 62, h = Math.max(19, n * 19);
        float sc = (float) m.num("Scale");
        Hud.begin(ctx, m, sc, sw - (int) (66 * sc), sh / 2 - 60, w, h, sw, sh);
        int y = 0;
        for (EquipmentSlot sl : ALL_SLOTS) {
            ItemStack st = mc.player.getEquippedStack(sl);
            if (st.isEmpty()) continue;
            ctx.fill(0, y, w, y + 18, Access.bg());
            ctx.drawItem(st, 1, y + 1);
            String t = st.isDamageable() ? String.valueOf(st.getMaxDamage() - st.getDamage())
                : (st.getCount() > 1 ? "x" + st.getCount() : "");
            ctx.drawText(tr, t, 20, y + 5, 0xFFFFFFFF, false);
            y += 19;
        }
        Hud.end(ctx, m, ed, m == drag);
    }

    // ---------------- effects ----------------
    private static void effects(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh,
                                boolean ed, Module drag) {
        Module m = Modules.EFFECTS_HUD;
        if (!m.enabled) return;
        ROWS.clear();
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String nm = e.getEffectType().value().getName().getString();
            int amp = e.getAmplifier() + 1;
            int sec = e.getDuration() / 20;
            String time = e.getDuration() < 0 ? "inf" : (sec / 60) + ":" + String.format("%02d", sec % 60);
            ROWS.add(nm + (amp > 1 ? " " + amp : "") + "  " + time);
        }
        if (ROWS.isEmpty()) {
            if (!ed) return;
            ROWS.add("Speed 2  1:30");
        }
        int w = 0;
        for (String s : ROWS) w = Math.max(w, tr.getWidth(s) + 8);
        int h = ROWS.size() * 12;
        float sc = (float) m.num("Scale");
        Hud.begin(ctx, m, sc, sw - (int) (w * sc) - 4, sh / 2 + 62, w, h, sw, sh);
        for (int i = 0; i < ROWS.size(); i++) {
            ctx.fill(0, i * 12, w, i * 12 + 11, Access.bg());
            ctx.drawText(tr, ROWS.get(i), 4, i * 12 + 2, 0xFFFFFFFF, false);
        }
        Hud.end(ctx, m, ed, m == drag);
    }

    // ---------------- info hud ----------------
    private static void info(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh,
                             boolean ed, Module drag) {
        Module m = Modules.INFO_HUD;
        if (!m.enabled) return;
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry pe = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (pe != null) ping = pe.getLatency();
        }
        String[] lines = {
            String.format("XYZ  %.1f  %.1f  %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()),
            "FPS  " + mc.getCurrentFps(),
            "Ping  " + ping + " ms",
            String.format("Speed  %.1f b/s", Esp.speed),
            "Facing  " + mc.player.getHorizontalFacing().asString()
        };
        int w = 0;
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        w += 14;
        int h = lines.length * 10 + 8;
        float sc = (float) m.num("Scale");
        Hud.begin(ctx, m, sc, 4, 4, w, h, sw, sh);
        if (m.on("Background")) ctx.fill(0, 0, w, h, Access.bg());
        ctx.fill(0, 0, 2, h, m.getColor());
        for (int i = 0; i < lines.length; i++) ctx.drawText(tr, lines[i], 8, 4 + i * 10, m.getColor(), false);
        Hud.end(ctx, m, ed, m == drag);
    }

    // ---------------- player list ----------------
    private static void playerList(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh,
                                   boolean ed, Module drag) {
        Module m = Modules.PLAYER_LIST;
        if (!m.enabled) return;
        PL.clear();
        for (PlayerEntity p : mc.world.getPlayers()) if (p != mc.player) PL.add(p);
        PL.sort((a, b) -> Double.compare(mc.player.distanceTo(a), mc.player.distanceTo(b)));
        int n = Math.min(PL.size(), (int) m.num("Count"));
        if (n == 0 && !ed) { PL.clear(); return; }
        int rows = Math.max(1, n);
        int w = tr.getWidth("Nearby") + 16;
        for (int i = 0; i < n; i++) {
            PlayerEntity p = PL.get(i);
            String r = (int) (p.getHealth() + p.getAbsorptionAmount()) + "  " + (int) mc.player.distanceTo(p) + "m";
            w = Math.max(w, tr.getWidth(p.getName().getString()) + tr.getWidth(r) + 24);
        }
        int h = 14 + rows * 10 + 4;
        float sc = (float) m.num("Scale");
        Module info = Modules.INFO_HUD;
        int defY = info.enabled ? info.dy + info.dh + 4 : 4;
        Hud.begin(ctx, m, sc, 4, defY, w, h, sw, sh);
        if (m.on("Background")) ctx.fill(0, 0, w, h, Access.bg());
        ctx.fill(0, 0, 2, h, m.getColor());
        ctx.drawText(tr, "Nearby", 8, 4, m.getColor(), false);
        for (int i = 0; i < n; i++) {
            PlayerEntity p = PL.get(i);
            float hp = p.getHealth() + p.getAbsorptionAmount();
            float t = MathHelper.clamp(hp / p.getMaxHealth(), 0f, 1f);
            String r = (int) hp + "  " + (int) mc.player.distanceTo(p) + "m";
            int ly = 15 + i * 10;
            ctx.drawText(tr, p.getName().getString(), 8, ly, Friends.is(p) ? 0xFF55FF55 : 0xFFFFFFFF, false);
            ctx.drawText(tr, r, w - 6 - tr.getWidth(r), ly, Esp.hpColor(t), false);
        }
        PL.clear();
        Hud.end(ctx, m, ed, m == drag);
    }

    // ---------------- radar ----------------
    private static void radar(DrawContext ctx, MinecraftClient mc, int sw, int sh, boolean ed, Module drag) {
        Module m = Modules.RADAR;
        if (!m.enabled) return;
        float sc = (float) m.num("Scale");
        Hud.begin(ctx, m, sc, 4, sh - (int) (100 * sc) - 4, 100, 100, sw, sh);
        int cx = 50, cy = 50;
        ctx.fill(0, 0, 100, 100, Access.bg());
        ctx.fill(0, cy, 100, cy + 1, 0x40FFFFFF);
        ctx.fill(cx, 0, cx + 1, 100, 0x40FFFFFF);
        double th = Math.toRadians(Esp.camYaw);
        double s = 48.0 / m.num("Range");
        int pc = m.getColor();
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            boolean pl = e instanceof PlayerEntity;
            if (!pl && !(e instanceof MobEntity)) continue;
            double dx = e.getX() - Esp.cam.x, dz = e.getZ() - Esp.cam.z;
            double f = dx * -Math.sin(th) + dz * Math.cos(th);
            double r = dx * -Math.cos(th) + dz * -Math.sin(th);
            int x = (int) (cx + r * s), y = (int) (cy - f * s);
            if (Math.abs(x - cx) > 48 || Math.abs(y - cy) > 48) continue;
            ctx.fill(x - 1, y - 1, x + 2, y + 2, pl ? (Friends.is(e) ? 0xFF55FF55 : pc) : 0xFF55FF55);
        }
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        Hud.end(ctx, m, ed, m == drag);
    }

    // ---------------- target hud ----------------
    private static void targetHud(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh,
                                  float dt, boolean ed, Module drag) {
        Module m = Modules.TARGET_HUD;
        if (!m.enabled) { thAnim = 0f; return; }
        LivingEntity t = target(mc);
        boolean animate = m.on("Animate") && Access.motion();
        float sp = (float) m.num("Smooth");
        float tgt = (t != null || ed) ? 1f : 0f;
        thAnim = animate ? step(thAnim, tgt, dt * 8f) : tgt;
        if (thAnim < 0.02f) return;

        float hp = 15f, max = 20f, dist = 0f;
        String name = "Preview";
        if (t != null) {
            hp = t.getHealth() + t.getAbsorptionAmount();
            max = t.getMaxHealth();
            name = t.getName().getString();
            dist = mc.player.distanceTo(t);
        }
        float ratio = MathHelper.clamp(hp / max, 0f, 1f);
        if (t != thEnt) { thEnt = t; thHp = ratio; thTrail = ratio; }
        if (ratio < thHp - 0.001f) thFlash = 1f;
        thFlash = step(thFlash, 0f, dt * 4f);
        thHp = animate ? step(thHp, ratio, dt * 12f * sp) : ratio;
        thTrail = animate ? step(thTrail, ratio, dt * 3f * sp) : ratio;

        int w = 150, h = 46;
        float sc = (float) m.num("Scale");
        Hud.begin(ctx, m, sc, sw / 2 + 20, sh / 2 + 24, w, h, sw, sh);
        MatrixStack ms = ctx.getMatrices();
        ms.translate(0, (1f - ease(thAnim)) * 12f, 0);
        int col = m.getColor();
        if (m.on("Background")) ctx.fill(0, 0, w, h, Access.bgStrong());
        ctx.fill(0, 0, (int) (w * ease(thAnim)), 1, col);
        ctx.drawText(tr, name, 6, 5, 0xFFFFFFFF, true);
        String ds = String.format("%.1fm", dist);
        ctx.drawText(tr, ds, w - 6 - tr.getWidth(ds), 5, 0xFFAAAAAA, false);

        int bx = 6, by = 18, bw = w - 12, bh = 8;
        ctx.fill(bx, by, bx + bw, by + bh, 0xFF2A2A37);
        ctx.fill(bx, by, bx + (int) (bw * thTrail), by + bh, 0x99FFFFFF);
        ctx.fill(bx, by, bx + (int) (bw * thHp), by + bh, Esp.hpColor(thHp));
        if (thFlash > 0.02f) ctx.fill(bx, by, bx + bw, by + bh, ((int) (0x90 * thFlash) << 24) | 0xFFFFFF);
        ctx.fill(bx, by + bh, bx + (int) (bw * thHp), by + bh + 1, col);

        ctx.drawText(tr, String.format("%.1f / %.0f", hp, max), 6, 32, 0xFFCCCCCC, false);
        if (m.on("Armor") && t != null) {
            int ix = w - 6 - 4 * 12;
            for (EquipmentSlot sl : ARMOR_SLOTS) {
                ItemStack st = t.getEquippedStack(sl);
                if (!st.isEmpty()) {
                    ms.push();
                    ms.translate(ix, 29, 0);
                    ms.scale(0.7f, 0.7f, 1f);
                    ctx.drawItem(st, 0, 0);
                    ms.pop();
                }
                ix += 12;
            }
        }
        Hud.end(ctx, m, ed, m == drag);
    }

    // ---------------- keybinds ----------------
    private static void keybinds(DrawContext ctx, TextRenderer tr, int sw, int sh, boolean ed, Module drag) {
        Module m = Modules.KEYBINDS_HUD;
        if (!m.enabled) return;
        KB.clear();
        for (Module x : Modules.ALL) if (x.key > 0) KB.add(x);
        if (KB.isEmpty() && !ed) return;
        int w = tr.getWidth("Keybinds") + 12;
        for (Module x : KB) w = Math.max(w, tr.getWidth(x.name + "  [" + x.keyLabel() + "]") + 12);
        int h = 14 + Math.max(1, KB.size()) * 10 + 4;
        float sc = (float) m.num("Scale");
        Hud.begin(ctx, m, sc, 4, sh / 2 - 20, w, h, sw, sh);
        ctx.fill(0, 0, w, h, Access.bg());
        ctx.fill(0, 0, 2, h, Module.rainbow(0f));
        ctx.drawText(tr, "Keybinds", 8, 4, 0xFFFFFFFF, false);
        for (int i = 0; i < KB.size(); i++) {
            Module x = KB.get(i);
            ctx.drawText(tr, x.name + "  [" + x.keyLabel() + "]", 8, 15 + i * 10,
                x.enabled ? 0xFF55FF55 : 0xFFAAAAAA, false);
        }
        KB.clear();
        Hud.end(ctx, m, ed, m == drag);
    }

    // ---------------- crosshair ----------------
    private static void crosshair(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        Module m = Modules.CROSSHAIR;
        if (!m.enabled) return;
        int cx = sw / 2, cy = sh / 2;
        int size = (int) m.num("Size"), gap = (int) m.num("Gap"), th = (int) m.num("Thickness");
        int col = m.getColor();
        int g = gap + (mc.player.isSprinting() ? 2 : 0);
        int a = th / 2, b = a + th;
        ctx.fill(cx - g - size, cy - a, cx - g, cy - a + th, col);
        ctx.fill(cx + g, cy - a, cx + g + size, cy - a + th, col);
        ctx.fill(cx - a, cy - g - size, cx - a + th, cy - g, col);
        ctx.fill(cx - a, cy + g, cx - a + th, cy + g + size, col);
        if (m.on("Dot")) ctx.fill(cx - a, cy - a, cx - a + th, cy - a + th, col);
        if (m.on("Cooldown")) {
            float cd = mc.player.getAttackCooldownProgress(0f);
            if (cd < 1f) {
                int y = cy + g + size + 4;
                ctx.fill(cx - 8, y, cx + 8, y + 2, 0x80000000);
                ctx.fill(cx - 8, y, cx - 8 + (int) (16 * cd), y + 2, col);
            }
        }
    }

    // ---------------- hit marker ----------------
    private static void hitMarker(DrawContext ctx, int sw, int sh, long now) {
        Module m = Modules.HITMARKER;
        if (!m.enabled) return;
        float age = (now - Hooks.lastHit) / 1.0e9f;
        if (age >= 0.3f) return;
        int a = (int) (255 * (1f - age / 0.3f));
        int col = (a << 24) | (m.getColor() & 0xFFFFFF);
        int cx = sw / 2, cy = sh / 2, gap = 3 + (int) (age * 20f);
        for (int i = 0; i < 5; i++) {
            int o = gap + i;
            ctx.fill(cx - o - 1, cy - o - 1, cx - o, cy - o, col);
            ctx.fill(cx + o, cy - o - 1, cx + o + 1, cy - o, col);
            ctx.fill(cx - o - 1, cy + o, cx - o, cy + o + 1, col);
            ctx.fill(cx + o, cy + o, cx + o + 1, cy + o + 1, col);
        }
    }

    // ---------------- target esp (glowing marker when you aim at something) ----------------
    private static void targetEsp(DrawContext ctx, MinecraftClient mc, int sw, int sh, float td, float dt) {
        Module m = Modules.TARGETESP;
        if (!m.enabled) { teAnim = 0f; return; }
        LivingEntity cur = null;
        if (m.on("Crosshair Only")) {
            if (mc.targetedEntity instanceof LivingEntity) cur = (LivingEntity) mc.targetedEntity;
        } else {
            cur = target(mc);
        }
        boolean motion = Access.motion();
        if (cur != null) { teEnt = cur; teAnim = motion ? step(teAnim, 1f, dt * 10f) : 1f; }
        else teAnim = motion ? step(teAnim, 0f, dt * 8f) : 0f;
        if (teAnim < 0.02f || teEnt == null || !teEnt.isAlive()) return;

        LivingEntity t = teEnt;
        Vec3d c = Esp.lerpPos(t, td).add(0, t.getHeight() / 2.0, 0);
        float[] s = Esp.project(c.x, c.y, c.z, sw, sh);
        if (s == null) return;
        double dist = Esp.cam.distanceTo(c);
        int sz = (int) (m.num("Size") * MathHelper.clamp(60.0 / Math.max(dist, 1.5), 10.0, 60.0) * ease(teAnim));
        if (sz < 2) return;
        float time = motion ? (System.currentTimeMillis() % 100000L) / 1000f * (float) m.num("Speed") : 0f;
        float pulse = 1f + (motion ? 0.08f * (float) Math.sin(time * 4f) : 0f);
        int rgb = m.getColor() & 0xFFFFFF;
        int arm = Math.max(4, sz / 2);
        int style = (int) m.num("Style");

        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(s[0], s[1], 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotation(time * 2f));
        ms.scale(pulse, pulse, 1f);
        if (style == 1) ctx.fill(-sz, -sz, sz, sz, 0x14000000 | rgb);
        for (int pass = 0; pass < 3; pass++) {
            int th = pass == 0 ? 5 : (pass == 1 ? 3 : 1);
            int al = pass == 0 ? 0x28 : (pass == 1 ? 0x60 : 0xFF);
            int col = (al << 24) | (pass == 2 ? 0xFFFFFF : rgb);
            if (style == 2) {
                for (int q = 0; q < 12; q++) {
                    double a = q * Math.PI / 6.0;
                    int x = (int) (Math.cos(a) * sz), y = (int) (Math.sin(a) * sz);
                    int d = th + 1;
                    ctx.fill(x - d / 2, y - d / 2, x - d / 2 + d, y - d / 2 + d, col);
                }
            } else if (style == 3) {
                ms.push();
                ms.multiply(RotationAxis.POSITIVE_Z.rotation((float) (Math.PI / 4.0)));
                int h = (int) (sz * 0.75);
                ctx.fill(-h, -h - th / 2, h, -h - th / 2 + th, col);
                ctx.fill(-h, h - th / 2, h, h - th / 2 + th, col);
                ctx.fill(-h - th / 2, -h, -h - th / 2 + th, h, col);
                ctx.fill(h - th / 2, -h, h - th / 2 + th, h, col);
                ms.pop();
            } else {
                for (int q = 0; q < 4; q++) {
                    ms.push();
                    ms.multiply(RotationAxis.POSITIVE_Z.rotation((float) (q * Math.PI / 2.0)));
                    ctx.fill(sz - arm, sz - th / 2, sz + th / 2 + 1, sz - th / 2 + th, col);
                    ctx.fill(sz - th / 2, sz - arm, sz - th / 2 + th, sz + th / 2 + 1, col);
                    ms.pop();
                }
            }
        }
        ms.pop();
    }
}

package com.prank;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModuleScreen extends Screen {
    public static int offX, offY;
    public static Module dragged;
    private static int tab = 0;
    private static final int[] SWATCH = {
        0xFF2EC4FF, 0xFFA78BFA, 0xFFFF4D6D, 0xFFFF9F43, 0xFFFFE066, 0xFF55FF55, 0xFF2EE6A6, 0xFF55FFFF,
        0xFF4DA3FF, 0xFFFFFFFF, 0xFFFF7BD5, 0xFF8B5A2B, 0xFF9AA0A6, 0xFFB6FF3B, 0};
    private static final int TAB_W = 37, TAB_GAP = 2;
    private static final Text TITLE = Text.literal("KELO CLIENT").setStyle(Style.EMPTY.withBold(true).withColor(0x2EC4FF));
    private static final Text TITLE_DARK = Text.literal("KELO CLIENT").setStyle(Style.EMPTY.withBold(true).withColor(0x0A4F9E));

    private int px, py, pw, ph;
    private Module sel;
    private boolean binding, dragPanel;
    private long lastFrame, openStart, burstT;
    private float popupT, tabAnim = -1f;
    private int dragDX, dragDY, hoverIdx = -1, burstIdx = -1;
    private final List<Module> cache = new ArrayList<>();
    private int cacheTab = -1;

    private int cBg, cCard, cBorder, cText, cSub, cChip, cAccent, cPopBorder, cTrackOff, cSwatchBorder;

    public ModuleScreen() { super(Text.literal("Kelo Client")); }

    private float scale() { return (float) Modules.LAYOUT.num("Scale"); }

    private void layout() {
        float S = scale();
        pw = (int) Math.min((width - 20) / S, 460);
        ph = (int) Math.min((height - 20) / S, 240);
        px = Math.max(0, Math.min(Math.max(0, width - pw), (width - pw) / 2 + offX));
        py = Math.max(0, Math.min(Math.max(0, height - ph), (height - ph) / 2 + offY));
    }

    private void theme() {
        cAccent = Modules.DARKMODE.color == 0 ? Module.rainbow(0f) : Modules.DARKMODE.color;
        if (Modules.DARKMODE.enabled) {
            cBg = 0xFF15151C; cCard = 0xFF1F1F29; cBorder = 0xFF2E2E3C; cText = 0xFFEDEDF5;
            cSub = 0xFF8C8CA3; cChip = 0xFF2A2A37; cTrackOff = 0xFF454556; cPopBorder = 0xFF3A3A4B;
            cSwatchBorder = 0xFF4A4A5C;
        } else {
            cBg = 0xFFF3F3F8; cCard = 0xFFFFFFFF; cBorder = 0xFFE2E2EC; cText = 0xFF1E1E2A;
            cSub = 0xFF8A8A9C; cChip = 0xFFF0F0F6; cTrackOff = 0xFFC9C9D6; cPopBorder = 0xFFD9D9E6;
            cSwatchBorder = 0xFFD0D0DC;
        }
        if (Access.contrast()) {
            cBg = 0xFF000000; cCard = 0xFF000000; cBorder = 0xFFFFFFFF; cText = 0xFFFFFFFF;
            cSub = 0xFFDDDDDD; cChip = 0xFF222222; cTrackOff = 0xFF666666; cPopBorder = 0xFFFFFFFF;
            cSwatchBorder = 0xFFFFFFFF;
        }
    }

    @Override protected void init() { layout(); openStart = System.nanoTime(); }
    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) { }
    @Override public void removed() { dragged = null; Config.save(); }

    // ---------- helpers ----------
    private static float ease(float t) { float u = 1f - t; return 1f - u * u * u; }

    private static float step(float cur, float target, float rate, boolean on) {
        if (!on) return target;
        float d = target - cur;
        if (Math.abs(d) < 0.01f) return target;
        return cur + d * Math.min(1f, rate);
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static float rnd(long s) {
        long x = s * 0x9E3779B97F4A7C15L;
        x ^= x >>> 29;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 32;
        return (x & 0xFFFFFFL) / (float) 0x1000000;
    }

    private void rr(DrawContext c, int x, int y, int w, int h, int r, int col) {
        c.fill(x, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, col);
        }
    }

    private boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private List<Module> mods() {
        if (cacheTab != tab) {
            cache.clear();
            for (Module m : Modules.ALL) if (m.category.equals(Modules.CATS[tab])) cache.add(m);
            cacheTab = tab;
        }
        return cache;
    }

    private int[] card(int i) {
        int cols = 3, gap = 6;
        int cw = (pw - 20 - gap * (cols - 1)) / cols;
        return new int[]{px + 10 + (i % cols) * (cw + gap), py + 54 + (i / cols) * 40, cw, 34};
    }

    private int tabX(int i) { return px + 14 + i * (TAB_W + TAB_GAP); }

    private int[] popup() {
        int n = sel.settings.size();
        int cols = n > 4 ? 2 : 1;
        int w = cols == 2 ? 420 : 250;
        int rows = (n + cols - 1) / cols;
        int h = 50 + (sel.hasColor ? 22 : 0) + rows * 20 + 8;
        return new int[]{px + (pw - w) / 2, py + 38, w, h, cols};
    }

    private String fmt(Module.Setting s) {
        return s.step >= 1 ? String.format("%.0f", s.value) : String.format("%.1f", s.value);
    }

    // ---------- electricity ----------
    private void electric(DrawContext c, long nowMs, List<Module> list) {
        if (!Modules.ELECTRICITY.enabled || !Access.motion()) return;
        float inten = (float) Modules.ELECTRICITY.num("Intensity");
        float spd = (float) Modules.ELECTRICITY.num("Speed");
        long bucket = (long) (nowMs / (90.0 / spd));
        int glowP = 0x552EC4FF, glowC = 0x558FE9FF, core = 0xFFE8F8FF;

        int tx = px + 10, ty = py + 18, tw = (int) (textRenderer.getWidth(TITLE) * 1.7f) + 4;
        Elec.bolt(c, bucket * 31L + 1, tx - 6, ty, tx + tw + 6, ty, 14, 5f, glowP, core);
        if (rnd(bucket) > 0.4f) Elec.bolt(c, bucket * 17L + 3, tx, ty - 9, tx + tw, ty + 9, 10, 4f, glowC, core);

        for (int i = 0; i < (int) inten; i++) {
            long s = bucket * 977L + i * 131L;
            int side = (int) (rnd(s) * 4);
            float p = rnd(s + 1), len = 30 + rnd(s + 2) * 40, j = (rnd(s + 3) - 0.5f) * 4;
            float x1, y1, x2, y2;
            if (side == 0 || side == 1) {
                x1 = px + 6 + p * (pw - 12 - len); x2 = x1 + len;
                y1 = (side == 0 ? py : py + ph); y2 = y1 + j;
            } else {
                y1 = py + 6 + p * (ph - 12 - len); y2 = y1 + len;
                x1 = (side == 2 ? px : px + pw); x2 = x1 + j;
            }
            Elec.bolt(c, s + 7, x1, y1, x2, y2, 6, 3f, i % 2 == 0 ? glowP : glowC, core);
        }

        if (hoverIdx >= 0 && hoverIdx < list.size()) {
            int[] r = card(hoverIdx);
            Elec.bolt(c, bucket * 13L + 5, r[0], r[1], r[0] + r[2], r[1], 8, 2.5f, glowP, core);
            Elec.bolt(c, bucket * 13L + 9, r[0], r[1] + r[3], r[0] + r[2], r[1] + r[3], 8, 2.5f, glowC, core);
        }

        if (burstIdx >= 0 && burstIdx < list.size() && nowMs - burstT < 350) {
            int[] r = card(burstIdx);
            float ccx = r[0] + r[2] / 2f, ccy = r[1] + r[3] / 2f;
            float rad = 10 + (nowMs - burstT) / 350f * 40f;
            for (int k = 0; k < 5; k++) {
                double a = k * 1.2566 + bucket * 0.3;
                Elec.bolt(c, bucket * 5L + k, ccx, ccy, ccx + (float) Math.cos(a) * rad,
                    ccy + (float) Math.sin(a) * rad, 5, 3f, glowP, core);
            }
        }
    }

    // ---------- render ----------
    @Override
    public void render(DrawContext c, int rawMx, int rawMy, float delta) {
        long nowN = System.nanoTime();
        long nowMs = nowN / 1000000L;
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (nowN - lastFrame) / 1.0e9f);
        lastFrame = nowN;
        boolean animOn = Modules.ANIMATIONS.enabled && Access.motion();
        float spd = (float) Modules.ANIMATIONS.num("Speed");

        layout();
        theme();
        float S = scale();
        float cx = px + pw / 2f, cy = py + ph / 2f;
        int mx = (int) (cx + (rawMx - cx) / S), my = (int) (cy + (rawMy - cy) / S);

        float open = animOn ? ease(Math.min(1f, (nowN - openStart) / 1.0e9f * spd * 5f)) : 1f;
        if (Modules.LAYOUT.enabled) c.fill(0, 0, width, height, ((int) (0x66 * open)) << 24);
        if (sel != null) popupT = animOn ? Math.min(1f, popupT + dt * spd * 8f) : 1f; else popupT = 0f;
        if (tabAnim < 0) tabAnim = tab;
        tabAnim = animOn ? tabAnim + (tab - tabAnim) * Math.min(1f, dt * 14f * spd) : tab;

        MatrixStack ms = c.getMatrices();
        ms.push();
        float k = S * (0.92f + 0.08f * open);
        ms.translate(cx, cy, 0);
        ms.scale(k, k, 1f);
        ms.translate(-cx, -cy, 0);

        rr(c, px + 2, py + 3, pw, ph, 8, 0x33000000);
        rr(c, px, py, pw, ph, 8, cBg);

        // title: pink, thick (bold + outline copies), pulsing glow behind
        float pulse = Access.motion() ? 0.5f + 0.5f * (float) Math.sin(nowMs / 350.0) : 0.5f;
        rr(c, px + 8, py + 5, 138, 22, 6, (((int) (0x14 + pulse * 0x14)) << 24) | 0x2EC4FF);
        ms.push();
        ms.translate(px + 12, py + 9, 0);
        ms.scale(1.7f, 1.7f, 1f);
        c.drawText(textRenderer, TITLE_DARK, 1, 1, 0xFFFFFFFF, false);
        c.drawText(textRenderer, TITLE_DARK, -1, 0, 0xFFFFFFFF, false);
        c.drawText(textRenderer, TITLE_DARK, 0, 1, 0xFFFFFFFF, false);
        c.drawText(textRenderer, TITLE, 0, 0, 0xFFFFFFFF, false);
        ms.pop();

        // tabs (sliding pill)
        rr(c, px + 14 + (int) (tabAnim * (TAB_W + TAB_GAP)), py + 30, TAB_W, 18, 9, mix(cAccent, cBg, 0.85f));
        for (int i = 0; i < Modules.CATS.length; i++) {
            String n = Modules.CATS[i];
            c.drawText(textRenderer, n, tabX(i) + (TAB_W - textRenderer.getWidth(n)) / 2, py + 35,
                i == tab ? cAccent : cSub, false);
        }

        // cards
        List<Module> list = mods();
        hoverIdx = -1;
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int[] r = card(i);
            int x = r[0], y = r[1], w = r[2], h = r[3];
            boolean hover = sel == null && dragged == null && in(mx, my, x, y, w, h);
            if (hover) hoverIdx = i;
            m.anim = step(m.anim, m.enabled ? 1f : 0f, dt * 14f * spd, animOn);
            m.hoverAnim = step(m.hoverAnim, hover ? 1f : 0f, dt * 14f * spd, animOn);

            rr(c, x, y, w, h, 6, mix(cBorder, cAccent, m.hoverAnim * 0.7f));
            rr(c, x + 1, y + 1, w - 2, h - 2, 5, cCard);
            c.drawText(textRenderer, m.name, x + 8, y + 7, cText, false);

            ms.push();
            ms.translate(x + 8, y + 20, 0);
            ms.scale(0.75f, 0.75f, 1f);
            c.drawText(textRenderer, m.desc, 0, 0, cSub, false);
            ms.pop();

            if (m.key > 0) {
                String kn = m.keyLabel();
                int kw = (int) (textRenderer.getWidth(kn) * 0.75f);
                ms.push();
                ms.translate(x + w - 20 - kw, y + 20, 0);
                ms.scale(0.75f, 0.75f, 1f);
                c.drawText(textRenderer, kn, 0, 0, cAccent, false);
                ms.pop();
            }
            if (m.hasColor) {
                if (m.color == 0) {
                    for (int q = 0; q < 6; q++) c.fill(x + w - 14 + q, y + 21, x + w - 13 + q, y + 27, Module.hsv(q / 6f));
                } else {
                    c.fill(x + w - 14, y + 21, x + w - 8, y + 27, m.color);
                }
            }
            if (m.movable) c.drawText(textRenderer, "+", x + w - 8 - 8, y + 6 - 6, cSub, false);

            int tx = x + w - 28, ty = y + 7;
            rr(c, tx, ty, 20, 10, 5, mix(cTrackOff, cAccent, m.anim));
            rr(c, tx + 1 + (int) (10 * m.anim), ty + 1, 8, 8, 4, 0xFFFFFFFF);
        }

        electric(c, nowMs, list);

        String hint = "Left click: toggle   Right click: settings   Drag the title or any HUD element to move it";
        ms.push();
        ms.translate(px + (pw - textRenderer.getWidth(hint) * 0.75f) / 2f, py + ph - 12, 0);
        ms.scale(0.75f, 0.75f, 1f);
        c.drawText(textRenderer, hint, 0, 0, cSub, false);
        ms.pop();

        if (sel != null) drawPopup(c, ms);
        ms.pop();
    }

    private void drawPopup(DrawContext c, MatrixStack ms) {
        int[] pr = popup();
        int ux = pr[0], uy = pr[1], uw = pr[2], uh = pr[3], cols = pr[4];
        float pk = 0.9f + 0.1f * ease(popupT);
        ms.push();
        ms.translate(ux + uw / 2f, uy + uh / 2f, 0);
        ms.scale(pk, pk, 1f);
        ms.translate(-(ux + uw / 2f), -(uy + uh / 2f), 0);

        rr(c, ux + 2, uy + 3, uw, uh, 8, 0x55000000);
        rr(c, ux, uy, uw, uh, 8, cPopBorder);
        rr(c, ux + 1, uy + 1, uw - 2, uh - 2, 7, cCard);
        c.drawText(textRenderer, sel.name, ux + 10, uy + 9, cText, false);
        c.drawText(textRenderer, "x", ux + uw - 14, uy + 8, cSub, false);
        if (sel.movable) {
            rr(c, ux + uw - 70, uy + 5, 46, 14, 4, cChip);
            c.drawText(textRenderer, "Reset", ux + uw - 62, uy + 8, cText, false);
        }

        rr(c, ux + 10, uy + 26, 110, 18, 5, binding ? mix(cAccent, cCard, 0.8f) : cChip);
        String kt = binding ? "Press a key..." : "Key: " + sel.keyLabel();
        c.drawText(textRenderer, kt, ux + 16, uy + 31, binding ? cAccent : cText, false);
        ms.push();
        ms.translate(ux + 126, uy + 32, 0);
        ms.scale(0.7f, 0.7f, 1f);
        c.drawText(textRenderer, "Backspace = clear", 0, 0, cSub, false);
        ms.pop();

        int y = uy + 50;
        if (sel.hasColor) {
            for (int i = 0; i < SWATCH.length; i++) {
                int sx = ux + 10 + i * 15;
                c.fill(sx - 1, y - 1, sx + 14, y + 13, cSwatchBorder);
                if (SWATCH[i] == 0) {
                    for (int q = 0; q < 13; q++) c.fill(sx + q, y, sx + q + 1, y + 12, Module.hsv(q / 13f));
                } else {
                    c.fill(sx, y, sx + 13, y + 12, SWATCH[i]);
                }
                if (SWATCH[i] == sel.color) {
                    c.fill(sx - 2, y - 2, sx + 15, y - 1, cAccent);
                    c.fill(sx - 2, y + 13, sx + 15, y + 14, cAccent);
                    c.fill(sx - 2, y - 1, sx - 1, y + 13, cAccent);
                    c.fill(sx + 14, y - 1, sx + 15, y + 13, cAccent);
                }
            }
            y += 22;
        }
        int colW = cols == 2 ? 195 : 230;
        for (int i = 0; i < sel.settings.size(); i++) {
            Module.Setting s = sel.settings.get(i);
            int sx = ux + 10 + (i % cols) * 205, sy = y + (i / cols) * 20;
            if (s.isBool) {
                c.drawText(textRenderer, s.name, sx, sy + 4, cText, false);
                boolean v = s.value > 0.5;
                rr(c, sx + colW - 24, sy + 3, 20, 10, 5, v ? cAccent : cTrackOff);
                rr(c, v ? sx + colW - 13 : sx + colW - 23, sy + 4, 8, 8, 4, 0xFFFFFFFF);
            } else {
                c.drawText(textRenderer, s.name + ": " + fmt(s), sx, sy + 4, cText, false);
                rr(c, sx + colW - 54, sy, 22, 16, 5, cChip);
                rr(c, sx + colW - 26, sy, 22, 16, 5, cChip);
                c.drawText(textRenderer, "-", sx + colW - 45, sy + 4, cText, false);
                c.drawText(textRenderer, "+", sx + colW - 17, sy + 4, cText, false);
            }
        }
        ms.pop();
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(double rawMx, double rawMy, int button) {
        layout();
        float S = scale();
        float cx = px + pw / 2f, cy = py + ph / 2f;
        double mx = cx + (rawMx - cx) / S, my = cy + (rawMy - cy) / S;

        if (sel != null) {
            int[] pr = popup();
            int ux = pr[0], uy = pr[1], uw = pr[2], uh = pr[3], cols = pr[4];
            if (!in(mx, my, ux, uy, uw, uh)) { sel = null; binding = false; return true; }
            if (button != 0) return true;
            if (in(mx, my, ux + uw - 18, uy + 6, 12, 12)) { sel = null; binding = false; return true; }
            if (sel.movable && in(mx, my, ux + uw - 70, uy + 5, 46, 14)) { sel.hx = Integer.MIN_VALUE; return true; }
            if (in(mx, my, ux + 10, uy + 26, 110, 18)) { binding = !binding; return true; }
            int y = uy + 50;
            if (sel.hasColor) {
                for (int i = 0; i < SWATCH.length; i++) {
                    if (in(mx, my, ux + 10 + i * 15, y, 13, 12)) { sel.color = SWATCH[i]; return true; }
                }
                y += 22;
            }
            int colW = cols == 2 ? 195 : 230;
            for (int i = 0; i < sel.settings.size(); i++) {
                Module.Setting s = sel.settings.get(i);
                int sx = ux + 10 + (i % cols) * 205, sy = y + (i / cols) * 20;
                if (s.isBool) {
                    if (in(mx, my, sx + colW - 24, sy + 3, 20, 10)) s.value = s.value > 0.5 ? 0 : 1;
                } else {
                    if (in(mx, my, sx + colW - 54, sy, 22, 16))
                        s.value = Math.max(s.min, Math.round((s.value - s.step) * 100) / 100.0);
                    if (in(mx, my, sx + colW - 26, sy, 22, 16))
                        s.value = Math.min(s.max, Math.round((s.value + s.step) * 100) / 100.0);
                }
            }
            return true;
        }

        boolean inPanel = in(mx, my, px, py, pw, ph);
        if (inPanel) {
            if (button == 0 && in(mx, my, px, py, pw, 28)) { dragPanel = true; return true; }
            for (int i = 0; i < Modules.CATS.length; i++) {
                if (button == 0 && in(mx, my, tabX(i), py + 30, TAB_W, 18)) { tab = i; return true; }
            }
            List<Module> list = mods();
            for (int i = 0; i < list.size(); i++) {
                int[] r = card(i);
                if (in(mx, my, r[0], r[1], r[2], r[3])) {
                    if (button == 0) {
                        list.get(i).toggle();
                        burstIdx = i;
                        burstT = System.nanoTime() / 1000000L;
                    } else if (button == 1) {
                        sel = list.get(i);
                        binding = false;
                    }
                    return true;
                }
            }
            return true;
        }

        // outside the panel: grab a HUD element
        if (button == 0) {
            for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
                Module m = Modules.ALL.get(i);
                if (m.movable && m.enabled && in(rawMx, rawMy, m.dx, m.dy, m.dw, m.dh)) {
                    dragged = m;
                    dragDX = (int) rawMx - m.dx;
                    dragDY = (int) rawMy - m.dy;
                    return true;
                }
            }
        }
        return super.mouseClicked(rawMx, rawMy, button);
    }

    @Override
    public boolean mouseDragged(double rawMx, double rawMy, int button, double dX, double dY) {
        if (dragged != null) {
            dragged.hx = (int) rawMx - dragDX;
            dragged.hy = (int) rawMy - dragDY;
            return true;
        }
        if (dragPanel) {
            offX += (int) dX;
            offY += (int) dY;
            return true;
        }
        return super.mouseDragged(rawMx, rawMy, button, dX, dY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragged != null || dragPanel) {
            dragged = null;
            dragPanel = false;
            Config.save();
            return true;
        }
        Config.save();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (binding && sel != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) binding = false;
            else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) { sel.key = -1; binding = false; }
            else if (key != GLFW.GLFW_KEY_F9) { sel.key = key; binding = false; }
            Config.save();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && sel != null) { sel = null; return true; }
        if (key == GLFW.GLFW_KEY_F9) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}

package com.prank;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

/** Positions movable HUD elements. Draw in local coordinates between begin() and end(). */
public class Hud {
    public static void begin(DrawContext ctx, Module m, float sc, int defX, int defY, int w, int h, int sw, int sh) {
        sc = sc * Access.hudScale();
        int bw = Math.max(1, (int) (w * sc)), bh = Math.max(1, (int) (h * sc));
        int x = m.hx == Integer.MIN_VALUE ? defX : m.hx;
        int y = m.hx == Integer.MIN_VALUE ? defY : m.hy;
        x = Math.max(0, Math.min(Math.max(0, sw - bw), x));
        y = Math.max(0, Math.min(Math.max(0, sh - bh), y));
        m.dx = x; m.dy = y; m.dw = bw; m.dh = bh;
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(x, y, 0);
        ms.scale(sc, sc, 1f);
    }

    public static void end(DrawContext ctx, Module m, boolean editing, boolean dragging) {
        ctx.getMatrices().pop();
        if (editing) {
            int col = dragging ? 0xFFFFFFFF : (m.hasColor ? (m.getColor() | 0xFF000000) : 0xFFFF7BD5);
            ctx.fill(m.dx - 1, m.dy - 1, m.dx + m.dw + 1, m.dy, col);
            ctx.fill(m.dx - 1, m.dy + m.dh, m.dx + m.dw + 1, m.dy + m.dh + 1, col);
            ctx.fill(m.dx - 1, m.dy, m.dx, m.dy + m.dh, col);
            ctx.fill(m.dx + m.dw, m.dy, m.dx + m.dw + 1, m.dy + m.dh, col);
        }
    }
}

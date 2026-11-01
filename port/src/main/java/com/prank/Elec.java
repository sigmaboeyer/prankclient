package com.prank;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import java.util.Random;

/** Little lightning bolts for the menu. */
public class Elec {
    private static final Random R = new Random();

    private static void seg(DrawContext c, float x1, float y1, float x2, float y2, int t, int col) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) return;
        MatrixStack ms = c.getMatrices();
        ms.push();
        ms.translate(x1, y1, 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(dy, dx)));
        int top = -(t / 2);
        c.fill(0, top, (int) Math.ceil(len) + 1, top + t, col);
        ms.pop();
    }

    /** A jagged bolt from (x1,y1) to (x2,y2). The same seed gives the same bolt. */
    public static void bolt(DrawContext c, long seed, float x1, float y1, float x2, float y2,
                            int segs, float jitter, int glow, int core) {
        R.setSeed(seed);
        float dx = (x2 - x1) / segs, dy = (y2 - y1) / segs;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        float nx = -dy / len, ny = dx / len;
        float px = x1, py = y1;
        for (int i = 1; i <= segs; i++) {
            float tx = x1 + dx * i, ty = y1 + dy * i;
            if (i < segs) {
                float o = (R.nextFloat() - 0.5f) * 2f * jitter;
                tx += nx * o;
                ty += ny * o;
            } else {
                tx = x2;
                ty = y2;
            }
            seg(c, px, py, tx, ty, 3, glow);
            seg(c, px, py, tx, ty, 1, core);
            px = tx;
            py = ty;
        }
    }
}

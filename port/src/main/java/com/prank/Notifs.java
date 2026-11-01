package com.prank;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Pop-ups: bouncy slide-in, smooth stacking, countdown bar, colored dot. */
public class Notifs {
    private static class N {
        final String text;
        final boolean good;
        final long born = System.currentTimeMillis();
        float y = -1000f;

        N(String text, boolean good) { this.text = text; this.good = good; }
    }

    private static final List<N> LIST = new ArrayList<>();

    public static void push(String text, boolean good) {
        if (!Modules.NOTIFICATIONS.enabled) return;
        if (!LIST.isEmpty()) {
            N last = LIST.get(LIST.size() - 1);
            if (last.text.equals(text) && System.currentTimeMillis() - last.born < 300) return;
        }
        LIST.add(new N(text, good));
        while (LIST.size() > 5) LIST.remove(0);
    }

    private static float backOut(float t) {
        float u = t - 1f;
        return 1f + 2.70158f * u * u * u + 1.70158f * u * u;
    }

    public static void draw(DrawContext ctx, TextRenderer tr, int sw) {
        long now = System.currentTimeMillis();
        long life = (long) (Modules.NOTIFICATIONS.num("Duration") * 1000);
        LIST.removeIf(n -> now - n.born > life);
        boolean motion = Access.motion();
        boolean bounce = motion && Modules.NOTIFICATIONS.on("Bounce");
        boolean bar = Modules.NOTIFICATIONS.on("Progress Bar");
        int target = 22;
        for (N n : LIST) {
            long age = now - n.born;
            float in = motion ? Math.min(1f, age / 320f) : 1f;
            float e = bounce ? backOut(in) : 1f - (1f - in) * (1f - in) * (1f - in);
            float out = age > life - 350 ? (age - (life - 350)) / 350f : 0f;
            float alpha = Math.min(1f, in * 2f) * (1f - out);
            int a = (int) (255 * alpha);
            if (a < 12) continue;
            if (n.y < -999f) n.y = target;
            n.y += (target - n.y) * (motion ? 0.25f : 1f);

            int w = tr.getWidth(n.text) + 28;
            int x = (sw - w) / 2;
            int yy = (int) (n.y + (1f - e) * -26f - out * 18f);
            int col = n.good ? 0x55FF55 : 0xFF5555;
            ctx.fill(x, yy, x + w, yy + 16, (((int) (a * 0.75f)) << 24) | 0x101018);
            ctx.fill(x, yy, x + 2, yy + 16, (a << 24) | col);
            ctx.fill(x + 7, yy + 5, x + 13, yy + 11, (a << 24) | col);
            ctx.drawText(tr, n.text, x + 18, yy + 4, (a << 24) | 0xFFFFFF, false);
            if (bar) {
                int bw = (int) (w * Math.max(0f, 1f - (float) age / life));
                ctx.fill(x, yy + 15, x + bw, yy + 16, (a << 24) | col);
            }
            target += 19;
        }
    }
}

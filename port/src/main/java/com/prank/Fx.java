package com.prank;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Fx {
    private static final Set<Long> HIT_PAIRS = new HashSet<>();
    private static final List<FallingBlockEntity> FBS = new ArrayList<>();
    private static final Set<Integer> ACTIVE = new HashSet<>();

    /** 1..11 = vanilla particles, 12 = colored dust (uses the module color). */
    public static ParticleEffect type(int i, Module m) {
        switch (i) {
            case 1: return ParticleTypes.END_ROD;
            case 2: return ParticleTypes.FLAME;
            case 3: return ParticleTypes.HEART;
            case 4: return ParticleTypes.CRIT;
            case 5: return ParticleTypes.ENCHANTED_HIT;
            case 6: return ParticleTypes.SOUL_FIRE_FLAME;
            case 7: return ParticleTypes.ELECTRIC_SPARK;
            case 8: return ParticleTypes.GLOW;
            case 9: return ParticleTypes.SNOWFLAKE;
            case 10: return ParticleTypes.NOTE;
            case 11: return ParticleTypes.CLOUD;
            default: {
                int c = m.getColor();
                return new DustParticleEffect(new Vector3f(((c >> 16) & 255) / 255f,
                    ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1.2f);
            }
        }
    }

    public static void burst(Entity t, Module m, int typeIdx, int count) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        Box b = t.getBoundingBox();
        ParticleEffect pt = type(typeIdx, m);
        for (int i = 0; i < count; i++) {
            double x = b.minX + mc.world.random.nextDouble() * (b.maxX - b.minX);
            double y = b.minY + mc.world.random.nextDouble() * (b.maxY - b.minY);
            double z = b.minZ + mc.world.random.nextDouble() * (b.maxZ - b.minZ);
            mc.world.addParticle(pt, x, y, z,
                (mc.world.random.nextDouble() - 0.5) * 0.3,
                mc.world.random.nextDouble() * 0.25,
                (mc.world.random.nextDouble() - 0.5) * 0.3);
        }
    }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) { HIT_PAIRS.clear(); return; }

        if (Modules.TRAIL.enabled && !Freecam.active && mc.player.getVelocity().horizontalLengthSquared() > 0.0004) {
            ParticleEffect pt = type((int) Modules.TRAIL.num("Type"), Modules.TRAIL);
            for (int i = 0; i < (int) Modules.TRAIL.num("Amount"); i++) {
                mc.world.addParticle(pt, mc.player.getX() + (mc.world.random.nextDouble() - 0.5) * 0.3,
                    mc.player.getY() + 0.1, mc.player.getZ() + (mc.world.random.nextDouble() - 0.5) * 0.3,
                    0, 0.02, 0);
            }
        }

        aura(mc);
        trackDamage(mc);
        tickDrip(mc);
        tickKill(mc);
    }

    private static final java.util.Map<Integer, Float> HP = new java.util.HashMap<>();
    private static final List<double[]> POPS = new ArrayList<>();

    private static void aura(MinecraftClient mc) {
        Module m = Modules.PARTICLEAURA;
        if (!m.enabled || Freecam.active) return;
        ParticleEffect pt = type((int) m.num("Type"), m);
        int n = (int) m.num("Amount");
        double r = m.num("Radius"), t = mc.player.age * 0.2 * m.num("Speed");
        for (int i = 0; i < n; i++) {
            double a = t + i * Math.PI * 2 / n;
            mc.world.addParticle(pt, mc.player.getX() + Math.cos(a) * r, mc.player.getY() + 0.2 + (i % 3) * 0.6,
                mc.player.getZ() + Math.sin(a) * r, 0, 0, 0);
        }
    }

    private static void trackDamage(MinecraftClient mc) {
        if (!Modules.DAMAGEPOPUPS.enabled) { HP.clear(); POPS.clear(); return; }
        long now = System.currentTimeMillis();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity) || e == mc.player || mc.player.squaredDistanceTo(e) > 1600) continue;
            LivingEntity le = (LivingEntity) e;
            float hp = le.getHealth();
            Float prev = HP.put(le.getId(), hp);
            if (prev != null && hp < prev - 0.01f)
                POPS.add(new double[]{le.getX(), le.getY() + le.getHeight() + 0.2, le.getZ(), prev - hp, now});
        }
        if (mc.player.age % 200 == 0) HP.clear();
        POPS.removeIf(p -> now - (long) p[4] > 4000);
    }

    public static void popups(DrawContext ctx, TextRenderer tr, int sw, int sh) {
        Module m = Modules.DAMAGEPOPUPS;
        if (!m.enabled || POPS.isEmpty()) return;
        long now = System.currentTimeMillis();
        double life = m.num("Duration") * 1000.0;
        float sc = (float) m.num("Scale") * Access.hudScale();
        for (double[] p : POPS) {
            double age = now - p[4];
            if (age > life) continue;
            float f = (float) (age / life);
            float[] s = Esp.project(p[0], p[1] + f * 1.2, p[2], sw, sh);
            if (s == null) continue;
            String t = "-" + (Math.round(p[3] * 10) / 10.0);
            int a = (int) (255 * (1f - f * f));
            if (a < 12) continue;
            MatrixStack ms = ctx.getMatrices();
            ms.push();
            ms.translate(s[0], s[1], 0);
            ms.scale(sc, sc, 1f);
            ctx.drawText(tr, t, -tr.getWidth(t) / 2, 0, (a << 24) | (m.getColor() & 0xFFFFFF), true);
            ms.pop();
        }
    }

    private static void tickKill(MinecraftClient mc) {
        LivingEntity t = Hooks.lastTarget;
        if (t == null) return;
        if (System.currentTimeMillis() - Hooks.lastAttackMs > 3000) { Hooks.lastTarget = null; return; }
        if (t.isDead() || t.getHealth() <= 0f) {
            Hooks.lastTarget = null;
            Hooks.kills++;
            kill(mc, t);
        }
    }

    private static void kill(MinecraftClient mc, LivingEntity t) {
        Vec3d c = t.getBoundingBox().getCenter();
        if (Modules.KILLSOUND.enabled)
            Sounds.playAt("kill", c.x, c.y, c.z, (float) Modules.KILLSOUND.num("Volume"),
                (float) Modules.KILLSOUND.num("Pitch"));
        if (Modules.KILLEFFECT.enabled) {
            int n = (int) Modules.KILLEFFECT.num("Count");
            int type = (int) Modules.KILLEFFECT.num("Type");
            for (int i = 0; i < n; i++) {
                double a = mc.world.random.nextDouble() * Math.PI * 2;
                double sp = 0.1 + mc.world.random.nextDouble() * 0.25;
                double r = mc.world.random.nextDouble();
                switch (type) {
                    case 2:
                        mc.world.addParticle(ParticleTypes.SOUL, c.x, c.y - 0.5, c.z,
                            Math.cos(a) * 0.03, 0.1 + sp * 0.3, Math.sin(a) * 0.03);
                        break;
                    case 3:
                        mc.world.addParticle(ParticleTypes.FLAME, c.x + Math.cos(a), c.y - 0.5, c.z + Math.sin(a),
                            0, 0.1, 0);
                        break;
                    case 4:
                        mc.world.addParticle(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y, c.z,
                            Math.cos(a) * sp, sp * 0.8, Math.sin(a) * sp);
                        break;
                    case 5:
                        mc.world.addParticle(ParticleTypes.HEART, c.x + (r - 0.5), c.y + r, c.z + (r - 0.5) * 0.5,
                            0, 0.05, 0);
                        break;
                    default:
                        mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z,
                            Math.cos(a) * sp * 2, (r - 0.3) * sp * 2, Math.sin(a) * sp * 2);
                        break;
                }
            }
            mc.world.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0, 0, 0);
        }
        if (Modules.AUTOGG.enabled && mc.player != null && mc.player.networkHandler != null) {
            String[] g = {"gg", "ez", "good fight", "gf"};
            mc.player.networkHandler.sendChatMessage(g[Math.max(0, Math.min(3, (int) Modules.AUTOGG.num("Style") - 1))]);
        }
        if (Modules.KILLEFFECT.on("Message"))
            Notifs.push("Killed " + t.getName().getString() + "  (#" + Hooks.kills + ")", true);
    }

    /** Plays a custom sound when a falling pointed dripstone touches a living entity. */
    private static void tickDrip(MinecraftClient mc) {
        if (!Modules.DRIPIMPACT.enabled) { HIT_PAIRS.clear(); return; }
        FBS.clear();
        ACTIVE.clear();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof FallingBlockEntity) {
                FallingBlockEntity fb = (FallingBlockEntity) e;
                if (fb.getBlockState().isOf(Blocks.POINTED_DRIPSTONE)) {
                    FBS.add(fb);
                    ACTIVE.add(fb.getId());
                }
            }
        }
        if (FBS.isEmpty()) { HIT_PAIRS.clear(); return; }
        HIT_PAIRS.removeIf(k -> !ACTIVE.contains((int) (k >> 32)));
        for (FallingBlockEntity fb : FBS) {
            Box b = fb.getBoundingBox().expand(0.2, 0.35, 0.2);
            for (Entity t : mc.world.getOtherEntities(fb, b)) {
                if (!(t instanceof LivingEntity)) continue;
                long key = ((long) fb.getId() << 32) | (t.getId() & 0xFFFFFFFFL);
                if (!HIT_PAIRS.add(key)) continue;
                Sounds.playAt("impact", t.getX(), t.getBodyY(0.5), t.getZ(),
                    (float) Modules.DRIPIMPACT.num("Volume"), (float) Modules.DRIPIMPACT.num("Pitch"));
                Vec3d c = t.getBoundingBox().getCenter();
                for (int i = 0; i < 10; i++) {
                    mc.world.addParticle(ParticleTypes.CRIT, c.x, c.y, c.z,
                        (mc.world.random.nextDouble() - 0.5) * 0.5,
                        mc.world.random.nextDouble() * 0.4,
                        (mc.world.random.nextDouble() - 0.5) * 0.5);
                }
                Notifs.push("Dripstone hit " + t.getName().getString(), true);
            }
        }
    }
}

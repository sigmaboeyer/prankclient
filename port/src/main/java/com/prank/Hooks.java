package com.prank;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Hooks {
    private static final java.util.Map<Integer, Integer> POPS = new java.util.HashMap<>();

    public static void onStatus(net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket packet) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Modules.POPALERT.enabled || mc.world == null || packet.getStatus() != 35) return;
        Entity e = packet.getEntity(mc.world);
        if (!(e instanceof PlayerEntity)) return;
        int n = POPS.merge(e.getId(), 1, Integer::sum);
        Notifs.push(e.getName().getString() + " popped a totem (" + n + ")", e != mc.player);
        Sounds.play("hit", 1f, 0.6f);
    }

    public static long lastHit, lastAttackMs;
    public static LivingEntity lastTarget;
    public static int kills;

    /** Called whenever the local player attacks something. */
    public static void onAttack(PlayerEntity player, Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || player != mc.player) return;

        if (Modules.CRITICALS.enabled && mc.player.isOnGround() && !mc.player.isTouchingWater()
            && !mc.player.isClimbing() && mc.player.networkHandler != null) {
            double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
        }

        if (target instanceof LivingEntity) {
            lastTarget = (LivingEntity) target;
            lastAttackMs = System.currentTimeMillis();
        }
        lastHit = System.nanoTime();
        if (Modules.HITPARTICLES.enabled)
            Fx.burst(target, Modules.HITPARTICLES, (int) Modules.HITPARTICLES.num("Type"),
                (int) Modules.HITPARTICLES.num("Count"));
    }
}

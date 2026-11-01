package com.prank;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Predicts where a pearl / snowball / egg / bow shot will land. */
public class Trajectory {
    public static void draw(MinecraftClient mc, Esp.Batch L, float td) {
        Module m = Modules.TRAJECTORIES;
        if (!m.enabled || mc.player == null || mc.world == null) return;
        ClientPlayerEntity p = mc.player;
        Item it = p.getMainHandStack().getItem();
        double speed, grav;
        if (it == Items.ENDER_PEARL || it == Items.SNOWBALL || it == Items.EGG) {
            speed = 1.5; grav = 0.03;
        } else if (it == Items.EXPERIENCE_BOTTLE) {
            speed = 0.7; grav = 0.07;
        } else if (it == Items.SPLASH_POTION || it == Items.LINGERING_POTION) {
            speed = 0.5; grav = 0.05;
        } else if (it == Items.BOW && p.isUsingItem()) {
            float pull = BowItem.getPullProgress(p.getItemUseTime());
            if (pull < 0.1f) return;
            speed = pull * 3.0; grav = 0.05;
        } else {
            return;
        }

        float yaw = MathHelper.lerp(td, p.prevYaw, p.getYaw());
        float pitch = MathHelper.lerp(td, p.prevPitch, p.getPitch());
        Vec3d pos = p.getEyePos().subtract(0, 0.1, 0);
        Vec3d vel = Vec3d.fromPolar(pitch, yaw).multiply(speed);
        int col = m.getColor();

        for (int i = 0; i < 120; i++) {
            Vec3d next = pos.add(vel);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(pos, next,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3d h = hit.getPos();
                L.line(pos, h, col);
                L.box(new Box(h.x - 0.25, h.y - 0.25, h.z - 0.25, h.x + 0.25, h.y + 0.25, h.z + 0.25), col);
                return;
            }
            L.line(pos, next, col);
            pos = next;
            vel = vel.multiply(0.99).subtract(0, grav, 0);
            if (pos.y < mc.world.getBottomY() - 10) return;
        }
    }
}

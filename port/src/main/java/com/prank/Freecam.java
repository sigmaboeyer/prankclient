package com.prank;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class Freecam {
    public static boolean active;
    public static double x, y, z;
    public static float yaw, pitch;
    public static BlockHitResult target;
    public static boolean allowed;
    private static long last;
    private static boolean useDown, breaking;

    /** Called every tick: start/stop, targeting, and block interaction. */
    public static void tick(MinecraftClient mc) {
        boolean want = Modules.FREECAM.enabled && mc.player != null && mc.world != null;
        if (!want) {
            if (active && breaking && mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
            active = false; target = null; last = 0; breaking = false; useDown = false;
            return;
        }
        if (!active) {
            Vec3d c = mc.gameRenderer.getCamera().getPos();
            x = c.x; y = c.y; z = c.z;
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
            active = true;
        }

        Vec3d start = new Vec3d(x, y, z);
        Vec3d end = start.add(Vec3d.fromPolar(pitch, yaw).multiply(6));
        BlockHitResult hit = mc.world.raycast(new RaycastContext(start, end,
            RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            target = hit;
            allowed = mc.player.getEyePos().distanceTo(hit.getPos()) <= Modules.FREECAM.num("Reach");
        } else {
            target = null;
            allowed = false;
        }

        boolean atk = mc.options.attackKey.isPressed();
        boolean use = mc.options.useKey.isPressed();
        boolean canAct = mc.currentScreen == null && target != null && allowed;

        if (canAct && atk) {
            mc.interactionManager.updateBlockBreakingProgress(target.getBlockPos(), target.getSide());
            mc.player.swingHand(Hand.MAIN_HAND);
            breaking = true;
        } else if (breaking) {
            mc.interactionManager.cancelBlockBreaking();
            breaking = false;
        }

        if (canAct && use && !useDown) {
            ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, target);
            if (res.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);
        }
        useDown = use;
    }

    /** Mouse movement while in freecam (player body stays still). */
    public static void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
    }

    /** Called every frame (from the camera) so movement is smooth. */
    public static void frame() {
        long now = System.nanoTime();
        double dt = last == 0 ? 0.016 : Math.min(0.1, (now - last) / 1.0e9);
        last = now;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) return;
        Vec3d look = Vec3d.fromPolar(pitch, yaw);
        Vec3d right = Vec3d.fromPolar(0, yaw + 90);
        Vec3d v = Vec3d.ZERO;
        if (mc.options.forwardKey.isPressed()) v = v.add(look);
        if (mc.options.backKey.isPressed()) v = v.subtract(look);
        if (mc.options.rightKey.isPressed()) v = v.add(right);
        if (mc.options.leftKey.isPressed()) v = v.subtract(right);
        if (mc.options.jumpKey.isPressed()) v = v.add(0, 1, 0);
        if (mc.options.sneakKey.isPressed()) v = v.add(0, -1, 0);
        if (v.lengthSquared() == 0) return;
        double s = Modules.FREECAM.num("Speed") * (mc.options.sprintKey.isPressed() ? 2.0 : 1.0) * dt;
        v = v.normalize().multiply(s);
        x += v.x; y += v.y; z += v.z;
    }
}

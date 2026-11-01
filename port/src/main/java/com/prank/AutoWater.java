package com.prank;

import net.minecraft.block.Block;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Right-click a lever/button holding anything: water from your hotbar bucket is placed so it washes
 * the lever away, the water is picked back up, and if you held a block (or cobweb) it is placed
 * in the freed spot. Sneak to click normally.
 */
public class AutoWater {
    private static class Job {
        BlockPos pos;
        int bucket, prev, blockSlot, timer, stage, wait;
    }

    private static Job job;

    private static boolean isTrigger(Block b) { return b instanceof LeverBlock || b instanceof ButtonBlock; }

    private static int findWater(ClientPlayerEntity p) {
        for (int i = 0; i < 9; i++) if (p.getInventory().getStack(i).isOf(Items.WATER_BUCKET)) return i;
        return -1;
    }

    private static void shift(ClientPlayerEntity p, boolean down) {
        p.networkHandler.sendPacket(new ClientCommandC2SPacket(p, down
            ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY
            : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
    }

    private static void useBucket(MinecraftClient mc, ClientPlayerEntity p, int slot) {
        p.getInventory().selectedSlot = slot;
        ActionResult r = mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
        if (r.shouldSwingHand()) p.swingHand(Hand.MAIN_HAND);
    }

    /** Called on every right-click. Returns true if we handled it (normal click is cancelled). */
    public static boolean intercept(MinecraftClient mc) {
        if (!Modules.AUTOWATER.enabled || mc.player == null || mc.world == null
            || mc.currentScreen != null || Freecam.active) return false;
        ClientPlayerEntity p = mc.player;
        if (p.isSneaking()) return false;
        if (job != null) return true;
        if (!(mc.crosshairTarget instanceof BlockHitResult)) return false;
        BlockHitResult bh = (BlockHitResult) mc.crosshairTarget;
        if (bh.getType() != HitResult.Type.BLOCK) return false;
        if (!isTrigger(mc.world.getBlockState(bh.getBlockPos()).getBlock())) return false;
        int bucket = findWater(p);
        if (bucket < 0) return false;

        Job j = new Job();
        j.pos = bh.getBlockPos();
        j.bucket = bucket;
        j.prev = p.getInventory().selectedSlot;
        j.blockSlot = Modules.AUTOWATER.on("Place Block After")
            && p.getMainHandStack().getItem() instanceof BlockItem ? j.prev : -1;
        j.stage = 1;
        job = j;
        useBucket(mc, p, bucket);        // water goes next to the lever and washes it away
        return true;
    }

    public static void tick(MinecraftClient mc) {
        Job j = job;
        if (j == null) return;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) { job = null; return; }
        j.timer++;

        if (j.stage == 1) {                                  // wait until the lever is gone
            if (!isTrigger(mc.world.getBlockState(j.pos).getBlock()) || j.timer > 40) {
                j.stage = 2;
                j.wait = (int) Modules.AUTOWATER.num("Delay");
            }
            return;
        }
        if (j.wait > 0) { j.wait--; return; }

        if (j.stage == 2) {                                  // pick the water back up
            if (Modules.AUTOWATER.on("Pick Up Water")) useBucket(mc, p, j.bucket);
            j.stage = 3;
            j.wait = 3;
            return;
        }

        if (j.blockSlot >= 0 && mc.world.getBlockState(j.pos).isReplaceable()) placeBlock(mc, p, j);
        p.getInventory().selectedSlot = j.prev;
        job = null;
    }

    private static void placeBlock(MinecraftClient mc, ClientPlayerEntity p, Job j) {
        Direction[] dirs = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};
        for (Direction d : dirs) {
            BlockPos nb = j.pos.offset(d);
            if (mc.world.getBlockState(nb).isReplaceable()) continue;
            Direction face = d.getOpposite();
            Vec3d hv = Vec3d.ofCenter(nb).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
            p.getInventory().selectedSlot = j.blockSlot;
            shift(p, true);
            ActionResult r = mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hv, face, nb, false));
            shift(p, false);
            if (r.shouldSwingHand()) p.swingHand(Hand.MAIN_HAND);
            return;
        }
    }
}

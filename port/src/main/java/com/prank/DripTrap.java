package com.prank;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class DripTrap {
    private static int cd;

    private static void use(MinecraftClient mc, BlockHitResult h) {
        ActionResult r = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, h);
        if (r.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);
    }

    private static void shift(ClientPlayerEntity p, boolean down) {
        p.networkHandler.sendPacket(new ClientCommandC2SPacket(p, down
            ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY
            : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
    }

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || Freecam.active) return;
        if (cd > 0) cd--;

        if (Modules.DRIPRELEASE.enabled && p.age % 2 == 0) release(mc, p);

        if (!Modules.DRIPTRAP.enabled || cd > 0 || mc.currentScreen != null) return;
        if (!p.getMainHandStack().isOf(Items.POINTED_DRIPSTONE)) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult)) return;
        BlockHitResult bh = (BlockHitResult) mc.crosshairTarget;
        if (bh.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = bh.getBlockPos();
        BlockState st = mc.world.getBlockState(pos);
        if (!(st.getBlock() instanceof TrapdoorBlock)) return;

        BlockHitResult trap = new BlockHitResult(Vec3d.ofCenter(pos), bh.getSide(), pos, false);

        // trapdoor is open: close it first so the trap can be re-armed
        if (st.get(TrapdoorBlock.OPEN)) {
            use(mc, trap);
            cd = 2;
            return;
        }

        // 1) place the dripstone on the trapdoor (server-side sneak so it places instead of opening)
        Direction face = Modules.DRIPTRAP.on("Underside") ? Direction.DOWN : bh.getSide();
        Vec3d fv = Vec3d.ofCenter(pos).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
        shift(p, true);
        use(mc, new BlockHitResult(fv, face, pos, false));
        shift(p, false);

        // 2) release it right away, same tick, no waiting
        use(mc, trap);
        cd = (int) Modules.DRIPTRAP.num("Delay");
    }

    /** Opens any armed trapdoor (dripstone hanging under it) as soon as something stands below. */
    private static void release(MinecraftClient mc, ClientPlayerEntity p) {
        BlockPos c = p.getBlockPos();
        int R = 5;
        for (int x = -R; x <= R; x++) {
            for (int y = -R; y <= R; y++) {
                for (int z = -R; z <= R; z++) {
                    BlockPos tp = c.add(x, y, z);
                    BlockState ts = mc.world.getBlockState(tp);
                    if (!(ts.getBlock() instanceof TrapdoorBlock) || ts.get(TrapdoorBlock.OPEN)) continue;
                    if (!mc.world.getBlockState(tp.down()).isOf(Blocks.POINTED_DRIPSTONE)) continue;
                    if (p.getEyePos().squaredDistanceTo(Vec3d.ofCenter(tp)) > 20.0) continue;

                    Box col = new Box(tp.getX() - 0.3, tp.getY() - 12, tp.getZ() - 0.3,
                        tp.getX() + 1.3, tp.getY(), tp.getZ() + 1.3);
                    boolean found = false;
                    for (Entity e : mc.world.getOtherEntities(null, col)) {
                        if (!(e instanceof LivingEntity)) continue;
                        if (e == p && !Modules.DRIPRELEASE.on("Include Self")) continue;
                        found = true;
                        break;
                    }
                    if (!found) continue;

                    use(mc, new BlockHitResult(Vec3d.ofCenter(tp), Direction.UP, tp, false));
                    return;
                }
            }
        }
    }
}

package com.prank;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Cheats {
    private static boolean zooming, twerking, midDown;
    private static int savedFov = 70;
    private static int totemCd, chestCd, afkTicks;

    public static void restoreFov(MinecraftClient mc) {
        if (zooming && mc.options != null) {
            mc.options.getFov().setValue(savedFov);
            zooming = false;
        }
    }

    private static void zoom(MinecraftClient mc) {
        if (mc.options == null) return;
        if (Modules.ZOOM.enabled) {
            if (!zooming) { savedFov = mc.options.getFov().getValue(); zooming = true; }
            mc.options.getFov().setValue((int) Modules.ZOOM.num("FOV"));
        } else if (zooming) {
            mc.options.getFov().setValue(savedFov);
            zooming = false;
        }
    }

    private static boolean valid(Module m, Entity e) {
        if (e instanceof PlayerEntity) {
            if (Modules.NOFRIENDDAMAGE.enabled && Friends.is(e)) return false;
            return m.on("Players");
        }
        return e instanceof MobEntity && m.on("Mobs");
    }

    private static void face(ClientPlayerEntity p, LivingEntity t) {
        Vec3d d = t.getEyePos().subtract(p.getEyePos());
        double h = Math.hypot(d.x, d.z);
        p.setYaw((float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0));
        p.setPitch((float) -Math.toDegrees(Math.atan2(d.y, h)));
    }

    public static void tick(MinecraftClient mc) {
        // module keybinds
        if (mc.currentScreen == null && mc.getWindow() != null) {
            long h = mc.getWindow().getHandle();
            for (Module m : Modules.ALL) {
                if (m.key <= 0) { m.wasDown = false; continue; }
                boolean d = InputUtil.isKeyPressed(h, m.key);
                if (d && !m.wasDown) m.toggle();
                m.wasDown = d;
            }
        }
        zoom(mc);

        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        if (Modules.TIMECHANGER.enabled) mc.world.setTimeOfDay((long) Modules.TIMECHANGER.num("Time"));
        if (Modules.NOWEATHER.enabled) {
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }

        if (Modules.AUTORESPAWN.enabled && mc.currentScreen instanceof DeathScreen) p.requestRespawn();
        chestStealer(mc, p);
        inventoryMove(mc);

        if (Modules.TWERK.enabled) {
            mc.options.sneakKey.setPressed(p.age % 4 < 2);
            twerking = true;
        } else if (twerking) {
            mc.options.sneakKey.setPressed(false);
            twerking = false;
        }

        // middle click = friend
        if (Modules.MIDDLEFRIEND.enabled && mc.currentScreen == null) {
            boolean d = mc.options.pickItemKey.isPressed();
            if (d && !midDown && mc.targetedEntity instanceof PlayerEntity)
                Friends.toggle((PlayerEntity) mc.targetedEntity);
            midDown = d;
        }

        if (Modules.ELYTRASWAP.enabled) {
            Modules.ELYTRASWAP.enabled = false;
            elytraSwap(mc, p);
        }

        if (Freecam.active) return;


        extra(mc, p);
        movement(mc, p);
        combat(mc, p);
        player(mc, p);
    }

    private static boolean walking, mining;
    private static int armorCd;

    private static void extra(MinecraftClient mc, ClientPlayerEntity p) {
        if (Modules.ANTIVOID.enabled && p.getVelocity().y < 0
            && p.getY() < mc.world.getBottomY() + Modules.ANTIVOID.num("Height")) {
            Vec3d av = p.getVelocity();
            p.setVelocity(av.x, 0.8, av.z);
        }
        if (Modules.DURABILITY.enabled && p.age % 40 == 0) durability(p);
        if (Modules.AIRJUMP.enabled) {
            boolean j = mc.options.jumpKey.isPressed();
            if (p.isOnGround()) airJumps = 0;
            else if (j && !jumpWas && mc.currentScreen == null && airJumps < (int) Modules.AIRJUMP.num("Jumps")) {
                Vec3d jv = p.getVelocity();
                p.setVelocity(jv.x, 0.42, jv.z);
                airJumps++;
            }
            jumpWas = j;
        }
        if (Modules.FASTCLIMB.enabled && p.isClimbing() && mc.options.jumpKey.isPressed()) {
            Vec3d cv = p.getVelocity();
            p.setVelocity(cv.x, Modules.FASTCLIMB.num("Speed"), cv.z);
        }
        if (Modules.FASTFALL.enabled && !p.isOnGround() && mc.options.sneakKey.isPressed() && p.getVelocity().y < 0) {
            Vec3d fv = p.getVelocity();
            p.setVelocity(fv.x, -Modules.FASTFALL.num("Speed"), fv.z);
        }
        if (Modules.ANTIBLIND.enabled) {
            p.removeStatusEffect(StatusEffects.BLINDNESS);
            p.removeStatusEffect(StatusEffects.NAUSEA);
            p.removeStatusEffect(StatusEffects.DARKNESS);
        }
        if (Modules.SAFEWALK.enabled && p.isOnGround() && !p.isSneaking()) {
            Vec3d sv = p.getVelocity();
            Vec3d ahead = p.getPos().add(sv.x * 4, 0, sv.z * 4);
            if ((Math.abs(sv.x) > 0.01 || Math.abs(sv.z) > 0.01)
                && mc.world.getBlockState(BlockPos.ofFloored(ahead.x, p.getY() - 0.5, ahead.z)).isAir())
                p.setVelocity(0, sv.y, 0);
        }
        if (Modules.AUTOREFILL.enabled) refill(mc, p);
        fastPlace(mc, p);
        noJumpDelay(mc, p);

        if (Modules.AUTOWALK.enabled) { mc.options.forwardKey.setPressed(true); walking = true; }
        else if (walking) { mc.options.forwardKey.setPressed(false); walking = false; }

        if (Modules.AUTOMINE.enabled && mc.currentScreen == null) { mc.options.attackKey.setPressed(true); mining = true; }
        else if (mining) { mc.options.attackKey.setPressed(false); mining = false; }

        if (Modules.PARKOUR.enabled && p.isOnGround() && !p.isSneaking()
            && (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed())) {
            Vec3d ahead = p.getPos().add(Vec3d.fromPolar(0, p.getYaw()).multiply(0.5));
            BlockPos below = BlockPos.ofFloored(ahead.x, p.getY() - 0.5, ahead.z);
            if (mc.world.getBlockState(below).isAir()) {
                Vec3d v = p.getVelocity();
                p.setVelocity(v.x, 0.42, v.z);
            }
        }

        if (Modules.AUTOLOG.enabled && p.networkHandler != null
            && p.getHealth() + p.getAbsorptionAmount() <= Modules.AUTOLOG.num("Health")) {
            Modules.AUTOLOG.enabled = false;
            p.networkHandler.getConnection().disconnect(Text.literal("[Kelo] AutoLog: health was low"));
            return;
        }

        if (Modules.AUTOARMOR.enabled && mc.currentScreen == null) {
            if (armorCd > 0) armorCd--; else autoArmor(mc, p);
        }
    }

    private static net.minecraft.item.Item lastHeld;
    private static int refillCd;

    /** When your hand runs empty, pulls the same item in from the inventory. */
    private static void refill(MinecraftClient mc, ClientPlayerEntity p) {
        ItemStack cur = p.getMainHandStack();
        if (!cur.isEmpty()) { lastHeld = cur.getItem(); return; }
        if (lastHeld == null || mc.currentScreen != null) return;
        if (refillCd > 0) { refillCd--; return; }
        for (int i = 9; i < 36; i++) {
            if (p.getInventory().getStack(i).isOf(lastHeld)) {
                mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, i,
                    p.getInventory().selectedSlot, SlotActionType.SWAP, p);
                refillCd = 4;
                return;
            }
        }
    }

    private static boolean jumpWas;
    private static int airJumps;
    private static int placeCd;

    /** Right-click held with a block: place every tick instead of every 4th. */
    private static void fastPlace(MinecraftClient mc, ClientPlayerEntity p) {
        if (!Modules.FASTPLACE.enabled) return;
        if (placeCd > 0) { placeCd--; return; }
        if (mc.currentScreen != null || !mc.options.useKey.isPressed()) return;
        if (!(p.getMainHandStack().getItem() instanceof BlockItem)) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult)) return;
        BlockHitResult bh = (BlockHitResult) mc.crosshairTarget;
        if (bh.getType() != HitResult.Type.BLOCK) return;
        Block b = mc.world.getBlockState(bh.getBlockPos()).getBlock();
        if (b instanceof BlockWithEntity || b instanceof LeverBlock || b instanceof ButtonBlock
            || b instanceof DoorBlock || b instanceof TrapdoorBlock || b instanceof FenceGateBlock) return;
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, bh);
        p.swingHand(Hand.MAIN_HAND);
        placeCd = (int) Modules.FASTPLACE.num("Delay") - 1;
    }

    /** Holding jump on the ground jumps again right away. */
    private static void noJumpDelay(MinecraftClient mc, ClientPlayerEntity p) {
        if (!Modules.NOJUMPDELAY.enabled || mc.currentScreen != null) return;
        Vec3d v = p.getVelocity();
        if (mc.options.jumpKey.isPressed() && p.isOnGround() && v.y <= 0.0) p.setVelocity(v.x, 0.42, v.z);
    }

    private static final boolean[] WARNED = new boolean[4];

    private static void durability(ClientPlayerEntity p) {
        EquipmentSlot[] sl = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        double pct = Modules.DURABILITY.num("Percent") / 100.0;
        for (int i = 0; i < 4; i++) {
            ItemStack st = p.getEquippedStack(sl[i]);
            if (st.isEmpty() || !st.isDamageable()) { WARNED[i] = false; continue; }
            double left = 1.0 - (double) st.getDamage() / st.getMaxDamage();
            if (left < pct && !WARNED[i]) {
                WARNED[i] = true;
                Notifs.push(st.getName().getString() + " is at " + (int) (left * 100) + "%", false);
                Sounds.play("off", 1f, 0.7f);
            } else if (left >= pct) {
                WARNED[i] = false;
            }
        }
    }

    private static void autoArmor(MinecraftClient mc, ClientPlayerEntity p) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int sync = p.playerScreenHandler.syncId;
        for (int k = 0; k < 4; k++) {
            EquipmentSlot sl = slots[k];
            ItemStack cur = p.getEquippedStack(sl);
            if (sl == EquipmentSlot.CHEST && cur.isOf(Items.ELYTRA)) continue;
            int curProt = cur.getItem() instanceof ArmorItem ? ((ArmorItem) cur.getItem()).getProtection() : -1;
            int best = -1, bestProt = curProt;
            for (int i = 0; i < 36; i++) {
                ItemStack st = p.getInventory().getStack(i);
                if (!(st.getItem() instanceof ArmorItem)) continue;
                ArmorItem ai = (ArmorItem) st.getItem();
                if (ai.getSlotType() != sl || ai.getProtection() <= bestProt) continue;
                best = i;
                bestProt = ai.getProtection();
            }
            if (best < 0) continue;
            int src = best < 9 ? 36 + best : best;
            if (cur.isEmpty()) {
                mc.interactionManager.clickSlot(sync, src, 0, SlotActionType.QUICK_MOVE, p);
            } else {
                mc.interactionManager.clickSlot(sync, src, 0, SlotActionType.PICKUP, p);
                mc.interactionManager.clickSlot(sync, 5 + k, 0, SlotActionType.PICKUP, p);
                mc.interactionManager.clickSlot(sync, src, 0, SlotActionType.PICKUP, p);
            }
            armorCd = 6;
            return;
        }
    }

    private static void inventoryMove(MinecraftClient mc) {
        if (!Modules.INVENTORYMOVE.enabled || !(mc.currentScreen instanceof HandledScreen)) return;
        long h = mc.getWindow().getHandle();
        KeyBinding[] ks = {mc.options.forwardKey, mc.options.backKey, mc.options.leftKey,
            mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey};
        for (KeyBinding k : ks) {
            k.setPressed(InputUtil.isKeyPressed(h, KeyBindingHelper.getBoundKeyOf(k).getCode()));
        }
    }

    private static void elytraSwap(MinecraftClient mc, ClientPlayerEntity p) {
        boolean wearingElytra = p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        int src = -1;
        for (int i = 0; i < 36 && src < 0; i++) {
            ItemStack st = p.getInventory().getStack(i);
            if (wearingElytra) {
                if (st.getItem() instanceof ArmorItem
                    && ((ArmorItem) st.getItem()).getSlotType() == EquipmentSlot.CHEST) src = i;
            } else if (st.isOf(Items.ELYTRA)) {
                src = i;
            }
        }
        if (src < 0) { Notifs.push(wearingElytra ? "No chestplate found" : "No elytra found", false); return; }
        int slot = src < 9 ? 36 + src : src;
        int sync = p.playerScreenHandler.syncId;
        mc.interactionManager.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);
        mc.interactionManager.clickSlot(sync, 6, 0, SlotActionType.PICKUP, p);
        mc.interactionManager.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);
        Notifs.push(wearingElytra ? "Swapped to chestplate" : "Swapped to elytra", true);
    }

    private static void movement(MinecraftClient mc, ClientPlayerEntity p) {
        boolean fwd = mc.options.forwardKey.isPressed(), back = mc.options.backKey.isPressed();
        boolean left = mc.options.leftKey.isPressed(), right = mc.options.rightKey.isPressed();
        boolean moving = fwd || back || left || right;
        Vec3d f = Vec3d.fromPolar(0, p.getYaw()), r = Vec3d.fromPolar(0, p.getYaw() + 90);
        Vec3d dir = Vec3d.ZERO;
        if (fwd) dir = dir.add(f);
        if (back) dir = dir.subtract(f);
        if (right) dir = dir.add(r);
        if (left) dir = dir.subtract(r);
        if (dir.lengthSquared() > 0) dir = dir.normalize();
        Vec3d v = p.getVelocity();

        if (Modules.BUNNYHOP.enabled && moving && p.isOnGround())
            p.setVelocity(v.x, Modules.BUNNYHOP.num("Height"), v.z);

        v = p.getVelocity();
        if (Modules.AUTOJUMP.enabled && moving && p.isOnGround() && p.horizontalCollision)
            p.setVelocity(v.x, 0.42, v.z);

        v = p.getVelocity();
        boolean justJumped = mc.options.jumpKey.isPressed() && !p.isOnGround() && v.y > 0.3 && v.y < 0.4;
        if (Modules.HIGHJUMP.enabled && justJumped)
            p.setVelocity(v.x, Modules.HIGHJUMP.num("Height"), v.z);

        v = p.getVelocity();
        if (Modules.LONGJUMP.enabled && justJumped) {
            double b = Modules.LONGJUMP.num("Boost");
            p.setVelocity(v.x * b, v.y, v.z * b);
        }

        v = p.getVelocity();
        if (Modules.SPEED.enabled && moving && (p.isOnGround() || !Modules.SPEED.on("Ground Only"))) {
            double s = 0.22 * Modules.SPEED.num("Mult");
            p.setVelocity(dir.x * s, v.y, dir.z * s);
        }

        v = p.getVelocity();
        if (Modules.SPIDER.enabled && p.horizontalCollision)
            p.setVelocity(v.x, Modules.SPIDER.num("Speed"), v.z);

        v = p.getVelocity();
        if (Modules.JESUS.enabled && p.isTouchingWater() && !p.isSneaking())
            p.setVelocity(v.x, Math.max(v.y, 0.12), v.z);

        v = p.getVelocity();
        double fs = -Modules.GLIDE.num("Fall Speed");
        if (Modules.GLIDE.enabled && !p.isOnGround() && v.y < fs)
            p.setVelocity(v.x, fs, v.z);

        if (Modules.FLY.enabled) {
            double sp = Modules.FLY.num("Speed");
            double vy = 0;
            double vs = Modules.FLY.num("Vertical");
            if (mc.options.jumpKey.isPressed()) vy += vs;
            if (mc.options.sneakKey.isPressed()) vy -= vs;
            if (vy == 0 && Modules.FLY.on("Anti Kick") && p.age % 20 == 0) vy = -0.04;
            p.setVelocity(dir.x * sp, vy, dir.z * sp);
        }

        if (Modules.NOFALL.enabled && p.fallDistance > Modules.NOFALL.num("Distance") && !p.isOnGround())
            p.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));

        if (Modules.AUTOSPRINT.enabled && fwd && !p.isSneaking() && !p.horizontalCollision
            && p.getHungerManager().getFoodLevel() > 6)
            p.setSprinting(true);
    }

    private static void combat(MinecraftClient mc, ClientPlayerEntity p) {
        if (mc.currentScreen != null) return;
        double cdTh = Modules.KILLAURA.enabled ? Modules.KILLAURA.num("Cooldown") : Modules.TRIGGERBOT.num("Cooldown");
        boolean ready = p.getAttackCooldownProgress(0.5f) >= cdTh;

        if (Modules.KILLAURA.enabled && ready) {
            LivingEntity best = null;
            double range = Modules.KILLAURA.num("Range") + 0.001, bd = 1e9;
            boolean lowHp = Modules.KILLAURA.num("Priority") > 1.5;
            for (Entity e : mc.world.getEntities()) {
                if (e == p || !(e instanceof LivingEntity)) continue;
                LivingEntity le = (LivingEntity) e;
                if (!le.isAlive() || !valid(Modules.KILLAURA, e)) continue;
                double d = p.distanceTo(e);
                double sc = lowHp ? le.getHealth() : d;
                if (d <= range && sc < bd) { bd = sc; best = le; }
            }
            if (best != null) {
                if (Modules.KILLAURA.on("Rotate")) face(p, best);
                mc.interactionManager.attackEntity(p, best);
                p.swingHand(Hand.MAIN_HAND);
                ready = false;
            }
        }

        if (ready && mc.targetedEntity instanceof LivingEntity) {
            LivingEntity t = (LivingEntity) mc.targetedEntity;
            boolean trigger = Modules.TRIGGERBOT.enabled && valid(Modules.TRIGGERBOT, t);
            boolean clicker = Modules.AUTOCLICKER.enabled && mc.options.attackKey.isPressed()
                && !(Modules.NOFRIENDDAMAGE.enabled && Friends.is(t));
            if (t.isAlive() && (trigger || clicker)) {
                mc.interactionManager.attackEntity(p, t);
                p.swingHand(Hand.MAIN_HAND);
            }
        }

        if (Modules.AIMASSIST.enabled
            && (!Modules.AIMASSIST.on("Click Only") || mc.options.attackKey.isPressed())) {
            double range = Modules.AIMASSIST.num("Range");
            double bestAng = Modules.AIMASSIST.num("FOV");
            float by = 0, bp = 0;
            boolean found = false;
            for (PlayerEntity t : mc.world.getPlayers()) {
                if (t == p || !t.isAlive() || p.distanceTo(t) > range) continue;
                if (Modules.NOFRIENDDAMAGE.enabled && Friends.is(t)) continue;
                Vec3d d = t.getBoundingBox().getCenter().subtract(p.getEyePos());
                float ty = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
                float tp = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
                float dy = MathHelper.wrapDegrees(ty - p.getYaw()), dp = tp - p.getPitch();
                double ang = Math.hypot(dy, dp);
                if (ang < bestAng) { bestAng = ang; by = dy; bp = dp; found = true; }
            }
            if (found) {
                float sp = (float) Modules.AIMASSIST.num("Speed");
                p.setYaw(p.getYaw() + MathHelper.clamp(by, -sp, sp));
                p.setPitch(MathHelper.clamp(p.getPitch() + MathHelper.clamp(bp, -sp, sp), -90f, 90f));
            }
        }

        if (Modules.AUTOTOTEM.enabled) {
            if (totemCd > 0) {
                totemCd--;
            } else if (!p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) && p.getHealth() <= Modules.AUTOTOTEM.num("Health")) {
                for (int i = 0; i < 36; i++) {
                    if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                        int slot = i < 9 ? 36 + i : i;
                        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 40,
                            SlotActionType.SWAP, p);
                        totemCd = (int) Modules.AUTOTOTEM.num("Delay");
                        break;
                    }
                }
            }
        }
    }

    private static void player(MinecraftClient mc, ClientPlayerEntity p) {
        // AutoTool
        if (Modules.AUTOTOOL.enabled && mc.currentScreen == null && mc.options.attackKey.isPressed()
            && !p.isCreative() && mc.crosshairTarget instanceof BlockHitResult) {
            BlockHitResult bh = (BlockHitResult) mc.crosshairTarget;
            if (bh.getType() == HitResult.Type.BLOCK) {
                BlockState st = mc.world.getBlockState(bh.getBlockPos());
                int bi = p.getInventory().selectedSlot;
                float best = p.getInventory().getStack(bi).getMiningSpeedMultiplier(st);
                for (int i = 0; i < 9; i++) {
                    float s = p.getInventory().getStack(i).getMiningSpeedMultiplier(st);
                    if (s > best) { best = s; bi = i; }
                }
                p.getInventory().selectedSlot = bi;
            }
        }

        // Scaffold
        if (Modules.SCAFFOLD.enabled && mc.currentScreen == null
            && p.getMainHandStack().getItem() instanceof BlockItem) {
            BlockPos pos = p.getBlockPos().down();
            if (mc.world.getBlockState(pos).isReplaceable()) {
                Direction[] dirs = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                for (Direction d : dirs) {
                    BlockPos nb = pos.offset(d);
                    if (mc.world.getBlockState(nb).isReplaceable()) continue;
                    Direction side = d.getOpposite();
                    Vec3d hv = Vec3d.ofCenter(nb).add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
                    mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hv, side, nb, false));
                    p.swingHand(Hand.MAIN_HAND);
                    break;
                }
            }
        }

        // AntiAFK
        if (Modules.ANTIAFK.enabled) {
            if (++afkTicks > (int) (Modules.ANTIAFK.num("Interval") * 20)) {
                afkTicks = 0;
                p.swingHand(Hand.MAIN_HAND);
                Vec3d v = p.getVelocity();
                if (p.isOnGround()) p.setVelocity(v.x, 0.42, v.z);
            }
        }
    }

    private static void chestStealer(MinecraftClient mc, ClientPlayerEntity p) {
        if (!Modules.CHESTSTEALER.enabled || !(mc.currentScreen instanceof GenericContainerScreen)) return;
        if (chestCd > 0) { chestCd--; return; }
        GenericContainerScreen gs = (GenericContainerScreen) mc.currentScreen;
        GenericContainerScreenHandler h = gs.getScreenHandler();
        int n = h.getRows() * 9;
        for (int i = 0; i < n; i++) {
            if (!h.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, p);
                chestCd = (int) Modules.CHESTSTEALER.num("Delay");
                break;
            }
        }
    }
}

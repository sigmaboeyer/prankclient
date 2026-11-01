package com.prank.mixin;

import com.prank.Freecam;
import com.prank.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void prank$glow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (Modules.GLOW.enabled && self instanceof PlayerEntity
            && self != MinecraftClient.getInstance().player)
            cir.setReturnValue(true);
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void prank$glowColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (Modules.GLOW.enabled && self instanceof PlayerEntity
            && self != MinecraftClient.getInstance().player)
            cir.setReturnValue(Modules.GLOW.getColor() & 0xFFFFFF);
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void prank$seeInvisible(PlayerEntity viewer, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.ANTI_INVIS.enabled) cir.setReturnValue(false);
    }

    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void prank$hitbox(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        if (Modules.HITBOX.enabled && self instanceof PlayerEntity
            && self != MinecraftClient.getInstance().player)
            cir.setReturnValue(cir.getReturnValue() + (float) Modules.HITBOX.num("Size"));
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void prank$freecamLook(double dx, double dy, CallbackInfo ci) {
        if (Freecam.active && (Object) this == MinecraftClient.getInstance().player) {
            Freecam.look(dx, dy);
            ci.cancel();
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void prank$noPush(Entity other, CallbackInfo ci) {
        if (Modules.NOPUSH.enabled && Modules.NOPUSH.on("Entities") && (Object) this == MinecraftClient.getInstance().player) ci.cancel();
    }

    @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
    private void prank$noFire(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.NOOVERLAY.enabled && (Object) this == MinecraftClient.getInstance().player)
            cir.setReturnValue(false);
    }
}

package com.prank.mixin;

import com.prank.Freecam;
import com.prank.Modules;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class PlayerMixin {
    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void prank$freeze(CallbackInfo ci) {
        if (Freecam.active) ci.cancel();
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void prank$noBlockPush(double x, double z, CallbackInfo ci) {
        if (Modules.NOPUSH.enabled && Modules.NOPUSH.on("Blocks")) ci.cancel();
    }
}

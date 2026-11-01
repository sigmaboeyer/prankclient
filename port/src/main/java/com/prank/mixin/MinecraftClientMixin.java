package com.prank.mixin;

import com.prank.AutoWater;
import com.prank.Freecam;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void prank$noAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.active) cir.setReturnValue(false);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void prank$noUse(CallbackInfo ci) {
        if (Freecam.active) { ci.cancel(); return; }
        if (AutoWater.intercept((MinecraftClient) (Object) this)) ci.cancel();
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void prank$noBreak(boolean breaking, CallbackInfo ci) {
        if (Freecam.active) ci.cancel();
    }
}

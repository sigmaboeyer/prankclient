package com.prank.mixin;

import com.prank.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "isPushedByFluids", at = @At("HEAD"), cancellable = true)
    private void prank$noFluidPush(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.NOPUSH.enabled && Modules.NOPUSH.on("Liquids")
            && (Object) this == MinecraftClient.getInstance().player)
            cir.setReturnValue(false);
    }
}

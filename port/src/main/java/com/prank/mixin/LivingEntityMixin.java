package com.prank.mixin;

import com.prank.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void prank$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        if (Modules.CUSTOMSWING.enabled && (Object) this == MinecraftClient.getInstance().player)
            cir.setReturnValue((int) Modules.CUSTOMSWING.num("Speed"));
    }
}

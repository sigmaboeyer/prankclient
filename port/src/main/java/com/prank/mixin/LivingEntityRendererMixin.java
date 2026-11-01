package com.prank.mixin;

import com.prank.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void prank$hideVanillaTag(LivingEntity e, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.NAMETAGS.enabled && Modules.NAMETAGS.on("Hide Vanilla")
            && e instanceof PlayerEntity && e != MinecraftClient.getInstance().player)
            cir.setReturnValue(false);
    }
}

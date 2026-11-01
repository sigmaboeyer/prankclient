package com.prank.mixin;

import com.prank.Friends;
import com.prank.Hooks;
import com.prank.Modules;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class InteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void prank$attack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (Modules.NOFRIENDDAMAGE.enabled && Friends.is(target)) { ci.cancel(); return; }
        Hooks.onAttack(player, target);
    }
}

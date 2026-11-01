package com.prank.mixin;

import com.prank.Hooks;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class NetworkMixin {
    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void prank$status(EntityStatusS2CPacket packet, CallbackInfo ci) {
        Hooks.onStatus(packet);
    }
}

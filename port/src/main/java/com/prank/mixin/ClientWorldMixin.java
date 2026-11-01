package com.prank.mixin;

import com.prank.Modules;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    private static Vec3d prank$sky() {
        int c = Modules.CUSTOMSKY.getColor();
        double b = Modules.CUSTOMSKY.num("Brightness");
        return new Vec3d(Math.min(1.0, ((c >> 16) & 255) / 255.0 * b),
                         Math.min(1.0, ((c >> 8) & 255) / 255.0 * b),
                         Math.min(1.0, (c & 255) / 255.0 * b));
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void prank$skyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (Modules.CUSTOMSKY.enabled) cir.setReturnValue(prank$sky());
    }

    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
    private void prank$cloudColor(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (Modules.CUSTOMSKY.enabled && Modules.CUSTOMSKY.on("Clouds")) cir.setReturnValue(prank$sky());
    }
}

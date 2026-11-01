package com.prank.mixin;

import com.prank.Modules;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private float prank$swing(float p) {
        if (!Modules.CUSTOMSWING.enabled) return p;
        switch ((int) Modules.CUSTOMSWING.num("Style")) {
            case 2: return p * p;
            case 3: return p * p * (3f - 2f * p);
            case 4: return p + 0.18f * (float) Math.sin(p * Math.PI * 3.0) * (1f - p);
            default: return (float) Math.sin(p * Math.PI / 2.0);
        }
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), ordinal = 3, argsOnly = true)
    private float prank$equip(float e) {
        return Modules.CUSTOMSWING.enabled && Modules.CUSTOMSWING.on("No Equip Anim") ? 0f : e;
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void prank$xform(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                             float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                             VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (!Modules.CUSTOMSWING.enabled) return;
        float s = (float) Modules.CUSTOMSWING.num("Scale");
        matrices.scale(s, s, s);
        matrices.translate(0.0, Modules.CUSTOMSWING.num("Y Offset"), 0.0);
    }
}

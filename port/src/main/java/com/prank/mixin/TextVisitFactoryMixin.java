package com.prank.mixin;

import com.prank.NameProtect;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
    @ModifyVariable(method = "visitFormatted", at = @At("HEAD"), argsOnly = true)
    private static String prank$protect(String text) {
        return NameProtect.apply(text);
    }
}

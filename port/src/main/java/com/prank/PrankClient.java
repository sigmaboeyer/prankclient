
package com.prank;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class PrankClient implements ClientModInitializer {
    private static KeyBinding guiKey;

    @Override
    public void onInitializeClient() {
        Config.load();

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.prank.gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            KeyBinding.Category.create(Identifier.of("prankclient", "general"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (guiKey.wasPressed()) mc.setScreen(new ModuleScreen());
            Esp.tick(mc);
            Amethyst.tick(mc);
            Freecam.tick(mc);
            Cheats.tick(mc);
            DripTrap.tick(mc);
            AutoWater.tick(mc);
            Fx.tick(mc);
            Waypoints.tick(mc);

            if (mc.player == null) return;

            if (Modules.FULLBRIGHT.enabled) {
                mc.player.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false)
                );
            } else if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            Cheats.restoreFov(mc);
            Config.save();
        });

        WorldRenderEvents.LAST.register(Esp::world);
        HudRenderCallback.EVENT.register(Esp::hud);
    }
}


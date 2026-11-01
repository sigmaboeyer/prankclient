package com.prank;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/** Safe access to private int fields (works in dev and in the real game). */
public class Reflect {
    private static final Map<String, Field> CACHE = new HashMap<>();

    public static void setInt(Object target, Class<?> cls, String named, int value) {
        String key = cls.getName() + "#" + named;
        try {
            if (!CACHE.containsKey(key)) {
                try {
                    String n = FabricLoader.getInstance().getMappingResolver()
                        .mapFieldName("named", cls.getName(), named, "I");
                    Field f = cls.getDeclaredField(n);
                    f.setAccessible(true);
                    CACHE.put(key, f);
                } catch (Throwable t) {
                    CACHE.put(key, null);
                }
            }
            Field f = CACHE.get(key);
            if (f != null && target != null) f.setInt(target, value);
        } catch (Throwable ignored) { }
    }

    public static void clearJumpCooldown(LivingEntity e) {
        setInt(e, LivingEntity.class, "jumpingCooldown", 0);
    }
}

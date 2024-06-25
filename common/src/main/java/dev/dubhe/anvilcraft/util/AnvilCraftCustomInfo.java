package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnvilCraftCustomInfo {
    public static final Map<String, List<String>> INTEGRATIONS = Collections.synchronizedMap(new HashMap<>());

    /**
     * 应用
     */
    public static void apply() {
        for (Map.Entry<String, List<String>> entry : AnvilCraftCustomInfo.INTEGRATIONS.entrySet()) {
            String modid = entry.getKey();
            if (Utils.isLoaded(modid)) {
                AnvilCraft.LOGGER.info("{}'s integrations is loading...", modid);
                List<String> classes = entry.getValue();
                classes.forEach(AnvilCraftCustomInfo::apply);
                AnvilCraft.LOGGER.info("{}'s integrations is loaded!", modid);
            }
        }
    }

    private static void apply(String name) {
        try {
            Class<?> clazz = Class.forName(name);
            if (!clazz.isAssignableFrom(Integration.class)) return;
            Class<? extends Integration> integrationClass = clazz.asSubclass(Integration.class);
            Integration integration = integrationClass.getDeclaredConstructor().newInstance();
            integration.apply();
        } catch (Exception ex) {
            AnvilCraft.LOGGER.error(ex.getMessage(), ex);
        }
    }
}

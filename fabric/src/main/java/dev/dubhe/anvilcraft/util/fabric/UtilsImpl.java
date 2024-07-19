package dev.dubhe.anvilcraft.util.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class UtilsImpl {
    public static boolean isLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }
}

package dev.dubhe.anvilcraft.util.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class UtilsImpl {
    public static boolean onlyJei() {
        return FabricLoader.getInstance().isModLoaded("jei") && !FabricLoader.getInstance().isModLoaded("emi");
    }
}

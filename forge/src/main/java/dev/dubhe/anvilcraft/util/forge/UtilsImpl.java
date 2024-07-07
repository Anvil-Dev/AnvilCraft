package dev.dubhe.anvilcraft.util.forge;

import net.minecraftforge.fml.ModList;

public class UtilsImpl {
    public static boolean isLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }
}

package dev.dubhe.anvilcraft.util.forge;

import net.minecraftforge.fml.ModList;

public class UtilsImpl {
    public static boolean onlyJei() {
        return ModList.get().isLoaded("jei") && !ModList.get().isLoaded("emi");
    }
}

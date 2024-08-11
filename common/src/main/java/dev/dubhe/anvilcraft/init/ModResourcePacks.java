package dev.dubhe.anvilcraft.init;

import dev.anvilcraft.lib.registrar.ResourcePacksHelper;
import dev.dubhe.anvilcraft.AnvilCraft;

public class ModResourcePacks {
    public static void register() {
        ResourcePacksHelper.registerBuiltinResourcePack(
            AnvilCraft.of("transparent_cauldron"),
            ResourcePacksHelper.PackType.CLIENT
        );
    }
}

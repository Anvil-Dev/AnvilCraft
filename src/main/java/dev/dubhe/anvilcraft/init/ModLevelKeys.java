package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ModLevelKeys {
    public static final ResourceKey<Level> MUN = ResourceKey.create(Registries.DIMENSION, AnvilCraft.of("mun"));
    public static final ResourceKey<Level> VOID_PLANET = ResourceKey.create(Registries.DIMENSION, AnvilCraft.of("void_planet"));

    private ModLevelKeys() {
    }
}

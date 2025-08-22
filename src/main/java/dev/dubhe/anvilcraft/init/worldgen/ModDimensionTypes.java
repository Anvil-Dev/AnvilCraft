package dev.dubhe.anvilcraft.init.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.OptionalLong;

public class ModDimensionTypes {

    public static final ResourceKey<DimensionType> MUN = key("mun");
    public static final ResourceKey<Level> MUN_LEVEL = ResourceKey.create(Registries.DIMENSION, AnvilCraft.of("mun"));

    public static void bootstrap(BootstrapContext<DimensionType> context) {
        context.register(MUN, new DimensionType(
            OptionalLong.of(6000),
            true,
            false,
            false,
            false,
            1.0,
            false,
            true,
            -64,
            384,
            384,
            ModBlockTags.INFINIBURN_MUN,
            MUN_LEVEL.location(),
            0,
            new DimensionType.MonsterSettings(
                false,
                true,
                ConstantInt.ZERO,
                0
            )
        ));
    }

    private static ResourceKey<DimensionType> key(String id) {
        return ResourceKey.create(Registries.DIMENSION_TYPE, AnvilCraft.of(id));
    }
}

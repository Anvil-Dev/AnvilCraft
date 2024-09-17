package dev.dubhe.anvilcraft.data.tags;

import dev.dubhe.anvilcraft.init.ModFluidTags;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import org.jetbrains.annotations.NotNull;

public class FluidTagLoader {

    private static ResourceKey<Fluid> findResourceKey(Fluid item) {
        return ResourceKey.create(Registries.FLUID, BuiltInRegistries.FLUID.getKey(item));
    }

    /**
     * 物品标签生成器初始化
     *
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateTagsProvider<Fluid> provider) {
        provider.addTag(ModFluidTags.MENGER_SPONGE_CAN_ABSORB)
                .add(findResourceKey(Fluids.WATER))
                .add(findResourceKey(Fluids.FLOWING_WATER))
                .add(findResourceKey(Fluids.LAVA))
                .add(findResourceKey(Fluids.FLOWING_LAVA));
    }
}

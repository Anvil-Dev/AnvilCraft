package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.Fluid;

public class FluidTagLoader {

    private static ResourceKey<Fluid> findResourceKey(Fluid item) {
        return ResourceKey.create(Registries.FLUID, BuiltInRegistries.FLUID.getKey(item));
    }

    /**
     * 物品标签生成器初始化
     *
     * @param provider 提供器
     */
    public static void init(RegistrumTagsProvider<Fluid> provider) {
        provider.addTag(ModFluidTags.OIL)
            .add(findResourceKey(ModFluids.OIL.get()))
            .add(findResourceKey(ModFluids.FLOWING_OIL.get()));
        provider.addTag(ModFluidTags.EXPERIENCE)
            .add(findResourceKey(ModFluids.EXP_FLUID.get()))
            .add(findResourceKey(ModFluids.FLOWING_EXP_FLUID.get()));
        TagsProvider.TagAppender<Fluid> appender = provider.addTag(ModFluidTags.CEMENT);
        ModFluids.SOURCE_CEMENTS.forEach((color, cement) -> appender.add(findResourceKey(cement.get())));
        ModFluids.FLOWING_CEMENTS.forEach((color, cement) -> appender.add(findResourceKey(cement.get())));
        provider.addTag(ModFluidTags.IGNITABLE)
            .add(findResourceKey(ModFluids.OIL.get()));
    }
}

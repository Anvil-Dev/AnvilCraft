package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagBuilder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FluidTagLoader {

    private static Identifier findId(Fluid item) {
        return BuiltInRegistries.FLUID.getKey(item);
    }

    /**
     * 物品标签生成器初始化
     *
     * @param provider 提供器
     */
    public static void init(RegistrumTagsProvider<Fluid> provider) {
        provider.rawBuilder(ModFluidTags.MENGER_SPONGE_CAN_ABSORB)
            .addElement(findId(Fluids.WATER))
            .addElement(findId(Fluids.FLOWING_WATER))
            .addElement(findId(Fluids.LAVA))
            .addElement(findId(Fluids.FLOWING_LAVA))
            .addElement(ModFluids.OIL.getId())
            .addElement(ModFluids.FLOWING_OIL.getId())
            .addElement(ModFluids.MELT_GEM.getId())
            .addElement(ModFluids.FLOWING_MELT_GEM.getId())
            .addElement(ModFluids.EXP_FLUID.getId())
            .addTag(ModFluidTags.CEMENT.location());
        provider.rawBuilder(ModFluidTags.OIL)
            .addElement(ModFluids.OIL.getId())
            .addElement(ModFluids.FLOWING_OIL.getId());
        provider.rawBuilder(ModFluidTags.EXPERIENCE)
            .addElement(ModFluids.EXP_FLUID.getId())
            .addElement(ModFluids.FLOWING_EXP_FLUID.getId());
        TagBuilder builder = provider.rawBuilder(ModFluidTags.CEMENT);
        ModFluids.SOURCE_CEMENTS.forEach((_, cement) -> builder.addElement(cement.getId()));
        ModFluids.FLOWING_CEMENTS.forEach((_, cement) -> builder.addElement(cement.getId()));
        provider.rawBuilder(ModFluidTags.IGNITABLE)
            .addElement(ModFluids.OIL.getId());
    }
}

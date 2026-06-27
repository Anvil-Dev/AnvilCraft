package dev.dubhe.anvilcraft.init.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cauldron.Layered4LevelCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.RegisterCauldronFluidContentEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ModCauldronFluidContents {
    @SubscribeEvent
    public static void register(RegisterCauldronFluidContentEvent event) {
        event.register(ModBlocks.MELT_GEM_CAULDRON.get(), ModFluids.MELT_GEM.get(), FluidType.BUCKET_VOLUME, null);
        event.register(ModBlocks.OIL_CAULDRON.get(), ModFluids.OIL.get(), FluidType.BUCKET_VOLUME, Layered4LevelCauldronBlock.LEVEL);
        event.register(Blocks.POWDER_SNOW_CAULDRON, ModFluids.POWDER_SNOW.get(), FluidType.BUCKET_VOLUME, LayeredCauldronBlock.LEVEL);
        event.register(
            ModBlocks.EXP_FLUID_CAULDRON.get(),
            ModFluids.EXP_FLUID.get(),
            FluidType.BUCKET_VOLUME,
            Layered4LevelCauldronBlock.LEVEL
        );
        event.register(
            ModBlocks.MILK_CAULDRON.get(),
            NeoForgeMod.MILK.value(),
            FluidType.BUCKET_VOLUME,
            Layered4LevelCauldronBlock.LEVEL
        );
        event.register(
            ModBlocks.HONEY_CAULDRON.get(),
            ModFluids.HONEY.get(),
            FluidType.BUCKET_VOLUME,
            Layered4LevelCauldronBlock.LEVEL
        );
        for (Color color : ModBlocks.CEMENT_CAULDRONS.keySet()) {
            event.register(
                ModBlocks.CEMENT_CAULDRONS.get(color).get(),
                ModFluids.SOURCE_CEMENTS.get(color).get(),
                FluidType.BUCKET_VOLUME,
                null
            );
        }
    }
}

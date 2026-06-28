package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.Layered4LevelCauldronBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.RegisterCauldronFluidContentEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class CauldronFluidContentEventListener {
    @SubscribeEvent
    public static void registerCauldronFluidContent(RegisterCauldronFluidContentEvent event) {
        event.register(ModBlocks.OIL_CAULDRON.get(), ModFluids.OIL.get(), 1000, Layered4LevelCauldronBlock.LEVEL);
        event.register(ModBlocks.MILK_CAULDRON.get(), NeoForgeMod.MILK.get(), 1000, Layered4LevelCauldronBlock.LEVEL);
        event.register(ModBlocks.HONEY_CAULDRON.get(), ModFluids.HONEY.get(), 1000, Layered4LevelCauldronBlock.LEVEL);
        event.register(Blocks.POWDER_SNOW_CAULDRON, ModFluids.POWDER_SNOW.get(), 1000, LayeredCauldronBlock.LEVEL);
        event.register(ModBlocks.EXP_FLUID_CAULDRON.get(), ModFluids.EXP_FLUID.get(), 1000, Layered4LevelCauldronBlock.LEVEL);
        event.register(ModBlocks.MELT_GEM_CAULDRON.get(), ModFluids.MELT_GEM.get(), 1000, null);
        ModBlocks.CEMENT_CAULDRONS.forEach((key, value) -> event.register(
            value.get(),
            ModFluids.SOURCE_CEMENTS.get(key).get(),
            1000,
            null
        ));
    }
}

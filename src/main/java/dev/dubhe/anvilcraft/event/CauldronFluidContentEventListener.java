package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.OilCauldronBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.RegisterCauldronFluidContentEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class CauldronFluidContentEventListener {
    @SubscribeEvent
    public static void registerCauldronFluidContent(RegisterCauldronFluidContentEvent event) {
        event.register(ModBlocks.OIL_CAULDRON.get(), ModFluids.OIL.get(), 1000, OilCauldronBlock.LEVEL);
        ModBlocks.CEMENT_CAULDRONS.forEach((key, value) -> event.register(
            value.get(),
            ModFluids.SOURCE_CEMENTS.get(key).get(),
            1000,
            null
        ));
        event.register(ModBlocks.MELT_GEM_CAULDRON.get(), ModFluids.MELT_GEM.get(), 1000, null);
        event.register(ModBlocks.MILK_CAULDRON.get(), NeoForgeMod.MILK.value(), 1000, null);
    }
}

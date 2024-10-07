package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.OilCauldronBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModFluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.fluids.RegisterCauldronFluidContentEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
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
    }
}

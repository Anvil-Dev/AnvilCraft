package dev.dubhe.anvilcraft.event.forge;


import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD, modid = AnvilCraft.MOD_ID)
public class RegisterAdditionalEventListener {

    /**
     * 注册模型
     */
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register( ModelResourceLocation.standalone(AnvilCraft.of("crab_claw_holding_block")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("crab_claw_holding_item")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("heliostats_head")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("creative_generator_cube")));
    }
}

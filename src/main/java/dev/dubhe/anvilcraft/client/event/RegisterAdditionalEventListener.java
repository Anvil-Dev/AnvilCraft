package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CelestialForgingAnvilBlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class RegisterAdditionalEventListener {

    /**
     * 注册模型
     */
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("item/crab_claw_holding_block")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("item/crab_claw_holding_item")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/heliostats_head")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/heliostats_head_sunflower")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/creative_generator_head")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/charge_collector_head")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/fe_collector_head")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/heat_collector_head")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/void_energy_collector_head")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/laser")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/axis")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/advanced_comparator_indicator")));
        event.register(CelestialForgingAnvilBlockEntityRenderer.RING1);
        event.register(CelestialForgingAnvilBlockEntityRenderer.RING2);
        event.register(CelestialForgingAnvilBlockEntityRenderer.RING3);
        event.register(CelestialForgingAnvilBlockEntityRenderer.RING4);
        event.register(CelestialForgingAnvilBlockEntityRenderer.RING5);
        event.register(CelestialForgingAnvilBlockEntityRenderer.RING6);
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/fire_cauldron_fire4")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/smart_block_placer_base")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/smart_block_placer_upperarm")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/smart_block_placer_forearm")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/smart_block_placer_claw")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/smart_block_placer_claw_open")));
    }
}

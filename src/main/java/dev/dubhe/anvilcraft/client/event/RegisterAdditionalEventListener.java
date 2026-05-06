package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.blockentity.AdvancedComparatorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FishTankRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class RegisterAdditionalEventListener {

    /**
     * 注册模型
     */
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("item/crab_claw_holding_block")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("item/crab_claw_holding_item")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/heliostats_head")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/heliostats_head_sunflower")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/creative_generator_cube")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/charge_collector_cube")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/heat_collector_cube")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/void_energy_collector_head")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/laser")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/axis")));
        event.register(
            AdvancedComparatorRenderer.INDICATOR,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/advanced_comparator_indicator"))
        );
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_1")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_2")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_3")));
        event.register(
            FishTankRenderer.FIRE,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/fire_cauldron_fire4"))
        );
    }
}

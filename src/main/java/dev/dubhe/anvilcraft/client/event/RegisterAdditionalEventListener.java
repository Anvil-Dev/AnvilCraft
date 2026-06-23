package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.blockentity.AdvancedComparatorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CFARenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.ChargeCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CreativeGeneratorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FeCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FishTankRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HeatCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HeliostatsRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.PumpBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.SmartBlockPlacerRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.VoidEnergyCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.CrabClawItemInHandRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.SpectralSlingshotRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.SpectralWeaponLauncherRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class RegisterAdditionalEventListener {

    /// 注册模型
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(
            CrabClawItemInHandRenderer.HOLDING_BLOCK,
            SimpleUnbakedStandaloneModel.quadCollection(AnvilCraft.of("item/crab_claw_holding_block"))
        );
        event.register(
            CrabClawItemInHandRenderer.HOLDING_ITEM,
            SimpleUnbakedStandaloneModel.quadCollection(AnvilCraft.of("item/crab_claw_holding_item"))
        );
        event.register(
            HeliostatsRenderer.HEAD,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/heliostats_head"))
        );
        event.register(
            HeliostatsRenderer.HEAD_SUNFLOWER,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/heliostats_head_sunflower"))
        );
        event.register(
            CreativeGeneratorRenderer.CUBE,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/creative_generator_cube"))
        );
        event.register(
            ChargeCollectorRenderer.CUBE,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/charge_collector_cube"))
        );
        event.register(
            HeatCollectorRenderer.CUBE,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/heat_collector_cube"))
        );
        event.register(
            VoidEnergyCollectorRenderer.HEAD,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/void_energy_collector_head"))
        );
        event.register(
            FeCollectorRenderer.MODEL,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/fe_collector_head"))
        );
        event.register(
            HammerEffectRenderEventListener.MODEL,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/axis"))
        );
        event.register(
            AdvancedComparatorRenderer.INDICATOR,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/advanced_comparator_indicator"))
        );
        event.register(
            CFARenderer.RING1,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_1"))
        );
        event.register(
            CFARenderer.RING2,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_2"))
        );
        event.register(
            CFARenderer.RING3,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_3"))
        );
        event.register(
            FishTankRenderer.FIRE,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/oil_cauldron_fire4"))
        );
        event.register(
            SmartBlockPlacerRenderer.BASE_MODEL,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/smart_block_placer_base"))
        );
        event.register(
            SmartBlockPlacerRenderer.UPPERARM_MODEL,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/smart_block_placer_upperarm"))
        );
        event.register(
            SmartBlockPlacerRenderer.FOREARM_MODEL,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/smart_block_placer_forearm"))
        );
        event.register(
            SmartBlockPlacerRenderer.CLAW_MODEL,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/smart_block_placer_claw"))
        );
        event.register(
            SmartBlockPlacerRenderer.CLAW_OPEN_MODEL,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/smart_block_placer_claw_open"))
        );
        event.register(
            PumpBlockEntityRenderer.PUMP_PISTON_1,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/pump_piston_1"))
        );
        event.register(
            PumpBlockEntityRenderer.PUMP_PISTON_2,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/pump_piston_2"))
        );
    }

    @SubscribeEvent
    public static void registerSpecialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(AnvilCraft.of("spectral_slingshot"), SpectralSlingshotRenderer.Unbaked.CODEC);
        event.register(AnvilCraft.of("spectral_weapon_launcher"), SpectralWeaponLauncherRenderer.Unbaked.CODEC);
    }
}

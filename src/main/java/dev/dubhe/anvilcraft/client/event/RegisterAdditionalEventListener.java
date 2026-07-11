package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.cfa.CfaPreviewPipRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.AdvancedComparatorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CFARenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.ChargeCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.ControlValveBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CreativeGeneratorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FeCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FishTankRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HeatCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HeliostatsRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.InfiniteCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.PipeCheckValveBERenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.PumpBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.SmartBlockPlacerRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.VoidEnergyCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.WipBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.CrabClawItemInHandRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.SpectralSlingshotRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.SpectralWeaponLauncherRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
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
            CreativeGeneratorRenderer.HEAD,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/creative_generator_head"))
        );
        event.register(
            ChargeCollectorRenderer.HEAD,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/charge_collector_head"))
        );
        event.register(
            HeatCollectorRenderer.HEAD,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/heat_collector_head"))
        );
        event.register(
            VoidEnergyCollectorRenderer.HEAD,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/void_energy_collector_head"))
        );
        event.register(
            InfiniteCollectorRenderer.HEAD,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/infinite_collector_head"))
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
            CFARenderer.RING4,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4"))
        );
        event.register(
            CFARenderer.RING5,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_5"))
        );
        event.register(
            CFARenderer.RING6,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_6"))
        );
        // 锻星砧巨构模型。
        event.register(
            CFARenderer.R1_EXCAVATOR,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_1_excavator"))
        );
        event.register(
            CFARenderer.R1_EXCAVATOR_OFF,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_1_excavator_off"))
        );
        event.register(
            CFARenderer.R1_EXTRACTOR,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_1_exctractor"))
        );
        event.register(
            CFARenderer.R2_EXTRACTOR,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_2_exctractor"))
        );
        event.register(
            CFARenderer.R1_ECO_STATION,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_1_eco_station"))
        );
        event.register(
            CFARenderer.R1_TEMPLE,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_1_temple"))
        );
        event.register(
            CFARenderer.R4_COLLIDER,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4_collider"))
        );
        event.register(
            CFARenderer.R4_DYSON_SPHERE,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4_dyson_sphere"))
        );
        event.register(
            CFARenderer.R5_DYSON_SPHERE,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_5_dyson_sphere"))
        );
        event.register(
            CFARenderer.R4_COIL,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4_coil"))
        );
        event.register(
            CFARenderer.R4_COIL_FIX,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4_coil_fix"))
        );
        event.register(
            CFARenderer.R4_COIL_RING,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4_coil_ring"))
        );
        event.register(
            CFARenderer.R4_PENROSE_SPHERE,
            SimpleUnbakedStandaloneModel.blockStateModel(
                AnvilCraft.of("block/celestial_forging_anvil_ring_4_penrose_sphere")
            )
        );
        event.register(
            CFARenderer.R4_PENROSE_SPHERE_FIX,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4_penrose_sphere_fix"))
        );
        event.register(
            CFARenderer.R4_PENROSE_SPHERE_LASER,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4_penrose_sphere_laser"))
        );
        event.register(
            CFARenderer.R4_PENROSE_SPHERE_LASER_OFF,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4_penrose_sphere_laser_off"))
        );
        event.register(
            CFARenderer.R4_MATTER_DECOMPRESSOR,
            SimpleUnbakedStandaloneModel.blockStateModel(
                AnvilCraft.of("block/celestial_forging_anvil_ring_4_matter_decompressor")
            )
        );
        event.register(
            CFARenderer.R4_MATTER_DECOMPRESSOR_FIX,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4_matter_decompressor_fix"))
        );
        event.register(
            CFARenderer.R4_MATTER_DECOMPRESSOR_RING,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4_matter_decompressor_ring"))
        );
        event.register(
            CFARenderer.R4_WORMHOLE_STABILIZER,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_4_wormhole_stabilizer"))
        );
        event.register(
            CFARenderer.R5_ACCELERATOR,
            SimpleUnbakedStandaloneModel.blockStateModel(
                AnvilCraft.of("block/celestial_forging_anvil_ring_5_stellar_evolution_accelerator")
            )
        );
        event.register(
            CFARenderer.R6_ACCELERATOR,
            SimpleUnbakedStandaloneModel.blockStateModel(
                AnvilCraft.of("block/celestial_forging_anvil_ring_6_stellar_evolution_accelerator")
            )
        );
        // 锻星砧天体模型。
        event.register(
            CFARenderer.BODY_STAR,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/star"))
        );
        event.register(
            CFARenderer.BODY_NEUTRON_STAR,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/neutron_star"))
        );
        event.register(
            CFARenderer.BODY_NEUTRON_STAR_JET,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/neutron_star_jet"))
        );
        event.register(
            CFARenderer.BODY_BLACK_HOLE,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/black_hole"))
        );
        // 行星模型，作为动态贴图不可用时的静态贴图后备。
        event.register(CFARenderer.BODY_PLANET_ARID,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_arid")));
        event.register(CFARenderer.BODY_PLANET_WET,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_wet")));
        event.register(CFARenderer.BODY_PLANET_BOGGY,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_boggy")));
        event.register(CFARenderer.BODY_PLANET_OCEANIC,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_oceanic")));
        event.register(CFARenderer.BODY_PLANET_ATMOSPHERELESS,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_atmosphereless")));
        event.register(CFARenderer.BODY_PLANET_GIANT,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_giant_0")));
        event.register(CFARenderer.BODY_PLANET_OVERWORLD,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_overworld")));
        event.register(CFARenderer.BODY_PLANET_FLESH,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_flesh")));
        event.register(CFARenderer.BODY_PLANET_INTELLIGENCE,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_intelligence")));
        event.register(CFARenderer.BODY_PLANET_SHATTERED,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_shattered")));
        event.register(CFARenderer.BODY_PLANET_HOLLOW,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_hollow")));
        event.register(CFARenderer.BODY_PLANET_ERROR,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_body/planet_error")));
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
        event.register(
            PipeCheckValveBERenderer.ARM,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/check_valve_arm"))
        );
        event.register(
            ControlValveBlockEntityRenderer.HANDWHEEL,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/control_valve_handwheel"))
        );
        // WIP block models for procedural process
        event.register(
            WipBlockEntityRenderer.REDSTONE_COMPUTER_WIP,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/redstone_computer_wip"))
        );
        event.register(
            WipBlockEntityRenderer.SPACETIME_SUPERCOMPUTER_WIP,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/spacetime_supercomputer_wip"))
        );
        event.register(
            WipBlockEntityRenderer.ANCIENT_DEBRIS_WIP,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/ancient_debris_wip"))
        );
        event.register(
            WipBlockEntityRenderer.NETHERITE_BLOCK_WIP,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/netherite_block_wip"))
        );
        event.register(
            WipBlockEntityRenderer.HEAVY_IRON_BLOCK_WIP,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/heavy_iron_block_wip"))
        );
        event.register(
            WipBlockEntityRenderer.ANCIENT_SEA_REEF_WIP,
            SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/ancient_sea_reef_wip"))
        );
    }

    @SubscribeEvent
    public static void registerSpecialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(AnvilCraft.of("spectral_slingshot"), SpectralSlingshotRenderer.Unbaked.CODEC);
        event.register(AnvilCraft.of("spectral_weapon_launcher"), SpectralWeaponLauncherRenderer.Unbaked.CODEC);
    }

    /** 注册锻星砧界面使用的画中画渲染器。 */
    @SubscribeEvent
    public static void registerPictureInPictureRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(CfaPreviewPipRenderer.State.class, CfaPreviewPipRenderer::new);
    }
}

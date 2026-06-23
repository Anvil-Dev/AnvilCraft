package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CelestialForgingAnvilBlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@SuppressWarnings("Linelength")
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
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/infinite_collector_head")));
        event.register(CelestialForgingAnvilBlockEntityRenderer.R1);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R2);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R3);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R4);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R5);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R6);

        // Celestial Restriction Ring megastructure models - Ring 1
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_1_eco_station")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_1_excavator")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_1_excavator_off")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_1_exctractor")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_1_temple")));
        // Ring 2
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_2_exctractor")));
        // Ring 4
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_coil")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_coil_fix")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_coil_ring")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_collider")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_dyson_sphere")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_matter_decompressor")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_matter_decompressor_fix")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_matter_decompressor_ring")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_penrose_sphere")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_penrose_sphere_fix")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_penrose_sphere_laser")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_penrose_sphere_laser_off")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_4_wormhole_stabilizer")));
        // Ring 5
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_5_dyson_sphere")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_5_stellar_evolution_accelerator")));
        // Ring 6
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_6_stellar_evolution_accelerator")));

        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/fire_cauldron_fire4")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/smart_block_placer_base")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/smart_block_placer_upperarm")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/smart_block_placer_forearm")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/smart_block_placer_claw")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/smart_block_placer_claw_open")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/pump_piston_1")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/pump_piston_2")));

        // Special celestial body models
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/planet_overworld")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/planet_flesh")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/planet_intelligence")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/planet_shattered")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/planet_hollow")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/planet_error")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/star")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/neutron_star")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/neutron_star_jet")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/black_hole")));

        // Celestial Forging Anvil Portal models
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_gate")));
        event.register(ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_gate_open")));
    }
}

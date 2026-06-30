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
        event.register(standaloneItem("crab_claw_holding_block"));
        event.register(standaloneItem("crab_claw_holding_item"));
        event.register(standaloneBlock("heliostats_head"));
        event.register(standaloneBlock("heliostats_head_sunflower"));
        event.register(standaloneBlock("creative_generator_head"));
        event.register(standaloneBlock("charge_collector_head"));
        event.register(standaloneBlock("fe_collector_head"));
        event.register(standaloneBlock("heat_collector_head"));
        event.register(standaloneBlock("void_energy_collector_head"));
        event.register(standaloneBlock("laser"));
        event.register(standaloneBlock("axis"));
        event.register(standaloneBlock("advanced_comparator_indicator"));
        event.register(standaloneBlock("infinite_collector_head"));
        event.register(CelestialForgingAnvilBlockEntityRenderer.R1);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R2);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R3);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R4);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R5);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R6);

        // Celestial Restriction Ring megastructure models - Ring 1
        event.register(standaloneBlock("celestial_forging_anvil_ring_1_eco_station"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_1_excavator"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_1_excavator_off"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_1_exctractor"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_1_temple"));
        // Ring 2
        event.register(standaloneBlock("celestial_forging_anvil_ring_2_exctractor"));
        // Ring 4
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_coil"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_coil_fix"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_coil_ring"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_collider"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_dyson_sphere"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_matter_decompressor"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_matter_decompressor_fix"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_matter_decompressor_ring"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_penrose_sphere"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_penrose_sphere_fix"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_penrose_sphere_laser"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_penrose_sphere_laser_off"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_wormhole_stabilizer"));
        // Ring 5
        event.register(standaloneBlock("celestial_forging_anvil_ring_5_dyson_sphere"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_5_stellar_evolution_accelerator"));
        // Ring 6
        event.register(standaloneBlock("celestial_forging_anvil_ring_6_stellar_evolution_accelerator"));

        event.register(standaloneBlock("fire_cauldron_fire4"));
        event.register(standaloneBlock("smart_block_placer_base"));
        event.register(standaloneBlock("smart_block_placer_upperarm"));
        event.register(standaloneBlock("smart_block_placer_forearm"));
        event.register(standaloneBlock("smart_block_placer_claw"));
        event.register(standaloneBlock("smart_block_placer_claw_open"));
        event.register(standaloneBlock("pump_piston_1"));
        event.register(standaloneBlock("pump_piston_2"));

        // Special celestial body models
        event.register(standaloneBlock("celestial_body/planet_overworld"));
        event.register(standaloneBlock("celestial_body/planet_flesh"));
        event.register(standaloneBlock("celestial_body/planet_intelligence"));
        event.register(standaloneBlock("celestial_body/planet_shattered"));
        event.register(standaloneBlock("celestial_body/planet_hollow"));
        event.register(standaloneBlock("celestial_body/planet_error"));
        event.register(standaloneBlock("celestial_body/star"));
        event.register(standaloneBlock("celestial_body/neutron_star"));
        event.register(standaloneBlock("celestial_body/neutron_star_jet"));
        event.register(standaloneBlock("celestial_body/black_hole"));

        // Celestial Forging Anvil Portal models
        event.register(standaloneBlock("celestial_forging_anvil_gate"));
        event.register(standaloneBlock("celestial_forging_anvil_gate_open"));

        // WIP models
        event.register(standaloneBlock("ancient_sea_reef_wip"));
        event.register(standaloneBlock("ancient_debris_wip"));
        event.register(standaloneBlock("netherite_block_wip"));
        event.register(standaloneBlock("heavy_iron_block_wip"));
        event.register(standaloneBlock("redstone_computer_wip"));
        event.register(standaloneBlock("spacetime_supercomputer_wip"));
    }

    private static ModelResourceLocation standaloneBlock(String path) {
        return ModelResourceLocation.standalone(AnvilCraft.of("block/" + path));
    }

    private static ModelResourceLocation standaloneItem(String path) {
        return ModelResourceLocation.standalone(AnvilCraft.of("item/" + path));
    }
}

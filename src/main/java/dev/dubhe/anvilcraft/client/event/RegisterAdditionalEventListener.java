package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CelestialForgingAnvilBlockEntityRenderer;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.HashSet;
import java.util.Set;

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
        event.register(standaloneBlock("processing_table_crushing_wheel_left"));
        event.register(standaloneBlock("processing_table_crushing_wheel_right"));
        event.register(standaloneBlock("pulse_generator_indicator"));
        event.register(standaloneBlock("pulse_generator_indicator_overspeed"));
        event.register(standaloneBlock("infinite_collector_head"));
        event.register(CelestialForgingAnvilBlockEntityRenderer.R1);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R2);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R3);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R4);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R5);
        event.register(CelestialForgingAnvilBlockEntityRenderer.R6);

        Set<ResourceLocation> registeredMegastructureModels = new HashSet<>();
        for (var megastructure : ModRegistries.MEGASTRUCTURE) {
            for (ResourceLocation modelLocation : megastructure.modelLocations().values()) {
                if (registeredMegastructureModels.add(modelLocation)) {
                    event.register(ModelResourceLocation.standalone(modelLocation));
                }
            }
        }

        // Keep built-ins available if this event fires before the custom registry is populated.
        event.register(standaloneBlock("celestial_forging_anvil_ring_1_eco_station"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_1_excavator"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_1_exctractor"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_1_temple"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_2_exctractor"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_2_dyson_sphere"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_coil"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_collider"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_dyson_sphere"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_matter_decompressor"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_penrose_sphere"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_wormhole_stabilizer"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_5_dyson_sphere"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_5_stellar_evolution_accelerator"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_6_stellar_evolution_accelerator"));

        // Built-in state-dependent and split-layer models are not part of the primary definition.
        event.register(standaloneBlock("celestial_forging_anvil_ring_1_excavator_off"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_coil_fix"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_coil_ring"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_matter_decompressor_fix"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_matter_decompressor_ring"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_penrose_sphere_fix"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_penrose_sphere_laser"));
        event.register(standaloneBlock("celestial_forging_anvil_ring_4_penrose_sphere_laser_off"));

        event.register(standaloneBlock("fire_cauldron_fire4"));
        event.register(standaloneBlock("smart_block_placer_base"));
        event.register(standaloneBlock("smart_block_placer_upperarm"));
        event.register(standaloneBlock("smart_block_placer_forearm"));
        event.register(standaloneBlock("smart_block_placer_claw"));
        event.register(standaloneBlock("smart_block_placer_claw_open"));
        event.register(standaloneBlock("pump_piston_1"));
        event.register(standaloneBlock("pump_piston_2"));
        event.register(standaloneBlock("control_valve_handwheel"));
        event.register(standaloneBlock("check_valve_arm"));

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
        registerCelestialBodyModels(event);

        // WIP models
        registerWipDisplayModels(event);

        event.register(standaloneBlock("hypercube"));
    }

    private static ModelResourceLocation standaloneBlock(String path) {
        return ModelResourceLocation.standalone(AnvilCraft.of("block/" + path));
    }

    private static ModelResourceLocation standaloneItem(String path) {
        return ModelResourceLocation.standalone(AnvilCraft.of("item/" + path));
    }

    /**
     * 自动注册资源包中提供的天体模型。
     */
    private static void registerCelestialBodyModels(ModelEvent.RegisterAdditional event) {
        registerModelsWithPathPrefix(event, "block/celestial_body");
    }

    /**
     * 自动注册资源包中提供的方块序列加工的中间态展示模型
     */
    private static void registerWipDisplayModels(ModelEvent.RegisterAdditional event) {
        registerModelsWithPathPrefix(event, "block/wip_display");
    }

    private static void registerModelsWithPathPrefix(ModelEvent.RegisterAdditional event, String prefix) {
        Minecraft.getInstance()
            .getResourceManager()
            .listResources("models/" + prefix, location -> location.getPath().endsWith(".json"))
            .keySet()
            .stream()
            .map(RegisterAdditionalEventListener::toStandaloneModel)
            .forEach(event::register);
    }

    private static ModelResourceLocation toStandaloneModel(ResourceLocation resourceLocation) {
        String resourcePath = resourceLocation.getPath();
        String modelPath = resourcePath.substring("models/".length(), resourcePath.length() - ".json".length());
        ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(
            resourceLocation.getNamespace(), modelPath
        );
        return ModelResourceLocation.standalone(modelLocation);
    }
}

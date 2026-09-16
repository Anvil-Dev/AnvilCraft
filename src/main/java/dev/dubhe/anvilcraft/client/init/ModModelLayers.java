package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.entity.model.CauldronOutletModel;
import dev.dubhe.anvilcraft.client.renderer.entity.model.EquipmentModels;
import dev.dubhe.anvilcraft.entity.model.IonocraftModel;
import dev.dubhe.anvilcraft.entity.model.MagnetizedNodeModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class ModModelLayers {
    public static final ModelLayerLocation IONOCRAFT = new ModelLayerLocation(AnvilCraft.of("ionocraft"), "main");
    public static final ModelLayerLocation MAGNETIZED_NODE = new ModelLayerLocation(AnvilCraft.of("magnetized_node"), "main");
    public static final ModelLayerLocation CAULDRON_OUTLET = CauldronOutletModel.LAYER_LOCATION;
    public static final ModelLayerLocation GOGGLES = new ModelLayerLocation(AnvilCraft.of("goggles"), "goggles");

    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
        EquipmentModels.register(event);
        event.registerLayerDefinition(
            IONOCRAFT,
            IonocraftModel::createBodyLayer
        );
        event.registerLayerDefinition(
            MAGNETIZED_NODE,
            MagnetizedNodeModel::createBodyLayer
        );
        event.registerLayerDefinition(
            CAULDRON_OUTLET,
            CauldronOutletModel::createBodyLayer
        );
        event.registerLayerDefinition(
            GOGGLES,
            () -> LayerDefinition.create(gogglesMesh(), 1, 1)
        );
    }

    public static void createModel(EntityRenderersEvent.AddLayers event) {
        EquipmentModels.bake(event);
    }

    private static MeshDefinition gogglesMesh() {
        CubeListBuilder builder = new CubeListBuilder();
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        mesh.getRoot().addOrReplaceChild("head", builder, PartPose.ZERO);
        return mesh;
    }
}

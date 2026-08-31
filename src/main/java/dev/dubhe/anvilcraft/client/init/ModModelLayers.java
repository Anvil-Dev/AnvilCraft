package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.entity.model.CauldronOutletModel;
import dev.dubhe.anvilcraft.entity.model.IonocraftBackpackModel;
import dev.dubhe.anvilcraft.entity.model.IonocraftModel;
import dev.dubhe.anvilcraft.entity.model.MagnetizedNodeModel;
import lombok.Getter;
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
    public static final ModelLayerLocation IONOCRAFT_BACKPACK = new ModelLayerLocation(AnvilCraft.of("ionocraft_backpack"), "main");
    public static final ModelLayerLocation MAGNETIZED_NODE = new ModelLayerLocation(AnvilCraft.of("magnetized_node"), "main");
    public static final ModelLayerLocation CAULDRON_OUTLET = CauldronOutletModel.LAYER_LOCATION;
    public static final ModelLayerLocation GOGGLES = new ModelLayerLocation(AnvilCraft.of("goggles"), "goggles");

    @Getter
    private static IonocraftBackpackModel ionocraftBackpackModel;

    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
            IONOCRAFT,
            IonocraftModel::createBodyLayer
        );
        event.registerLayerDefinition(
            IONOCRAFT_BACKPACK,
            IonocraftBackpackModel::createBodyLayer
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
        ionocraftBackpackModel = new IonocraftBackpackModel(event.getContext().bakeLayer(IONOCRAFT_BACKPACK));
    }

    private static MeshDefinition gogglesMesh() {
        CubeListBuilder builder = new CubeListBuilder();
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        mesh.getRoot().addOrReplaceChild("head", builder, PartPose.ZERO);
        return mesh;
    }
}

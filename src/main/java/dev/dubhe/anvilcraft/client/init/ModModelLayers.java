package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.entity.model.IonocraftBackpackModel;
import dev.dubhe.anvilcraft.entity.model.IonocraftModel;
import dev.dubhe.anvilcraft.entity.model.MagnetizedNodeModel;
import dev.dubhe.anvilcraft.entity.model.ThrownHeavyHalberdModel;
import lombok.Getter;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class ModModelLayers {
    public static final ModelLayerLocation IONOCRAFT = new ModelLayerLocation(AnvilCraft.of("ionocraft"), "main");
    public static final ModelLayerLocation IONOCRAFT_BACKPACK = new ModelLayerLocation(AnvilCraft.of("ionocraft_backpack"), "main");
    public static final ModelLayerLocation THROWN_HEAVY_HALBERD = new ModelLayerLocation(AnvilCraft.of("thrown_heavy_halberd"), "main");
    public static final ModelLayerLocation MAGNETIZED_NODE = new ModelLayerLocation(AnvilCraft.of("magnetized_node"), "main");

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
            THROWN_HEAVY_HALBERD,
            ThrownHeavyHalberdModel::createBodyLayer
        );
        event.registerLayerDefinition(
            MAGNETIZED_NODE,
            MagnetizedNodeModel::createBodyLayer
        );
    }

    public static void createModel(EntityRenderersEvent.AddLayers event) {
        ionocraftBackpackModel = new IonocraftBackpackModel(event.getContext().bakeLayer(IONOCRAFT_BACKPACK));
    }
}

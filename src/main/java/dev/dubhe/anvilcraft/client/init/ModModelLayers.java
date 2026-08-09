package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.entity.model.CauldronOutletModel;
import dev.dubhe.anvilcraft.client.renderer.entity.model.IonocraftBackpackModel;
import dev.dubhe.anvilcraft.client.renderer.entity.model.IonocraftModel;
import dev.dubhe.anvilcraft.client.renderer.entity.model.MagnetizedNodeModel;
import dev.dubhe.anvilcraft.client.renderer.entity.model.ThrownHeavyHalberdModel;
import lombok.Getter;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ModModelLayers {
    public static final ModelLayerLocation IONOCRAFT = new ModelLayerLocation(AnvilCraft.of("ionocraft"), "main");
    public static final ModelLayerLocation IONOCRAFT_BACKPACK = new ModelLayerLocation(AnvilCraft.of("ionocraft_backpack"), "main");
    public static final ModelLayerLocation THROWN_HEAVY_HALBERD = new ModelLayerLocation(AnvilCraft.of("thrown_heavy_halberd"), "main");
    public static final ModelLayerLocation MAGNETIZED_NODE = new ModelLayerLocation(AnvilCraft.of("magnetized_node"), "main");
    public static final ModelLayerLocation CAULDRON_OUTLET = CauldronOutletModel.LAYER_LOCATION;

    @Getter
    @Nullable
    private static IonocraftBackpackModel ionocraftBackpackModel;

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
            ModModelLayers.IONOCRAFT,
            IonocraftModel::createBodyLayer
        );
        event.registerLayerDefinition(
            ModModelLayers.IONOCRAFT_BACKPACK,
            IonocraftBackpackModel::createBodyLayer
        );
        event.registerLayerDefinition(
            ModModelLayers.THROWN_HEAVY_HALBERD,
            ThrownHeavyHalberdModel::createBodyLayer
        );
        event.registerLayerDefinition(
            ModModelLayers.MAGNETIZED_NODE,
            MagnetizedNodeModel::createBodyLayer
        );
        event.registerLayerDefinition(
            ModModelLayers.CAULDRON_OUTLET,
            CauldronOutletModel::createBodyLayer
        );
    }

    @SubscribeEvent
    public static void createModel(EntityRenderersEvent.AddLayers event) {
        ModModelLayers.ionocraftBackpackModel = new IonocraftBackpackModel(event.getContext().bakeLayer(ModModelLayers.IONOCRAFT_BACKPACK));
    }
}

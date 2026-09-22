package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.entity.model.CauldronOutletModel;
import dev.dubhe.anvilcraft.client.renderer.entity.model.EquipmentBootsModel;
import dev.dubhe.anvilcraft.client.renderer.entity.model.EquipmentHelmetModel;
import dev.dubhe.anvilcraft.client.renderer.entity.model.IonocraftBackpackModel;
import dev.dubhe.anvilcraft.client.renderer.entity.model.IonocraftModel;
import dev.dubhe.anvilcraft.client.renderer.entity.model.MagnetizedNodeModel;
import dev.dubhe.anvilcraft.client.renderer.entity.model.ThrownHeavyHalberdModel;
import lombok.Getter;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ModModelLayers {
    public static final ModelLayerLocation IONOCRAFT = new ModelLayerLocation(AnvilCraft.of("ionocraft"), "main");
    public static final ModelLayerLocation IONOCRAFT_BACKPACK = new ModelLayerLocation(AnvilCraft.of("ionocraft_backpack"), "main");
    public static final ModelLayerLocation THROWN_HEAVY_HALBERD = new ModelLayerLocation(AnvilCraft.of("thrown_heavy_halberd"), "main");
    public static final ModelLayerLocation MAGNETIZED_NODE = new ModelLayerLocation(AnvilCraft.of("magnetized_node"), "main");
    public static final ModelLayerLocation CAULDRON_OUTLET = CauldronOutletModel.LAYER_LOCATION;

    public static final ModelLayerLocation EQUIPMENT_HELMET = new ModelLayerLocation(AnvilCraft.of("equipment_helmet"), "armor");
    public static final ModelLayerLocation EQUIPMENT_BOOTS = new ModelLayerLocation(AnvilCraft.of("equipment_boots"), "armor");
    private static final Map<Model<?>, EquipmentBootsModel> EQUIPMENT_BOOTS_MODELS = new IdentityHashMap<>();
    private static final Map<Model<?>, EquipmentHelmetModel> EQUIPMENT_HELMETS = new IdentityHashMap<>();
    private static @Nullable EntityModelSet equipmentModels;

    public static EquipmentHelmetModel getEquipmentHelmetModel(Model<?> original) {
        return EQUIPMENT_HELMETS.computeIfAbsent(original, model -> {
            var replacement = new EquipmentHelmetModel(Objects.requireNonNull(equipmentModels).bakeLayer(EQUIPMENT_HELMET));
            replacement.root().setInitialPose(model.root().getInitialPose());
            if (model instanceof HumanoidModel<?> humanoid) replacement.head.setInitialPose(humanoid.head.getInitialPose());
            return replacement;
        });
    }

    public static EquipmentBootsModel getEquipmentBootsModel(Model<?> original) {
        return EQUIPMENT_BOOTS_MODELS.computeIfAbsent(original, model -> {
            var replacement = new EquipmentBootsModel(Objects.requireNonNull(equipmentModels).bakeLayer(EQUIPMENT_BOOTS));
            replacement.root().setInitialPose(model.root().getInitialPose());
            if (model instanceof HumanoidModel<?> humanoid) {
                replacement.rightLeg.setInitialPose(humanoid.rightLeg.getInitialPose());
                replacement.leftLeg.setInitialPose(humanoid.leftLeg.getInitialPose());
            }
            return replacement;
        });
    }

    @Getter
    @Nullable
    private static IonocraftBackpackModel ionocraftBackpackModel;

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EQUIPMENT_BOOTS, EquipmentBootsModel::createBodyLayer);
        event.registerLayerDefinition(EQUIPMENT_HELMET, EquipmentHelmetModel::createBodyLayer);
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
        EQUIPMENT_HELMETS.clear();
        EQUIPMENT_BOOTS_MODELS.clear();
        equipmentModels = event.getContext().getModelSet();
        ModModelLayers.ionocraftBackpackModel = new IonocraftBackpackModel(event.getContext().bakeLayer(ModModelLayers.IONOCRAFT_BACKPACK));
    }
}

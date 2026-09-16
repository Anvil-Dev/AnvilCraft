package dev.dubhe.anvilcraft.client.renderer.entity.model;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** 从 models/equipment 下的 Blockbench 模型导出的装备层。 */
public final class EquipmentModels {
    private static final Map<String, HumanoidModel<LivingEntity>> MODELS = new HashMap<>();

    private EquipmentModels() {
    }

    public static HumanoidModel<?> get(ItemStack stack, HumanoidModel<?> original) {
        @Nullable HumanoidModel<?> model = MODELS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        return model == null ? original : model;
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(AnvilCraft.of(name), "armor");
    }

    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(layer("breathing_helmet"), EquipmentModels::breathingHelmet);
        event.registerLayerDefinition(layer("ionocraft_backpack"), EquipmentModels::ionocraftBackpack);
        event.registerLayerDefinition(layer("pockets_leggings"), EquipmentModels::pocketsLeggings);
        event.registerLayerDefinition(layer("buffer_boots"), EquipmentModels::bufferBoots);
        event.registerLayerDefinition(layer("weatherproof_spacesuit_helmet"), EquipmentModels::breathingHelmet);
        event.registerLayerDefinition(layer("weatherproof_spacesuit_chestplate"), EquipmentModels::ionocraftBackpack);
        event.registerLayerDefinition(layer("weatherproof_spacesuit_leggings"), EquipmentModels::pocketsLeggings);
        event.registerLayerDefinition(layer("weatherproof_spacesuit_boots"), EquipmentModels::bufferBoots);
    }

    public static void bake(EntityRenderersEvent.AddLayers event) {
        MODELS.put("breathing_helmet",
            new HumanoidModel<>(event.getContext().bakeLayer(layer("breathing_helmet"))));
        MODELS.put("ionocraft_backpack",
            new HumanoidModel<>(event.getContext().bakeLayer(layer("ionocraft_backpack"))));
        MODELS.put("pockets_leggings",
            new HumanoidModel<>(event.getContext().bakeLayer(layer("pockets_leggings"))));
        MODELS.put("buffer_boots",
            new HumanoidModel<>(event.getContext().bakeLayer(layer("buffer_boots"))));
        MODELS.put("weatherproof_spacesuit_helmet",
            new HumanoidModel<>(event.getContext().bakeLayer(layer("weatherproof_spacesuit_helmet"))));
        MODELS.put("weatherproof_spacesuit_chestplate",
            new HumanoidModel<>(event.getContext().bakeLayer(layer("weatherproof_spacesuit_chestplate"))));
        MODELS.put("weatherproof_spacesuit_leggings",
            new HumanoidModel<>(event.getContext().bakeLayer(layer("weatherproof_spacesuit_leggings"))));
        MODELS.put("weatherproof_spacesuit_boots",
            new HumanoidModel<>(event.getContext().bakeLayer(layer("weatherproof_spacesuit_boots"))));
    }

    private static LayerDefinition breathingHelmet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition part1 = root.addOrReplaceChild("head", CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.10472F, 0.087266F, 0.0F));
        part1.addOrReplaceChild("cube2", CubeListBuilder.create().texOffs(0, 0)
            .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        part1.addOrReplaceChild("cube3", CubeListBuilder.create().texOffs(32, 0)
            .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.5F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition ionocraftBackpack() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition part1 = root.addOrReplaceChild("body", CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        part1.addOrReplaceChild("cube2", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(1.01F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        part1.addOrReplaceChild("cube3", CubeListBuilder.create().texOffs(48, 56)
            .addBox(-3.0F, -3.0F, -1.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 8.0F, 4.0F, -0.0F, -0.0F, -0.785398F));
        part1.addOrReplaceChild("cube4", CubeListBuilder.create().texOffs(48, 56)
            .addBox(-3.0F, -3.0F, -1.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 8.0F, -4.0F, -0.0F, -0.0F, -0.785398F));
        PartDefinition part5 = part1.addOrReplaceChild("part5", CubeListBuilder.create(),
            PartPose.offsetAndRotation(-5.0F, 3.0F, 7.0F, -0.0F, -0.0F, 0.0F));
        part5.addOrReplaceChild("cube6", CubeListBuilder.create().texOffs(32, 25)
            .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, -0.217701F, 0.042619F));
        part5.addOrReplaceChild("cube7", CubeListBuilder.create().texOffs(27, 40)
            .addBox(-4.0F, -1.0F, 4.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, -0.217701F, 0.042619F));
        part5.addOrReplaceChild("cube8", CubeListBuilder.create().texOffs(0, 40)
            .addBox(-11.0F, -1.0F, -4.0F, 7.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, -0.217701F, 0.042619F));
        part5.addOrReplaceChild("cube9", CubeListBuilder.create().texOffs(48, 42)
            .addBox(-13.0F, -1.0F, -4.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, -0.217701F, 0.042619F));
        part5.addOrReplaceChild("cube10", CubeListBuilder.create().texOffs(10, 32)
            .addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.040553F, -1.002779F, 0.071983F));
        part5.addOrReplaceChild("cube11", CubeListBuilder.create().texOffs(10, 32)
            .addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.115726F, -0.567493F, -3.117705F));
        PartDefinition part12 = part1.addOrReplaceChild("part12", CubeListBuilder.create(),
            PartPose.offsetAndRotation(5.0F, 3.0F, 7.0F, -0.0F, -0.0F, 0.0F));
        part12.addOrReplaceChild("cube13", CubeListBuilder.create().texOffs(10, 32).mirror()
            .addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.115726F, 0.567493F, 3.117705F));
        part12.addOrReplaceChild("cube14", CubeListBuilder.create().texOffs(10, 32).mirror()
            .addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.040553F, 1.002779F, -0.071983F));
        part12.addOrReplaceChild("cube15", CubeListBuilder.create().texOffs(48, 42).mirror()
            .addBox(11.0F, -1.0F, -4.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, 0.217701F, -0.042619F));
        part12.addOrReplaceChild("cube16", CubeListBuilder.create().texOffs(32, 25).mirror()
            .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, 0.217701F, -0.042619F));
        part12.addOrReplaceChild("cube17", CubeListBuilder.create().texOffs(27, 40).mirror()
            .addBox(0.0F, -1.0F, 4.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, 0.217701F, -0.042619F));
        part12.addOrReplaceChild("cube18", CubeListBuilder.create().texOffs(0, 40).mirror()
            .addBox(4.0F, -1.0F, -4.0F, 7.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, 0.217701F, -0.042619F));
        PartDefinition part19 = root.addOrReplaceChild("right_arm", CubeListBuilder.create(),
            PartPose.offsetAndRotation(-5.0F, 2.0F, 0.0F, -0.174533F, -0.0F, 0.0F));
        part19.addOrReplaceChild("cube20", CubeListBuilder.create().texOffs(40, 16)
            .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(1.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        PartDefinition part21 = root.addOrReplaceChild("left_arm", CubeListBuilder.create(),
            PartPose.offsetAndRotation(5.0F, 2.0F, 0.0F, 0.20944F, -0.0F, 0.0F));
        part21.addOrReplaceChild("cube22", CubeListBuilder.create().texOffs(40, 16).mirror()
            .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(1.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition pocketsLeggings() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition part1 = root.addOrReplaceChild("body", CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        part1.addOrReplaceChild("cube2", CubeListBuilder.create().texOffs(16, 55)
            .addBox(-4.0F, 7.0F, -2.0F, 8.0F, 5.0F, 4.0F, new CubeDeformation(0.51F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        PartDefinition part3 = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
            PartPose.offsetAndRotation(-1.9F, 12.0F, 0.0F, 0.191986F, -0.0F, 0.034907F));
        part3.addOrReplaceChild("cube4", CubeListBuilder.create().texOffs(0, 50)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.5F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        part3.addOrReplaceChild("cube5", CubeListBuilder.create().texOffs(16, 48)
            .addBox(-2.0F, 3.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.75F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        PartDefinition part6 = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
            PartPose.offsetAndRotation(1.9F, 12.0F, 0.0F, -0.174533F, -0.0F, -0.034907F));
        part6.addOrReplaceChild("cube7", CubeListBuilder.create().texOffs(0, 50).mirror()
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.5F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        part6.addOrReplaceChild("cube8", CubeListBuilder.create().texOffs(16, 48).mirror()
            .addBox(-2.0F, 3.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.75F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition bufferBoots() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition part1 = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
            PartPose.offsetAndRotation(-1.9F, 12.0F, 0.0F, 0.191986F, -0.0F, 0.034907F));
        part1.addOrReplaceChild("cube2", CubeListBuilder.create().texOffs(0, 16)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        PartDefinition part3 = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
            PartPose.offsetAndRotation(1.9F, 12.0F, 0.0F, -0.174533F, -0.0F, -0.034907F));
        part3.addOrReplaceChild("cube4", CubeListBuilder.create().texOffs(0, 16).mirror()
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

}

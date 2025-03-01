package dev.dubhe.anvilcraft.integration.curios.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class GogglesCurioRenderer implements ICurioRenderer {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(AnvilCraft.of("goggles"), "goggles");

    private final HumanoidModel<LivingEntity> model;

    public GogglesCurioRenderer(ModelPart part) {
        this.model = new HumanoidModel<>(part);
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
        ItemStack stack,
        @NotNull SlotContext slotContext,
        @NotNull PoseStack matrixStack,
        RenderLayerParent<T, M> renderLayerParent,
        MultiBufferSource renderTypeBuffer,
        int light,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        // Prepare values for transformation
        model.setupAnim(slotContext.entity(), limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        model.prepareMobModel(slotContext.entity(), limbSwing, limbSwingAmount, partialTicks);
        ICurioRenderer.followHeadRotations(slotContext.entity(), model.head);

        // Translate and rotate with our head
        matrixStack.pushPose();
        matrixStack.translate(model.head.x / 16.0, model.head.y / 16.0, model.head.z / 16.0);
        matrixStack.mulPose(Axis.YP.rotation(model.head.yRot));
        matrixStack.mulPose(Axis.XP.rotation(model.head.xRot));

        // Translate and scale to our head
        matrixStack.translate(0, -0.25, 0);
        matrixStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        matrixStack.scale(0.625f, 0.625f, 0.625f);

        if (!slotContext.entity().getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            matrixStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            matrixStack.translate(0, -0.25, 0);
        }

        // Render
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer()
            .renderStatic(stack, ItemDisplayContext.HEAD, light, OverlayTexture.NO_OVERLAY, matrixStack,
                renderTypeBuffer, mc.level, 0);
        matrixStack.popPose();
    }

    public static @NotNull MeshDefinition mesh() {
        CubeListBuilder builder = new CubeListBuilder();
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        mesh.getRoot().addOrReplaceChild("head", builder, PartPose.ZERO);
        return mesh;
    }
}

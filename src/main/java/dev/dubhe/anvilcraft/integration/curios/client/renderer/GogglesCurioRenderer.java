package dev.dubhe.anvilcraft.integration.curios.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class GogglesCurioRenderer implements ICurioRenderer {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(AnvilCraft.of("goggles"), "goggles");

    private final HumanoidModel<HumanoidRenderState> model;

    public GogglesCurioRenderer() {
        this.model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(GogglesCurioRenderer.LAYER));
    }

    public static MeshDefinition mesh() {
        CubeListBuilder builder = new CubeListBuilder();
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        mesh.getRoot().addOrReplaceChild("head", builder, PartPose.ZERO);
        return mesh;
    }

    @Override
    public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(
        ItemStack stack,
        SlotContext slotContext,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int packedLight,
        S renderState,
        RenderLayerParent<S, M> renderLayerParent,
        EntityRendererProvider.Context context,
        float rotationY,
        float rotationX
    ) {
        // Prepare values for transformation
        ICurioRenderer.setupHumanoidAnimations(this.model, renderState);

        // Translate and rotate with our head
        poseStack.pushPose();
        poseStack.translate(this.model.head.x / 16.0, this.model.head.y / 16.0, this.model.head.z / 16.0);
        poseStack.mulPose(Axis.YP.rotation(this.model.head.yRot));
        poseStack.mulPose(Axis.XP.rotation(this.model.head.xRot));

        // Translate and scale to our head
        poseStack.translate(0, -0.25, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        poseStack.scale(0.625f, 0.625f, 0.625f);

        if (!slotContext.entity().getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            poseStack.translate(0, -0.25, 0);
        }

        // Render
        // TODO: This works for now. But maybe there is a better way to render this?
        Minecraft mc = Minecraft.getInstance();
        ItemStackRenderState itemStackRenderState = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(itemStackRenderState, stack, ItemDisplayContext.HEAD, null, null, 0);
        itemStackRenderState.submit(poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }
}

package dev.dubhe.anvilcraft.integration.curios.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.entity.model.IonocraftBackpackModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import static dev.dubhe.anvilcraft.item.IonocraftBackpackItem.TEXTURE;
import static dev.dubhe.anvilcraft.item.IonocraftBackpackItem.TEXTURE_OFF;
import static dev.dubhe.anvilcraft.item.IonocraftBackpackItem.getFlightTime;

public class IonocraftBackpackCurioRenderer implements ICurioRenderer {
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
        ItemStack stack,
        SlotContext slotContext,
        PoseStack matrixStack,
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
        IonocraftBackpackModel model = ModModelLayers.getIonocraftBackpackModel();
        ModelPart body = model.getRoot();
        VertexConsumer buffer = renderTypeBuffer.getBuffer(RenderType.entityCutoutNoCull(texture(stack)));
        matrixStack.pushPose();
        ICurioRenderer.translateIfSneaking(matrixStack, slotContext.entity());
        ICurioRenderer.followBodyRotations(slotContext.entity(), model);
        body.render(
            matrixStack,
            buffer,
            light,
            OverlayTexture.NO_OVERLAY
        );
        matrixStack.popPose();
    }

    private ResourceLocation texture(ItemStack itemStack) {
        if (getFlightTime(itemStack) > 0) {
            return TEXTURE;
        }
        return TEXTURE_OFF;
    }
}

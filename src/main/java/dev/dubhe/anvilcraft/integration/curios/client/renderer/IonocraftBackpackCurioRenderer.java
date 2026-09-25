package dev.dubhe.anvilcraft.integration.curios.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.client.renderer.item.EquipmentPoweredProperty;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class IonocraftBackpackCurioRenderer implements ICurioRenderer {
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
        if (!(renderState instanceof HumanoidRenderState humanoid)) return;
        var model = ModModelLayers.getEquipmentChestModel(renderLayerParent.getModel());
        boolean powered = Boolean.TRUE.equals(renderState.getRenderData(EquipmentPoweredProperty.IN_GRID));
        var texture = ((IonoCraftBackpackItem) stack.getItem()).getArmorTexture(stack, powered);
        submitNodeCollector.submitModel(model, humanoid, poseStack, RenderTypes.armorCutoutNoCull(texture),
            packedLight, OverlayTexture.NO_OVERLAY, -1, null, renderState.outlineColor, null);
    }
}

package dev.dubhe.anvilcraft.integration.curios.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import static dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem.TEXTURE;
import static dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem.TEXTURE_OFF;
import static dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem.getFlightTime;

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
        float yRotation,
        float xRotation
    ) {
        ModelPart modelPart = ModModelLayers.getIonocraftBackpackModel().getRoot();
        submitNodeCollector.submitModelPart(
            modelPart,
            poseStack,
            RenderTypes.entityCutout(texture(stack)),
            packedLight,
            OverlayTexture.NO_OVERLAY,
            null
        );
    }

    private Identifier texture(ItemStack itemStack) {
        if (getFlightTime(itemStack) > 0) {
            return TEXTURE;
        }
        return TEXTURE_OFF;
    }
}

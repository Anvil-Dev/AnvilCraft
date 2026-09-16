package dev.dubhe.anvilcraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemLayerTransformInvoker {
    @Invoker("applyTransform")
    void anvilcraft$applyTransform(PoseStack.Pose pose);
}

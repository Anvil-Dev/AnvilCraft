package dev.dubhe.anvilcraft.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface BuildingRodItemLayerAccessor {
    @Invoker("applyTransform")
    void anvilcraft$applyTransform(PoseStack.Pose pose);
}

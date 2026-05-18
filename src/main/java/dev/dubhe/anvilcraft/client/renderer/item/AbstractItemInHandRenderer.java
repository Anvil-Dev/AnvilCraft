package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Setter;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Setter
public abstract class AbstractItemInHandRenderer {
    protected ItemStack offHandItem;
    protected ItemStack mainHandItem;
    private final IItemRenderer renderer;
    private final ItemModelResolver itemModelResolver;

    protected AbstractItemInHandRenderer(ItemModelResolver itemModelResolver, IItemRenderer renderer) {
        this.renderer = renderer;
        this.itemModelResolver = itemModelResolver;
    }

    public void renderItem(
        LivingEntity entity,
        ItemStack itemStack,
        ItemDisplayContext displayContext,
        boolean leftHand,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int seed
    ) {
        this.renderer.renderItem(entity, itemStack, displayContext, leftHand, poseStack, buffer, seed);
    }

    public abstract void render(
        AbstractClientPlayer player,
        float partialTicks,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack stack,
        float equippedProgress,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int combinedLight,
        CallbackInfo ci
    );
}

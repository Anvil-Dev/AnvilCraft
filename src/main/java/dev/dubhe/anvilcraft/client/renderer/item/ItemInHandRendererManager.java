package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.item.IExtraItemDisplay;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

public class ItemInHandRendererManager extends AbstractItemInHandRenderer {
    private final Set<AbstractItemInHandRenderer> renderers = new HashSet<>();
    public final CrabClawItemInHandRenderer crabClawItemRenderer;
    public final IExtraItemDisplayRenderer extraItemRenderer;

    public ItemInHandRendererManager(ItemRenderer itemRenderer, IItemRenderer iItemRenderer) {
        super(itemRenderer, iItemRenderer);
        this.crabClawItemRenderer = new CrabClawItemInHandRenderer(itemRenderer, iItemRenderer);
        this.renderers.add(this.crabClawItemRenderer);
        this.extraItemRenderer = new IExtraItemDisplayRenderer(itemRenderer, iItemRenderer);
        this.renderers.add(this.extraItemRenderer);
    }

    @Override
    public void setMainHandItem(ItemStack mainHandItem) {
        this.renderers.forEach(renderer -> renderer.setMainHandItem(mainHandItem));
        super.setMainHandItem(mainHandItem);
    }

    @Override
    public void setOffHandItem(ItemStack offHandItem) {
        this.renderers.forEach(renderer -> renderer.setOffHandItem(offHandItem));
        super.setOffHandItem(offHandItem);
    }

    public void render(
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
    ) {
        if (
            this.offHandItem.is(ModItems.CRAB_CLAW.get())
                && !this.mainHandItem.is(ModItems.CRAB_CLAW.get())
        ) {
            this.crabClawItemRenderer.render(
                player,
                partialTicks,
                pitch,
                hand,
                swingProgress,
                stack,
                equippedProgress,
                poseStack,
                buffer,
                combinedLight,
                ci
            );
        }
        if (stack.getItem() instanceof IExtraItemDisplay) {
            this.extraItemRenderer.render(
                player,
                partialTicks,
                pitch,
                hand,
                swingProgress,
                stack,
                equippedProgress,
                poseStack,
                buffer,
                combinedLight,
                ci
            );
        }
    }
}

package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.item.IExtraItemDisplay;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

public class ItemInHandRendererManager extends AbstractItemInHandRenderer {
    private final Set<AbstractItemInHandRenderer> renderers = new HashSet<>();
    public final CrabClawItemInHandRenderer crabClawItemRenderer;
    public final ExtraItemDisplayRenderer extraItemRenderer;

    public ItemInHandRendererManager(ItemModelResolver resolver, IItemRenderer renderer) {
        super(resolver, renderer);
        this.crabClawItemRenderer = new CrabClawItemInHandRenderer(resolver, renderer);
        this.renderers.add(this.crabClawItemRenderer);
        this.extraItemRenderer = new ExtraItemDisplayRenderer(resolver, renderer);
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

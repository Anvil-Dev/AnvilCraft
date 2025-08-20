package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.item.IExtraItemDisplay;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

public class IExtraItemDisplayRenderer extends AbstractItemInHandRenderer {
    protected IExtraItemDisplayRenderer(ItemRenderer itemRenderer, IItemRenderer iItemRenderer) {
        super(itemRenderer, iItemRenderer);
    }

    public static void renderGuiExtra(
        PoseStack pose,
        IGuiItemRenderer itemRenderer,
        LivingEntity entity,
        Level level,
        ItemStack stack,
        int x,
        int y,
        int seed,
        int guiOffset,
        int recursion,
        int maxRecursion,
        Consumer<Integer> recursionSetter
    ) {
        if (recursion >= maxRecursion) return;
        if (!(stack.getItem() instanceof IExtraItemDisplay item)) return;
        ItemStack innerStack = item.getDisplayedItem(stack);
        if (innerStack.isEmpty()) return;
        recursionSetter.accept(recursion + 1);
        pose.pushPose();
        pose.translate(x + item.xOffset(stack), y + item.yOffset(stack), 0);
        float scale = item.scale(stack);
        pose.scale(scale, scale, 1.0f);
        itemRenderer.renderItem(entity, level, innerStack, 0, 0, seed, guiOffset + 10);
        recursionSetter.accept(recursion - 1);
        pose.popPose();
    }

    @Override
    public void render(
        AbstractClientPlayer player,
        float partialTicks,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        @NotNull ItemStack stack,
        float equippedProgress,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int combinedLight,
        CallbackInfo ci
    ) {
        if (!(stack.getItem() instanceof IExtraItemDisplay display)) return;
        HumanoidArm humanoidarm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean flag = humanoidarm == HumanoidArm.LEFT;
        int i = flag ? -1 : 1;
        ItemStack displayedItem = display.getDisplayedItem(stack);
        float scale = display.scale(stack);
        int xOffset = display.xOffset(stack);
        int yOffset = display.yOffset(stack);
        poseStack.pushPose();
        poseStack.translate(i * 0.024, 0.015 * i + 0.10 + yOffset * 0.03, 0.0225 * i - 0.1425 + xOffset * 0.03);
        poseStack.scale(scale, scale, scale);
        this.renderItem(
            player,
            displayedItem,
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            flag,
            poseStack,
            buffer,
            combinedLight
        );
        poseStack.popPose();
    }
}

package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.item.IExtraItemDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Unique
    private static int ANVILCRAFT$RECURSION = 0;
    @Unique
    private static final int ANVILCRAFT$MAX_RECURSION = 3;

    @Final
    @Shadow
    private PoseStack pose;

    @Shadow
    protected abstract void renderItem(
        @Nullable LivingEntity entity, @Nullable Level level, ItemStack stack, int x, int y, int seed, int guiOffset
    );

    @Inject(
        method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
        at = @At(
            value = "RETURN"
        )
    )
    private void renderExtra(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int guiOffset, CallbackInfo ci) {
        if (ANVILCRAFT$RECURSION >= ANVILCRAFT$MAX_RECURSION) return;
        if (!(stack.getItem() instanceof IExtraItemDisplay item)) return;
        ItemStack innerStack = item.getDisplayedItem(stack);
        if (innerStack.isEmpty()) return;
        ANVILCRAFT$RECURSION++;
        pose.pushPose();
        pose.translate(x + item.xOffset(stack), y + item.yOffset(stack), 0);
        float scale = item.scale(stack);
        pose.scale(scale, scale, 1.0f);
        this.renderItem(entity, level, innerStack, 0, 0, seed, guiOffset + 10);
        ANVILCRAFT$RECURSION--;
        pose.popPose();
    }
}

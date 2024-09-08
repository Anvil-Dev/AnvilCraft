package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.item.IEngineerGoggles;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class TooltipRenderMixin {

    @Shadow
    public abstract Font getFont();

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/gui/GuiLayerManager;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
                    shift = At.Shift.AFTER
            )
    )
    void onHudRender(GuiGraphics guiGraphics, DeltaTracker pDeltaTracker, CallbackInfo ci) {
        float partialTick = pDeltaTracker.getGameTimeDeltaPartialTick(Minecraft.getInstance().isPaused());
        Window window = Minecraft.getInstance().getWindow();
        int screenWidth = window.getWidth();
        int screenHeight = window.getHeight();
        if (minecraft.player == null || minecraft.isPaused()) return;
        if (minecraft.screen != null) return;
        ItemStack mainHandItem = minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHandItem = minecraft.player.getItemInHand(InteractionHand.OFF_HAND);
        ItemStack handItem = mainHandItem.isEmpty() ? offHandItem : mainHandItem;
        if (!handItem.isEmpty()) {
            HudTooltipManager.INSTANCE.renderHandItemHudTooltip(
                    guiGraphics,
                    handItem,
                    partialTick,
                    screenWidth,
                    screenHeight
            );
        }
        if (!(minecraft.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof IEngineerGoggles)) return;
        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) hit).getBlockPos();
            if (minecraft.level == null) return;
            BlockEntity e = minecraft.level.getBlockEntity(blockPos);
            if (e == null) return;
            HudTooltipManager.INSTANCE.renderTooltip(guiGraphics, e, partialTick, screenWidth, screenHeight);
        }
    }
}

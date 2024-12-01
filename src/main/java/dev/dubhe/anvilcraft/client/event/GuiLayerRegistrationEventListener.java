package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.item.IEngineerGoggles;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import com.mojang.blaze3d.platform.Window;

public class GuiLayerRegistrationEventListener {

    public static void onRegister(RegisterGuiLayersEvent event) {
        event.registerAboveAll(AnvilCraft.of("power"), (guiGraphics, pDeltaTracker) -> {
            Minecraft minecraft = Minecraft.getInstance();
            float partialTick = pDeltaTracker.getGameTimeDeltaPartialTick(
                Minecraft.getInstance().isPaused()
            );
            Window window = Minecraft.getInstance().getWindow();
            int screenWidth = window.getGuiScaledWidth();
            int screenHeight = window.getGuiScaledHeight();
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
        });
    }
}

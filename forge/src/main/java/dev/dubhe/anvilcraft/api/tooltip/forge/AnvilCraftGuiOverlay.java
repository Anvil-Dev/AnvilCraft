package dev.dubhe.anvilcraft.api.tooltip.forge;

import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.item.IEngineerGoggles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class AnvilCraftGuiOverlay implements IGuiOverlay {
    @Override
    public void render(ForgeGui forgeGui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
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

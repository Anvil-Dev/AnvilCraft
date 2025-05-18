package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.platform.Window;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.client.hud.IonoCraftBackpackHUD;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public class GuiLayerRegistrationEventListener {

    public static void onRegister(RegisterGuiLayersEvent event) {
        event.registerAboveAll(AnvilCraft.of("power"), (guiGraphics, deltaTracker) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.options.hideGui) return;
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(
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
            if (!AnvilHammerItem.isWearing(minecraft.player)) return;
            HitResult hit = minecraft.hitResult;
            if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
                return;
            }
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = ((BlockHitResult) hit).getBlockPos();
                if (minecraft.level == null) return;
                BlockEntity e = minecraft.level.getBlockEntity(blockPos);
                if (e == null) {
                    BlockState s = minecraft.level.getBlockState(blockPos);
                    if (s.is(BlockTags.AIR)) return;
                    HudTooltipManager.INSTANCE.renderTooltip(guiGraphics, s, partialTick, screenWidth, screenHeight);
                    return;
                }
                HudTooltipManager.INSTANCE.renderTooltip(guiGraphics, e, partialTick, screenWidth, screenHeight);
            }
        });

        event.registerAboveAll(AnvilCraft.of("test"), GuiLayerRegistrationEventListener::render);
        event.registerAboveAll(AnvilCraft.of("ionocraft_backpack"), IonoCraftBackpackHUD::render);
    }

    @SuppressWarnings("EmptyMethod")
    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        // 删除了注释掉的代码
        // 可于VCS记录中找回
    }
}

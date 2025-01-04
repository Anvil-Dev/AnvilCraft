package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.item.IEngineerGoggles;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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

        event.registerAboveAll(AnvilCraft.of("test"), GuiLayerRegistrationEventListener::render);
    }

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
//        PoseStack poseStack = guiGraphics.pose();
//        Matrix4f matrix4f = poseStack.last().pose();
//        Tesselator tesselator = Tesselator.getInstance();
//        BufferBuilder bufferBuilder = tesselator.begin(
//            VertexFormat.Mode.QUADS,
//            DefaultVertexFormat.POSITION_COLOR
//        );
//
//        bufferBuilder.addVertex(matrix4f, 0, 0, 5).setColor(0xFFffffff);
//        bufferBuilder.addVertex(matrix4f, 0, 20, 5).setColor(0xFFffffff);
//        bufferBuilder.addVertex(matrix4f, 20, 20, 5).setColor(0xFFffffff);
//        bufferBuilder.addVertex(matrix4f, 20, 0, 5).setColor(0xFFffffff);
//        Window window = Minecraft.getInstance().getWindow();
//        float guiScale = (float) window.getGuiScale();
//        RenderSystem.setShader(ModShaders::getRingShader);
//
//        ModShaders.getRingShader()
//            .safeGetUniform("Center")
//            .set(10f * guiScale, 10f * guiScale);
//        ModShaders.getRingShader()
//            .safeGetUniform("FramebufferSize")
//            .set((float)window.getWidth(),(float) window.getHeight());
//        ModShaders.getRingShader()
//            .safeGetUniform("Radius")
//            .set(10f * guiScale);
//
//        RenderSystem.setShaderColor(1, 1, 1, 1);
//        BufferUploader.drawWithShader(bufferBuilder.build());


    }
}

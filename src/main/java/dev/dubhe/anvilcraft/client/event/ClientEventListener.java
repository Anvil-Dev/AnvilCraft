package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.item.StructureToolItem;
import dev.dubhe.anvilcraft.util.BlockHighlightUtil;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import static dev.dubhe.anvilcraft.util.BlockHighlightUtil.NO_DEPTH;

public class ClientEventListener {
    public static void blockHighlight(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (BlockHighlightUtil.SUBCHUNKS.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockHighlightUtil.render(
                level, Minecraft.getInstance().renderBuffers().bufferSource(), event.getPoseStack(), event.getCamera());
    }

    public static void StructureHighlight(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level != null && player != null) {
            ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
            StructureToolItem.StructureData data = itemStack.get(ModComponents.STRUCTURE_DATA);
            if (data != null) {
                MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
                VertexConsumer consumer = bufferSource.getBuffer(NO_DEPTH);
                PoseStack poseStack = event.getPoseStack();
                Camera camera = event.getCamera();

                Vec3 position = camera.getPosition();
                poseStack.pushPose();
                poseStack.translate(-position.x, -position.y, -position.z);

                LevelRenderer.renderLineBox(
                        poseStack,
                        consumer,
                        data.getMinX(),
                        data.getMinY(),
                        data.getMinZ(),
                        data.getMaxX() + 1,
                        data.getMaxY() + 1,
                        data.getMaxZ() + 1,
                        1,
                        1,
                        1,
                        1);

                poseStack.popPose();
            }
        }
    }
}

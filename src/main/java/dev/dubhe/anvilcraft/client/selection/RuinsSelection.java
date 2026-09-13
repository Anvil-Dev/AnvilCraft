package dev.dubhe.anvilcraft.client.selection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class RuinsSelection {
    private RuinsSelection() {
    }

    public static void prepare(RuinsBlockEntity ruins) {
        if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(ruins) instanceof RuinsBlockEntityRenderer renderer) {
            renderer.prepareDisplay(ruins);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void highlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.options.hideGui) return;
        BlockPos pos = event.getTarget().getBlockPos();
        if (!(level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins)) return;
        try (RuinsRenderContext ignored = RuinsRenderContext.enter(level)) {
            prepare(ruins);
            // 复用原方块的整组几何及 BER 姿态，避免把同一结构的体素箱逐帧重新做凸体并集。
            ModelBlockSelection.highlight(event);
            if (event.isCanceled()) return;
            BlockState state = ruins.getDisplayState();
            CollisionContext context = minecraft.player == null ? CollisionContext.empty() : CollisionContext.of(minecraft.player);
            VoxelShape shape = state.getShape(level, pos, context);
            PoseStack pose = event.getPoseStack();
            Vec3 camera = event.getCamera().getPosition();
            VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            shape.forAllEdges((x0, y0, z0, x1, y1, z1) -> {
                Vec3 normal = new Vec3(x1 - x0, y1 - y0, z1 - z0).normalize();
                consumer.addVertex(pose.last(), (float) x0, (float) y0, (float) z0).setColor(0, 0, 0, 0.4F)
                    .setNormal(pose.last(), (float) normal.x, (float) normal.y, (float) normal.z);
                consumer.addVertex(pose.last(), (float) x1, (float) y1, (float) z1).setColor(0, 0, 0, 0.4F)
                    .setNormal(pose.last(), (float) normal.x, (float) normal.y, (float) normal.z);
            });
            pose.popPose();
            event.setCanceled(true);
        }
    }
}

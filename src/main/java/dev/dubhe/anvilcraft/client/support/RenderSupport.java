package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.LevelLike;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Optional;

// TODO:
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RenderSupport {
    private static final int MAX_CACHE_SIZE = 64;
    private static final float WIP_PREVIEW_SCALE = 0.6F;
    private static final LinkedHashMap<BlockState, BlockEntity> BLOCK_ENTITY_CACHE = new LinkedHashMap<>();
    private static final LinkedHashMap<WipPreviewKey, LevelLike> WIP_LEVEL_CACHE = new LinkedHashMap<>();
    // private static final RandomSource RANDOM = RandomSource.createThreadLocalInstance();
    // public static final Vector3f L1 = new Vector3f(0.4F, 0.0F, 1.0F).normalize();
    // public static final Vector3f L2 = new Vector3f(-0.4F, 1.0F, -0.2F).normalize();
    private static final PoseStack.Pose BLOCK_DISPLAY_POSE;
    private static @Nullable ClientLevel currentClientLevel;

    static {
        BLOCK_DISPLAY_POSE = new PoseStack.Pose();
        RenderSupport.BLOCK_DISPLAY_POSE.rotate(Axis.XP.rotationDegrees(30));
        RenderSupport.BLOCK_DISPLAY_POSE.rotate(Axis.YP.rotationDegrees(45));
    }

    public static void renderBlock(GuiGraphicsExtractor graphics, BlockState block, float x, float y, float size) {
        GuiRenderExtras.tessellateBlock(
            graphics,
            block,
            null,
            null,
            x,
            y,
            x + size,
            y + size,
            -1,
            true,
            RenderSupport.BLOCK_DISPLAY_POSE.copy()
        );
    }

    public static void render3x3Block(GuiGraphicsExtractor graphics, BlockState block, float x, float y, float size) {
        PoseStack.Pose poseStack = RenderSupport.BLOCK_DISPLAY_POSE.copy();
        poseStack.scale(0.3f, 0.3f, 0.3f);
        GuiRenderExtras.tessellateBlock(
            graphics,
            block,
            null,
            null,
            x,
            y,
            x + size,
            y + size,
            -1,
            true,
            poseStack
        );
    }

    public static void renderWipBlock(
        GuiGraphicsExtractor graphics,
        Identifier recipeId,
        int stepCount,
        float x,
        float y,
        float size
    ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            RenderSupport.renderBlock(graphics, ModBlocks.WIP_BLOCK.get().defaultBlockState(), x, y, size);
            return;
        }
        if (RenderSupport.currentClientLevel != level) {
            RenderSupport.currentClientLevel = level;
            RenderSupport.WIP_LEVEL_CACHE.clear();
        }
        WipPreviewKey key = new WipPreviewKey(recipeId, stepCount);
        if (!RenderSupport.WIP_LEVEL_CACHE.containsKey(key) && RenderSupport.WIP_LEVEL_CACHE.size() >= RenderSupport.MAX_CACHE_SIZE) {
            RenderSupport.WIP_LEVEL_CACHE.pollFirstEntry();
        }
        LevelLike preview = RenderSupport.WIP_LEVEL_CACHE.computeIfAbsent(key, previewKey -> {
            LevelLike result = new LevelLike(level);
            result.setBlockState(BlockPos.ZERO, ModBlocks.WIP_BLOCK.get().defaultBlockState());
            if (result.getBlockEntity(BlockPos.ZERO) instanceof WipBlockEntity wip) {
                wip.setRecipeId(previewKey.recipeId());
                wip.setStepCount(previewKey.stepCount());
            }
            return result;
        });
        PoseStack poseStack = new PoseStack();
        poseStack.last().set(RenderSupport.BLOCK_DISPLAY_POSE);
        GuiRenderExtras.submitStructure(
            graphics,
            preview,
            BlockPos.ZERO,
            BlockPos.ZERO,
            x,
            y,
            x + size,
            y + size,
            size * RenderSupport.WIP_PREVIEW_SCALE,
            true,
            false,
            poseStack
        );
    }

    public static void renderLevelLike(
        LevelLike level,
        GuiGraphicsExtractor graphics,
        int posX,
        int posY,
        int size,
        int scale,
        float rotationSpeed,
        boolean glitched
    ) {
        Optional<BlockPos> minPos = level.getMinPos();
        Optional<BlockPos> maxPos = level.getMaxPos();
        if (minPos.isEmpty() || maxPos.isEmpty()) return;
        PoseStack poseStack = new PoseStack();
        poseStack.last().set(RenderSupport.BLOCK_DISPLAY_POSE);
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel currentLevel = minecraft.level;
        if (currentLevel == null) return;
        float gameTime = currentLevel.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        poseStack.mulPose(Axis.YP.rotationDegrees(gameTime * rotationSpeed));
        GuiRenderExtras.submitStructure(
            graphics,
            level,
            minPos.get(),
            maxPos.get(),
            posX,
            posY,
            posX + size,
            posY + size,
            scale,
            true,
            glitched,
            poseStack
        );
    }

    private static Optional<BlockEntity> getCachedBlockEntity(BlockState state) {
        if (!state.hasBlockEntity()) return Optional.empty();
        if (RenderSupport.BLOCK_ENTITY_CACHE.containsKey(state)) return Optional.of(RenderSupport.BLOCK_ENTITY_CACHE.get(state));
        Optional<BlockEntity> opt = Optional.of(state.getBlock())
            .filter(b -> b instanceof EntityBlock)
            .map(b -> ((EntityBlock) b).newBlockEntity(BlockPos.ZERO, state));
        opt.ifPresent(be -> {
            RenderSupport.BLOCK_ENTITY_CACHE.put(state, be);
            if (RenderSupport.BLOCK_ENTITY_CACHE.size() > RenderSupport.MAX_CACHE_SIZE) {
                RenderSupport.BLOCK_ENTITY_CACHE.pollFirstEntry();
            }
        });
        return opt;
    }

    private record WipPreviewKey(Identifier recipeId, int stepCount) {
    }
}

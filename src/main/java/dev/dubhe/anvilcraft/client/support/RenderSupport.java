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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Optional;

// TODO:
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RenderSupport {
    private static final int MAX_CACHE_SIZE = 64;
    private static final LinkedHashMap<BlockState, BlockEntity> BLOCK_ENTITY_CACHE = new LinkedHashMap<>();
    private static final LinkedHashMap<Identifier, LevelLike> WIP_LEVEL_CACHE = new LinkedHashMap<>();
    // private static final RandomSource RANDOM = RandomSource.createThreadLocalInstance();
    // public static final Vector3f L1 = new Vector3f(0.4F, 0.0F, 1.0F).normalize();
    // public static final Vector3f L2 = new Vector3f(-0.4F, 1.0F, -0.2F).normalize();
    private static final PoseStack.Pose BLOCK_DISPLAY_POSE;
    private static ClientLevel currentClientLevel = null;
    private static LevelLike.AirLevelLike airLevelLike = null;

    static {
        BLOCK_DISPLAY_POSE = new PoseStack.Pose();
        BLOCK_DISPLAY_POSE.rotate(Axis.XP.rotationDegrees(30));
        BLOCK_DISPLAY_POSE.rotate(Axis.YP.rotationDegrees(45));
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
            BLOCK_DISPLAY_POSE.copy()
        );
    }

    public static void renderWipBlock(
        GuiGraphicsExtractor graphics,
        Identifier recipeId,
        float x,
        float y,
        float size
    ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            renderBlock(graphics, ModBlocks.WIP_BLOCK.get().defaultBlockState(), x, y, size);
            return;
        }
        if (currentClientLevel != level) {
            currentClientLevel = level;
            WIP_LEVEL_CACHE.clear();
        }
        if (!WIP_LEVEL_CACHE.containsKey(recipeId) && WIP_LEVEL_CACHE.size() >= MAX_CACHE_SIZE) {
            WIP_LEVEL_CACHE.pollFirstEntry();
        }
        LevelLike preview = WIP_LEVEL_CACHE.computeIfAbsent(recipeId, id -> {
            LevelLike result = new LevelLike(level);
            result.setBlockState(BlockPos.ZERO, ModBlocks.WIP_BLOCK.get().defaultBlockState());
            if (result.getBlockEntity(BlockPos.ZERO) instanceof WipBlockEntity wip) {
                wip.setRecipeId(id);
            }
            return result;
        });
        PoseStack poseStack = new PoseStack();
        poseStack.last().set(BLOCK_DISPLAY_POSE);
        GuiRenderExtras.submitStructure(
            graphics,
            preview,
            BlockPos.ZERO,
            BlockPos.ZERO,
            x,
            y,
            x + size,
            y + size,
            size,
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
        poseStack.last().set(BLOCK_DISPLAY_POSE);
        Minecraft minecraft = Minecraft.getInstance();
        float gameTime = (minecraft.level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
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
        if (BLOCK_ENTITY_CACHE.containsKey(state)) return Optional.of(BLOCK_ENTITY_CACHE.get(state));
        Optional<BlockEntity> opt = Optional.of(state.getBlock())
            .filter(b -> b instanceof EntityBlock)
            .map(b -> ((EntityBlock) b).newBlockEntity(BlockPos.ZERO, state));
        opt.ifPresent(be -> {
            BLOCK_ENTITY_CACHE.put(state, be);
            if (BLOCK_ENTITY_CACHE.size() > MAX_CACHE_SIZE) {
                BLOCK_ENTITY_CACHE.pollFirstEntry();
            }
        });
        return opt;
    }
}

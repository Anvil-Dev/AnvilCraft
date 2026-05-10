package dev.dubhe.anvilcraft.client.gui.state;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public record RotatedBlockRenderState(
    MovingBlockRenderState state,
    Vector3f translation,
    Quaternionf rotation,
    @Nullable Quaternionf overrideCameraAngle,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public RotatedBlockRenderState(
        ClientLevel level,
        BlockPos pos,
        BlockState state,
        Holder<Biome> biome,
        Vector3f translation,
        Quaternionf rotation,
        @Nullable Quaternionf overrideCameraAngle,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(
            RotatedBlockRenderState.createMovingBlock(pos, state, biome, level),
            translation,
            rotation,
            overrideCameraAngle,
            x0,
            y0,
            x1,
            y1,
            scale,
            scissorArea,
            PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea)
        );
    }

    private static MovingBlockRenderState createMovingBlock(BlockPos pos, BlockState blockState, Holder<Biome> biome, ClientLevel level) {
        MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
        movingBlockRenderState.randomSeedPos = pos;
        movingBlockRenderState.blockPos = pos;
        movingBlockRenderState.blockState = blockState;
        movingBlockRenderState.biome = biome;
        movingBlockRenderState.cardinalLighting = level.cardinalLighting();
        movingBlockRenderState.lightEngine = level.getLightEngine();
        return movingBlockRenderState;
    }
}

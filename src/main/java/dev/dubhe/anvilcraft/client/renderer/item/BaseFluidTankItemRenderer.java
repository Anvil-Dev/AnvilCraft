package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.item.state.FluidTankItemRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

abstract class BaseFluidTankItemRenderer implements SpecialModelRenderer<FluidTankItemRenderState> {
    private static final long MODEL_SEED = 42L;

    private final BlockState shellState;
    private final float minExtent;
    private final float maxExtent;
    private final BlockModelRenderState shellRenderState = new BlockModelRenderState();
    private @Nullable BlockStateModel shellModel;

    protected BaseFluidTankItemRenderer(BlockState shellState, float minExtent, float maxExtent) {
        this.shellState = shellState;
        this.minExtent = minExtent;
        this.maxExtent = maxExtent;
    }

    protected void submitShell(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int lightCoords,
        int overlayCoords,
        int outlineColor
    ) {
        BlockStateModel model = Minecraft.getInstance()
            .getModelManager()
            .getBlockStateModelSet()
            .get(this.shellState);
        if (model != this.shellModel) {
            this.shellRenderState.clear();
            model.collectParts(
                BlockAndTintGetter.EMPTY,
                BlockPos.ZERO,
                this.shellState,
                RandomSource.create(BaseFluidTankItemRenderer.MODEL_SEED),
                this.shellRenderState.setupModel(new Matrix4f(), false)
            );
            this.shellModel = model;
        }
        this.shellRenderState.submit(poseStack, collector, lightCoords, overlayCoords, outlineColor);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(this.minExtent, this.minExtent, this.minExtent));
        output.accept(new Vector3f(this.minExtent, this.minExtent, this.maxExtent));
        output.accept(new Vector3f(this.minExtent, this.maxExtent, this.minExtent));
        output.accept(new Vector3f(this.minExtent, this.maxExtent, this.maxExtent));
        output.accept(new Vector3f(this.maxExtent, this.minExtent, this.minExtent));
        output.accept(new Vector3f(this.maxExtent, this.minExtent, this.maxExtent));
        output.accept(new Vector3f(this.maxExtent, this.maxExtent, this.minExtent));
        output.accept(new Vector3f(this.maxExtent, this.maxExtent, this.maxExtent));
    }
}

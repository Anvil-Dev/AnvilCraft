package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.api.hammer.IHasHammerEffect;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder.ModelRenderTarget;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class HammerPreviewState implements IHasHammerEffect {
    private final BlockPos target;
    private final BlockState initial;
    private final List<BlockState> choices;
    private final ModelRenderTarget originalModel;
    private ModelRenderTarget currentModel;
    private BlockState currentState;
    private boolean active;

    public HammerPreviewState(BlockPos target, BlockState initial, List<BlockState> choices) {
        this.target = target.immutable();
        this.initial = initial;
        this.choices = List.copyOf(choices);
        this.originalModel = this.model(initial);
        this.currentModel = this.originalModel;
        this.currentState = initial;
    }

    private ModelRenderTarget model(BlockState state) {
        var level = Minecraft.getInstance().level;
        return level != null && state.getBlock() instanceof IMultiPartBlockModelHolder holder
            ? holder.getModelRenderTarget(level, this.target, this.initial, state) : new ModelRenderTarget(this.target, state);
    }

    public int currentIndex() {
        return this.choices.indexOf(this.currentState);
    }

    public void begin() {
        if (this.active) return;
        this.active = true;
        Minecraft.getInstance().levelRenderer.setBlockDirty(this.hiddenBlockPos(), false);
    }

    public void select(int index) {
        if (index >= 0 && index < this.choices.size()) {
            this.currentState = this.choices.get(index);
            this.currentModel = this.model(this.currentState);
        }
    }

    public boolean isValid() {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.getBlockState(this.target).is(this.initial.getBlock())) return false;
        var entity = level.getBlockEntity(this.target);
        return entity == null || entity.getType().isValid(this.currentState);
    }

    public void end() {
        this.active = false;
        var client = Minecraft.getInstance();
        if (client.level == null) return;
        client.levelRenderer.setBlockDirty(this.hiddenBlockPos(), false);
        if (!this.target.equals(this.hiddenBlockPos())) client.levelRenderer.setBlockDirty(this.target, false);
    }

    @Override
    public boolean shouldRender() {
        return this.active && Minecraft.getInstance().level != null;
    }

    @Override
    public boolean shouldSkipRebuildBlock() {
        return this.shouldRender();
    }

    @Override
    public BlockPos renderingBlockPos() {
        return this.currentModel.pos();
    }

    @Override
    public BlockPos hiddenBlockPos() {
        return this.originalModel.pos();
    }

    @Override
    public BlockState renderingBlockState() {
        return this.currentModel.state();
    }

    @Override
    public RenderType renderType() {
        return IrisState.isShaderEnabled() ? RenderTypes.translucentMovingBlock() : ModRenderTypes.TRANSLUCENT_COLORED_OVERLAY;
    }
}

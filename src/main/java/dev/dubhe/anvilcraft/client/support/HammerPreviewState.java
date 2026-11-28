package dev.dubhe.anvilcraft.client.support;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.dubhe.anvilcraft.api.hammer.IHasHammerEffect;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder.ModelRenderTarget;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HammerPreviewState implements IHasHammerEffect {
    // 区块编译线程只读取不可变的位置集合。
    private static volatile Set<BlockPos> hidden = Set.of();
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
        if (this.active) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Set<BlockPos> positions = new HashSet<>();
        positions.add(this.target);
        for (var part : this.originalModel.parts()) {
            if (level.getBlockState(part.pos()).is(part.state().getBlock())) {
                positions.add(part.pos());
            }
        }
        this.active = true;
        hidden = Set.copyOf(positions);
        refresh(positions);
    }

    public static boolean hidesBlock(BlockPos pos) {
        if (!hidden.isEmpty()) {
            return hidden.contains(pos);
        }
        return Minecraft.getInstance().screen instanceof IHasHammerEffect effect
            && effect.shouldSkipRebuildBlock() && effect.hiddenBlockPos().equals(pos);
    }

    public static boolean hidesNeighbor(BlockPos pos, Direction face) {
        var positions = hidden;
        return !positions.isEmpty() && positions.contains(pos.relative(face));
    }

    private static void refresh(Set<BlockPos> positions) {
        var client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        var pipeline = CachedBlockEntityRenderingPipeline.getInstance();
        for (BlockPos pos : positions) {
            client.levelRenderer.setBlockDirty(pos, true);
            var entity = client.level.getBlockEntity(pos);
            if (entity != null && pipeline != null) {
                pipeline.update(entity, true);
            }
        }
    }

    public void select(int index) {
        BlockState selected = index >= 0 && index < this.choices.size() ? this.choices.get(index) : this.initial;
        if (selected != this.currentState) {
            this.currentState = selected;
            this.currentModel = selected == this.initial ? this.originalModel : this.model(selected);
        }
    }

    public boolean isValid() {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.getBlockState(this.target).is(this.initial.getBlock())) {
            return false;
        }
        var entity = level.getBlockEntity(this.target);
        return entity == null || entity.getType().isValid(this.currentState);
    }

    public void end() {
        if (!this.active) {
            return;
        }
        this.active = false;
        Set<BlockPos> positions = hidden;
        hidden = Set.of();
        refresh(positions);
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
        return ModRenderTypes.TRANSLUCENT_COLORED_OVERLAY;
    }
}

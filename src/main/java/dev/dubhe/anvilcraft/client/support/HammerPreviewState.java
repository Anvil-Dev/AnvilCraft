package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.api.hammer.IHasHammerEffect;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder.ModelRenderTarget;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import dev.dubhe.anvilcraft.item.property.component.MountedButtonBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class HammerPreviewState implements IHasHammerEffect {
    private final BlockPos target;
    private final BlockState initial;
    private final List<BlockState> choices;
    private final @Nullable MountedButtonBlock mounted;
    private final List<MountedButtonBlock> mountedChoices;
    private final int initialIndex;
    private int selectedIndex;
    private final ModelRenderTarget originalModel;
    private ModelRenderTarget currentModel;
    private BlockState currentState;
    private boolean active;

    public HammerPreviewState(BlockPos target, BlockState initial, List<BlockState> choices) {
        this(target, initial, choices, null, List.of());
    }

    public HammerPreviewState(BlockPos target, MountedButtonBlock mounted, List<MountedButtonBlock> choices) {
        this(target, mounted.state(), choices.stream().map(MountedButtonBlock::state).toList(), mounted, List.copyOf(choices));
    }

    private HammerPreviewState(
        BlockPos target, BlockState initial, List<BlockState> choices,
        @Nullable MountedButtonBlock mounted, List<MountedButtonBlock> mountedChoices
    ) {
        this.target = target.immutable();
        this.initial = initial;
        this.choices = List.copyOf(choices);
        this.mounted = mounted;
        this.mountedChoices = mountedChoices;
        this.initialIndex = mounted == null ? choices.indexOf(initial) : mountedChoices.indexOf(mounted);
        this.selectedIndex = this.initialIndex;
        this.originalModel = this.model(initial);
        this.currentModel = this.originalModel;
        this.currentState = initial;
    }

    private ModelRenderTarget model(BlockState state) {
        if (this.mounted != null) {
            return new ModelRenderTarget(this.target, state);
        }
        var level = Minecraft.getInstance().level;
        return level != null && state.getBlock() instanceof IMultiPartBlockModelHolder holder
            ? holder.getModelRenderTarget(level, this.target, this.initial, state) : new ModelRenderTarget(this.target, state);
    }

    public int currentIndex() {
        return this.selectedIndex;
    }

    public void begin() {
        if (this.active) return;
        this.active = true;
        if (this.mounted == null) {
            Minecraft.getInstance().levelRenderer.setBlockDirty(this.hiddenBlockPos(), false);
        }
    }

    public void select(int index) {
        this.selectedIndex = index >= 0 && index < this.choices.size() ? index : this.initialIndex;
        BlockState selected = index >= 0 && index < this.choices.size() ? this.choices.get(index) : this.initial;
        if (selected != this.currentState) {
            this.currentState = selected;
            this.currentModel = selected == this.initial ? this.originalModel : this.model(selected);
        }
    }

    public boolean isValid() {
        var client = Minecraft.getInstance();
        var level = client.level;
        if (this.mounted != null) {
            if (level == null || client.player == null || !this.mounted.canAdjustWithHammer(client.player)
                || !(level.getBlockEntity(this.target) instanceof BigRedButtonBlockEntity button)) {
                return false;
            }
            var contents = button.getMountedBlock();
            return this.mounted.equals(contents) && (this.active
                || client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(this.target) && BigRedButtonBlock.isBaseHit(button.getBlockState(), level, hit));
        }
        if (level == null || !level.getBlockState(this.target).is(this.initial.getBlock())) return false;
        var entity = level.getBlockEntity(this.target);
        return entity == null || entity.getType().isValid(this.currentState);
    }

    public void end() {
        this.active = false;
        if (this.mounted != null) {
            return;
        }
        var client = Minecraft.getInstance();
        if (client.level == null) return;
        client.levelRenderer.setBlockDirty(this.hiddenBlockPos(), false);
        if (!this.target.equals(this.hiddenBlockPos())) client.levelRenderer.setBlockDirty(this.target, false);
    }

    @Override
    public boolean shouldRender() {
        return this.mounted == null && this.active && Minecraft.getInstance().level != null;
    }

    public @Nullable MountedButtonBlock mountedBlockAt(BlockPos pos) {
        if (this.mounted == null || !this.active || !this.target.equals(pos)) {
            return null;
        }
        return this.selectedIndex < 0 ? this.mounted : this.mountedChoices.get(this.selectedIndex);
    }

    public boolean isMounted() {
        return this.mounted != null;
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

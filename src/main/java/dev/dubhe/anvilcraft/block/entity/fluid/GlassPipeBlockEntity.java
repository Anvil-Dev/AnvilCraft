package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class GlassPipeBlockEntity extends AbstractPipeCheckValveBlockEntity {
    private static final String TAG_DISPLAY_FLUID = "DisplayFluid";
    private static final String TAG_DISPLAY_DIRECTIONS = "DisplayDirections";
    private static final String TAG_DISPLAY_UNTIL = "DisplayUntil";
    /** 最后一次流动事件后，流体显示保持可见的 tick 数（流动停止后的清理依据）。 */
    private static final int DISPLAY_DURATION = 1;

    private FluidStack displayFluid = FluidStack.EMPTY;
    private final EnumSet<Direction> displayDirections = EnumSet.noneOf(Direction.class);
    private final Map<Direction, Long> displayDirectionUntil = new EnumMap<>(Direction.class);
    private long displayUntil = Long.MIN_VALUE;

    public GlassPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public FluidStack getDisplayFluid() {
        if (this.level == null || this.displayFluid.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return this.displayFluid;
    }

    public Set<Direction> getDisplayDirections() {
        if (this.level == null || this.displayFluid.isEmpty()) {
            return Set.of();
        }
        if (this.displayDirections.isEmpty()) {
            return Set.of();
        }
        return EnumSet.copyOf(this.displayDirections);
    }

    public void showFluid(FluidStack fluid, Set<Direction> directions) {
        if (fluid.isEmpty() || this.level == null || this.level.isClientSide()) {
            return;
        }
        if (!(this.getBlockState().getBlock() instanceof PipeBlock pipe) || !pipe.isGlassPipe()) {
            return;
        }
        long gameTime = this.level.getGameTime();
        final long displayEndGameTime = gameTime + DISPLAY_DURATION;
        final boolean expired = gameTime > this.displayUntil;
        final boolean sameFluid = !this.displayFluid.isEmpty()
            && FluidStack.isSameFluidSameComponents(this.displayFluid, fluid);
        boolean directionsChanged = this.updateDisplayDirections(directions, gameTime, displayEndGameTime, expired || !sameFluid);
        final boolean changed = expired || !sameFluid || directionsChanged;
        this.displayFluid = fluid.copyWithAmount(1);
        this.displayUntil = displayEndGameTime;
        // 仅在显示状态实际变化（开始/停止/换流体/换方向）时向客户端发包；
        // 持续流动相同流体期间不再周期性刷新，显示结束由所属网络的过期检测负责。
        if (changed) {
            this.setChanged();
            this.sendUpdate();
        }
    }

    /**
     * 显示有效期已过则清除并同步客户端；返回本次是否发生了清除。
     * 由所属管道网络每 tick 调用，使流动停止后的显示按时消失。
     */
    public boolean checkDisplayExpiry() {
        if (this.level == null || this.level.isClientSide() || this.displayFluid.isEmpty()) {
            return false;
        }
        if (this.level.getGameTime() <= this.displayUntil) {
            return false;
        }
        this.clearDisplay();
        return true;
    }

    /** 立即清除显示并同步客户端（显示状态变化 → 发包）。 */
    public void clearDisplay() {
        if (this.level == null || this.level.isClientSide() || this.displayFluid.isEmpty()) {
            return;
        }
        this.displayFluid = FluidStack.EMPTY;
        this.displayDirections.clear();
        this.displayDirectionUntil.clear();
        this.displayUntil = Long.MIN_VALUE;
        this.setChanged();
        this.sendUpdate();
    }

    private boolean updateDisplayDirections(
        Set<Direction> directions, long gameTime, long displayEndGameTime, boolean reset
    ) {
        final int previousMask = writeDisplayDirections(this.displayDirections);
        if (reset) {
            this.displayDirections.clear();
            this.displayDirectionUntil.clear();
        } else {
            this.clearExpiredDisplayDirections(gameTime);
        }
        for (Direction direction : directions) {
            this.displayDirections.add(direction);
            this.displayDirectionUntil.put(direction, displayEndGameTime);
        }
        return previousMask != writeDisplayDirections(this.displayDirections);
    }

    private void clearExpiredDisplayDirections(long gameTime) {
        for (Direction direction : Direction.values()) {
            Long until = this.displayDirectionUntil.get(direction);
            if (until != null && gameTime > until) {
                this.displayDirections.remove(direction);
                this.displayDirectionUntil.remove(direction);
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_DISPLAY_FLUID, Tag.TAG_COMPOUND)) {
            this.displayFluid = FluidStack.parseOptional(registries, tag.getCompound(TAG_DISPLAY_FLUID));
            this.displayUntil = tag.getLong(TAG_DISPLAY_UNTIL);
            readDisplayDirections(tag.getInt(TAG_DISPLAY_DIRECTIONS));
        } else {
            this.displayFluid = FluidStack.EMPTY;
            this.displayDirections.clear();
            this.displayDirectionUntil.clear();
            this.displayUntil = Long.MIN_VALUE;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!this.displayFluid.isEmpty()) {
            tag.put(TAG_DISPLAY_FLUID, this.displayFluid.save(registries));
            tag.putInt(TAG_DISPLAY_DIRECTIONS, writeDisplayDirections(this.displayDirections));
            tag.putLong(TAG_DISPLAY_UNTIL, this.displayUntil);
        }
        return tag;
    }

    private static int writeDisplayDirections(Set<Direction> directions) {
        int mask = 0;
        for (Direction direction : directions) {
            mask |= 1 << direction.get3DDataValue();
        }
        return mask;
    }

    private void readDisplayDirections(int mask) {
        this.displayDirections.clear();
        this.displayDirectionUntil.clear();
        for (Direction direction : Direction.values()) {
            if ((mask & (1 << direction.get3DDataValue())) != 0) {
                this.displayDirections.add(direction);
                this.displayDirectionUntil.put(direction, this.displayUntil);
            }
        }
    }
}

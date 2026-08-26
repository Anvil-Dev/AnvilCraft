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
    private static final String TAG_GAS_FLUID = "GasFluid";
    private static final String TAG_GAS_DIRECTIONS = "GasDirections";
    private static final String TAG_GAS_ALPHA = "GasAlpha";
    /** 最后一次流动事件后，流体显示保持可见的 tick 数（流动停止后的清理依据）。 */
    private static final int DISPLAY_DURATION = 1;

    private FluidStack displayFluid = FluidStack.EMPTY;
    private final EnumSet<Direction> displayDirections = EnumSet.noneOf(Direction.class);
    private final Map<Direction, Long> displayDirectionUntil = new EnumMap<>(Direction.class);
    private long displayUntil = Long.MIN_VALUE;

    /** 气体持久显示：存在扩散体系时，连接参与扩散端点的管道持续充满气体。 */
    private FluidStack gasFluid = FluidStack.EMPTY;
    private final EnumSet<Direction> gasDirections = EnumSet.noneOf(Direction.class);
    /** 气体渲染透明度（0..1），与扩散系内储罐的气体透明度一致。 */
    private float gasAlpha = 1.0f;

    public GlassPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public FluidStack getDisplayFluid() {
        if (this.level == null) {
            return FluidStack.EMPTY;
        }
        if (!this.gasFluid.isEmpty()) {
            return this.gasFluid;
        }
        return this.displayFluid;
    }

    public Set<Direction> getDisplayDirections() {
        if (this.level == null) {
            return Set.of();
        }
        if (!this.gasFluid.isEmpty()) {
            if (this.gasDirections.isEmpty()) {
                return Set.of();
            }
            return EnumSet.copyOf(this.gasDirections);
        }
        if (this.displayFluid.isEmpty()) {
            return Set.of();
        }
        if (this.displayDirections.isEmpty()) {
            return Set.of();
        }
        return EnumSet.copyOf(this.displayDirections);
    }

    /**
     * 设置气体持久显示：管道持续充满指定气体（直到扩散体系消失时清除），
     * 不受短暂液体显示的过期清理影响。
     *
     * @param alphaFill 气体渲染透明度（0..1），与扩散系内储罐的气体透明度一致
     */
    public void setGasDisplay(FluidStack fluid, Set<Direction> directions, float alphaFill) {
        if (fluid.isEmpty() || this.level == null || this.level.isClientSide()) {
            return;
        }
        if (!(this.getBlockState().getBlock() instanceof PipeBlock pipe) || !pipe.isGlassPipe()) {
            return;
        }
        final boolean sameFluid = !this.gasFluid.isEmpty()
            && FluidStack.isSameFluidSameComponents(this.gasFluid, fluid);
        boolean directionsChanged = !this.gasDirections.equals(directions);
        boolean alphaChanged = Float.compare(this.gasAlpha, alphaFill) != 0;
        if (sameFluid && !directionsChanged && !alphaChanged) {
            return;
        }
        this.gasFluid = fluid.copyWithAmount(1);
        this.gasDirections.clear();
        this.gasDirections.addAll(directions);
        this.gasAlpha = alphaFill;
        this.setChanged();
        this.sendUpdate();
    }

    /** 当前是否处于气体持久显示状态。 */
    public boolean isShowingGas() {
        return !this.gasFluid.isEmpty();
    }

    /** 当前气体显示的透明度（0..1），供渲染器与扩散系内储罐透明度保持一致。 */
    public float getGasAlpha() {
        return this.gasAlpha;
    }

    /** 清除气体持久显示（扩散体系消失时）。 */
    public void clearGasDisplay() {
        if (this.level == null || this.level.isClientSide() || this.gasFluid.isEmpty()) {
            return;
        }
        this.gasFluid = FluidStack.EMPTY;
        this.gasDirections.clear();
        this.gasAlpha = 1.0f;
        this.setChanged();
        this.sendUpdate();
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
     * 气体持久显示不受过期影响。
     */
    public boolean checkDisplayExpiry() {
        if (this.level == null || this.level.isClientSide() || this.displayFluid.isEmpty()) {
            return false;
        }
        if (this.level.getGameTime() <= this.displayUntil) {
            return false;
        }
        this.clearLiquidDisplay();
        return true;
    }

    /** 立即清除显示并同步客户端（显示状态变化 → 发包）。 */
    public void clearDisplay() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.clearLiquidDisplay();
        this.clearGasDisplay();
    }

    private void clearLiquidDisplay() {
        if (this.displayFluid.isEmpty()) {
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
        if (tag.contains(TAG_GAS_FLUID, Tag.TAG_COMPOUND)) {
            this.gasFluid = FluidStack.parseOptional(registries, tag.getCompound(TAG_GAS_FLUID));
            this.gasAlpha = tag.getFloat(TAG_GAS_ALPHA);
            readGasDirections(tag.getInt(TAG_GAS_DIRECTIONS));
        } else {
            this.gasAlpha = 1.0f;
            this.gasFluid = FluidStack.EMPTY;
            this.gasDirections.clear();
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
        if (!this.gasFluid.isEmpty()) {
            tag.put(TAG_GAS_FLUID, this.gasFluid.save(registries));
            tag.putInt(TAG_GAS_DIRECTIONS, writeDisplayDirections(this.gasDirections));
        }
        tag.putFloat(TAG_GAS_ALPHA, this.gasAlpha);
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

    private void readGasDirections(int mask) {
        this.gasDirections.clear();
        for (Direction direction : Direction.values()) {
            if ((mask & (1 << direction.get3DDataValue())) != 0) {
                this.gasDirections.add(direction);
            }
        }
    }
}

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
    private static final int DISPLAY_DURATION = 40;
    private static final int DISPLAY_SYNC_INTERVAL = 20;

    private FluidStack displayFluid = FluidStack.EMPTY;
    private final EnumSet<Direction> displayDirections = EnumSet.noneOf(Direction.class);
    private final Map<Direction, Long> displayDirectionUntil = new EnumMap<>(Direction.class);
    private long displayUntil = Long.MIN_VALUE;
    private long lastDisplaySync = Long.MIN_VALUE;

    public GlassPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public FluidStack getDisplayFluid() {
        if (this.level == null || this.displayFluid.isEmpty() || this.level.getGameTime() > this.displayUntil) {
            return FluidStack.EMPTY;
        }
        return this.displayFluid;
    }

    public Set<Direction> getDisplayDirections() {
        if (this.level == null || this.displayFluid.isEmpty() || this.level.getGameTime() > this.displayUntil) {
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
        final boolean expired = gameTime > this.displayUntil;
        final boolean sameFluid = !this.displayFluid.isEmpty()
            && FluidStack.isSameFluidSameComponents(this.displayFluid, fluid);
        final long displayEndGameTime = gameTime + DISPLAY_DURATION;
        boolean directionsChanged = this.updateDisplayDirections(directions, gameTime, displayEndGameTime, expired || !sameFluid);
        final boolean changed = expired || !sameFluid || directionsChanged;
        this.displayFluid = fluid.copyWithAmount(1);
        this.displayUntil = displayEndGameTime;
        if (changed) {
            this.setChanged();
        }
        if (changed || gameTime - this.lastDisplaySync >= DISPLAY_SYNC_INTERVAL) {
            this.lastDisplaySync = gameTime;
            this.sendUpdate();
        }
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

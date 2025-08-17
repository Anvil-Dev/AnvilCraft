package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.ItemDetectorBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.ItemDetectorMenu;
import dev.dubhe.anvilcraft.inventory.container.FilterOnlyContainer;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

import static dev.dubhe.anvilcraft.block.ItemDetectorBlock.POWERED;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemDetectorBlockEntity extends BlockEntity implements MenuProvider, IFilterBlockEntity, IHasAffectRange,
    IDiskCloneable {

    public static final int DATASLOT_ID_RANGE = 0;
    public static final int DATASLOT_ID_FILTER_MODE = 1;
    private static final FilteredItemStackHandler DUMMY_HANDLER = new FilteredItemStackHandler(0);
    private static final int MIN_RANGE = 1;
    private static final int MAX_RANGE = 8;
    @Getter
    private final FilterOnlyContainer filter;
    @Getter
    private Mode filterMode;
    @Getter
    private int range = 0;
    private boolean rangeChanged = true;
    private AABB detectionRange;
    @Getter
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATASLOT_ID_RANGE -> ItemDetectorBlockEntity.this.range;
                case DATASLOT_ID_FILTER_MODE -> ItemDetectorBlockEntity.this.filterMode.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATASLOT_ID_RANGE:
                    ItemDetectorBlockEntity.this.setRange(value);
                    break;
                case DATASLOT_ID_FILTER_MODE:
                    if (value < 0 || value >= Mode.values().length) return;
                    ItemDetectorBlockEntity.this.setFilterMode(Mode.values()[value]);
                    break;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };
    @Getter
    private int outputSignal = 0;

    public ItemDetectorBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.filterMode = Mode.ANY;
        this.filter = new FilterOnlyContainer(this, 9);
        this.setRange(1);
    }

    public ItemDetectorBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.ITEM_DETECTOR.get(), pos, blockState);
    }

    public static ItemDetectorBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState) {
        return new ItemDetectorBlockEntity(type, pos, blockState);
    }

    private static int lerpOutput(int matchCount, int targetCount) {
        if (matchCount < targetCount) return 0;
        return Math.min(15, 1 + (matchCount - targetCount) * 14 / (63 * targetCount));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.setRange(tag.getInt("Range"));
        if (tag.contains("FilterMode")) this.filterMode = Mode.valueOf(tag.getString("FilterMode"));
        if (tag.contains("Filter")) filter.deserializeNBT(registries, tag.getCompound("Filter"));
        if (tag.contains("OutputSignal")) this.outputSignal = tag.getInt("OutputSignal");
        this.recalcDetectionRange();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Range", this.range);
        tag.putString("FilterMode", this.filterMode.toString());
        tag.put("Filter", this.filter.serializeNBT(registries));
        tag.putInt("OutputSignal", this.outputSignal);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        if (!this.rangeChanged) return new CompoundTag();
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Range", this.range);
        this.rangeChanged = false;
        return tag;
    }

    public void tick() {
        Level level = this.level;
        if (level == null || level.isClientSide) return;
        if (this.detectionRange == null) {
            this.recalcDetectionRange();
            if (this.detectionRange == null) return;
        }
        BlockPos pos = this.getBlockPos();
        BlockState blockState = level.getBlockState(pos);
        if (!blockState.is(ModBlocks.ITEM_DETECTOR)) return;
        List<ItemEntity> itemEntities = level.getEntitiesOfClass(
            ItemEntity.class,
            this.detectionRange,
            entity -> !entity.getItem().isEmpty()
        );
        int output = getOutput(itemEntities);
        if (output == this.outputSignal) return;
        this.outputSignal = output;
        if (blockState.getValue(POWERED) != (this.outputSignal > 0)) {
            blockState = blockState.setValue(POWERED, this.outputSignal > 0);
            level.setBlock(pos, blockState, 2);
        }
        ModBlocks.ITEM_DETECTOR.get().updateNeighborsInFront(level, pos, blockState);
    }

    private int getOutput(List<ItemEntity> itemEntities) {
        if (itemEntities.isEmpty()) return 0;
        int minNonZeroOutput = 16;
        boolean canOutput = true;
        boolean hasFilter = false;
        for (ItemStack filterStack : this.getFilteredItems()) {
            if (filterStack.isEmpty()) continue;
            hasFilter = true;
            Item filterItem = filterStack.getItem();
            int matchCount = 0;
            int targetCount = filterStack.getCount();
            for (ItemEntity itemEntity : itemEntities) {
                if (itemEntity.getItem().is(filterItem)) {
                    matchCount += itemEntity.getItem().getCount();
                }
            }
            int lerpedOutput = lerpOutput(matchCount, targetCount);
            if (lerpedOutput > 0) {
                minNonZeroOutput = Math.min(minNonZeroOutput, lerpedOutput);
            } else if (this.filterMode == Mode.ALL) {
                canOutput = false;
                break;
            }
        }
        int output = (canOutput && minNonZeroOutput <= 15) ? minNonZeroOutput : 0;
        if (!hasFilter) {
            int totalCount = 0;
            for (ItemEntity itemEntity : itemEntities) {
                totalCount += itemEntity.getItem().getCount();
            }
            output = lerpOutput(totalCount, 1);
        }
        return output;
    }

    public void setFilterMode(Mode filterMode) {
        if (this.filterMode == filterMode) return;
        this.filterMode = filterMode;
        this.setChanged();
    }

    public void increaseRange() {
        this.range = Mth.clamp(range + 1, MIN_RANGE, MAX_RANGE);
    }

    public void decreaseRange() {
        this.range = Mth.clamp(range - 1, MIN_RANGE, MAX_RANGE);
    }

    public void setRange(int range) {
        range = Mth.clamp(range, MIN_RANGE, MAX_RANGE);
        if (this.range == range) return;
        this.range = range;
        this.recalcDetectionRange();
    }

    @Override
    public void setChanged() {
        this.rangeChanged = true;
        super.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return ModBlocks.ITEM_DETECTOR.get().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new ItemDetectorMenu(ModMenuTypes.ITEM_DETECTOR.get(), id, inventory, this);
    }

    @Override
    public FilteredItemStackHandler getFilteredItemStackHandler() {
        return DUMMY_HANDLER;
    }

    @Override
    public boolean isFilterEnabled() {
        return true;
    }

    @Override
    public void setFilterEnabled(boolean enable) {
    }

    @Override
    public boolean isSlotDisabled(int slot) {
        return false;
    }

    @Override
    public void setSlotDisabled(int slot, boolean disable) {
    }

    @Override
    public NonNullList<ItemStack> getFilteredItems() {
        return this.filter.getFilterList();
    }

    @Override
    public ItemStack getFilter(int slot) {
        if (slot < 0 || slot >= this.filter.getContainerSize()) return ItemStack.EMPTY;
        return this.filter.getItem(slot);
    }

    @Override
    public boolean setFilter(int slot, ItemStack filter) {
        if (slot < 0 || slot >= this.filter.getContainerSize()) return false;
        this.filter.setItem(slot, filter);
        return true;
    }

    @Override
    public AABB shape() {
        if (this.detectionRange == null && this.hasLevel()) {
            this.recalcDetectionRange();
        }
        return Optional.ofNullable(this.detectionRange).orElse(new AABB(this.getBlockPos()));
    }

    public void recalcDetectionRange() {
        this.detectionRange = this.calcDetectionRange();
        this.setChanged();
        if (this.level instanceof ServerLevel) {
            BlockPos pos = this.getBlockPos();
            BlockState state = this.level.getBlockState(pos);
            this.level.sendBlockUpdated(this.getBlockPos(), state, state, 2);
        }
    }

    @Nullable
    public AABB calcDetectionRange() {
        if (this.level == null) return null;
        BlockPos pos = this.getBlockPos();
        BlockState blockState = this.level.getBlockState(this.getBlockPos());
        if (!blockState.is(ModBlocks.ITEM_DETECTOR)) return null;
        Direction direction = blockState.getValue(ItemDetectorBlock.FACING);
        return AABB.encapsulatingFullBlocks(pos.relative(direction), pos.relative(direction, this.range));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void storeDiskData(CompoundTag data) {
        if (this.level == null) return;
        data.putInt("Range", this.range);
        data.putString("FilterMode", this.filterMode.toString());
        data.put("Filter", this.filter.serializeNBT(this.level.registryAccess()));
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        if (this.level == null) return;
        this.setRange(data.getInt("Range"));
        this.filterMode = Mode.valueOf(data.getString("FilterMode"));
        filter.deserializeNBT(this.level.registryAccess(), data.getCompound("Filter"));
        this.recalcDetectionRange();
    }

    public enum Mode {
        ANY("any"),
        ALL("all");

        public final String buttonPath;

        Mode(String buttonPath) {
            this.buttonPath = buttonPath;
        }

        public Mode cycle() {
            return this == ANY ? ALL : ANY;
        }
    }
}

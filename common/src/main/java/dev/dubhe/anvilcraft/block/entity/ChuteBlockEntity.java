package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import dev.dubhe.anvilcraft.block.ChuteBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.function.BooleanSupplier;

public class ChuteBlockEntity extends BaseMachineBlockEntity implements IFilterBlockEntity, Hopper {
    private int cooldown = 0;
    @Getter
    @Setter
    private boolean record = false;
    @Getter
    private final NonNullList<Boolean> disabled = this.getNewDisabled();
    @Getter
    private final NonNullList<ItemStack> filter = this.getNewFilter();

    public ChuteBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CHUTE.get(), pos, blockState);
    }

    public ChuteBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState, 9);
    }

    public boolean smallerStackExist(int count, ItemStack itemStack, int index) {
        for (int index2 = index + 1; index2 < 9; ++index2) {
            ItemStack itemStack1;
            if (this.getDisabled().get(index2) ||
                    (isRecord() && getFilter().get(index2).isEmpty()) ||
                    (!(itemStack1 = this.getItem(index2)).isEmpty() && (itemStack1.getCount() >= count) ||
                            !ItemStack.isSameItemSameTags(itemStack1, itemStack) ||
                            (itemStack1).isEmpty()))
                continue;
            return true;
        }
        return false;
    }

    @Override
    protected @Nonnull Component getDefaultName() {
        return Component.translatable("block.anvilcraft.chute");
    }

    @Override
    protected @Nonnull AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory inventory) {
        return new ChuteMenu(containerId, inventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockEntity e) {
        if (!(e instanceof ChuteBlockEntity entity)) return;
        entity.cooldown -= 1;
        if (entity.cooldown < 0) entity.refreshCooldown();
        BlockState state = level.getBlockState(pos);
        if (!state.getValue(ChuteBlock.ENABLED)) return;
        if (!entity.isOnCooldown()) {
            ChuteBlockEntity.tryMoveItems(level, pos, state, entity, () -> ChuteBlockEntity.suckInItems(level, entity));
            entity.dropOrInsert(level, pos);
        }
        level.updateNeighborsAt(pos, ModBlocks.AUTO_CRAFTER.get());
    }

    public void dropOrInsert(Level level, @NotNull BlockPos pos) {
        Direction direction = this.getDirection();
        ItemDepository itemDepository = ItemDepository.getItemDepository(level, pos.relative(direction), direction.getOpposite());
        for (int i = this.items.size() - 1; i >= 0; i--) {
            ItemStack stack = this.items.get(i);
            if (stack.isEmpty()) continue;
            if (this.outputItem(itemDepository, direction, level, pos, this, i, true, false, false, true)) return;
        }
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        this.cooldown = tag.getInt("cooldown");
        this.loadTag(tag);
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("cooldown", this.cooldown);
        this.saveTag(tag);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean tryMoveItems(@Nonnull Level level, BlockPos pos, BlockState state, ChuteBlockEntity blockEntity, BooleanSupplier validator) {
        if (level.isClientSide) {
            return false;
        }
        if (!blockEntity.isOnCooldown() && state.getValue(HopperBlock.ENABLED)) {
            boolean bl = false;
            if (!blockEntity.isEmpty()) {
                bl = ChuteBlockEntity.ejectItems(level, pos, state, blockEntity);
            }
            if (!blockEntity.inventoryFull()) {
                bl |= validator.getAsBoolean();
            }
            if (bl) {
                HopperBlockEntity.setChanged(level, pos, state);
                return true;
            }
        }
        return false;
    }

    private static boolean ejectItems(Level level, BlockPos pos, @NotNull BlockState state, Container sourceContainer) {
        if (!state.is(ModBlocks.CHUTE.get())) return false;
        Direction direction = state.getValue(ChuteBlock.FACING);
        ItemDepository depository = ItemDepository.getItemDepository(level, pos.relative(direction), direction.getOpposite());
        if (depository == null) return false;
        for (int i = 0; i < sourceContainer.getContainerSize(); ++i) {
            if (sourceContainer.getItem(i).isEmpty()) continue;
            ItemStack itemStack = sourceContainer.getItem(i).copy();
            sourceContainer.removeItem(i, itemStack.getCount());
            int count = (int) depository.inject(itemStack, itemStack.getCount(), false);
            itemStack.setCount(count);
            if (count == 0) return true;
            sourceContainer.setItem(i, itemStack);
        }
        return false;
    }

    public static boolean suckInItems(Level level, Hopper hopper) {
        Container container = HopperBlockEntity.getSourceContainer(level, hopper);
        if (container != null) {
            Direction direction = Direction.DOWN;
            if (HopperBlockEntity.isEmptyContainer(container, direction)) {
                return false;
            }
            return HopperBlockEntity.getSlots(container, direction).anyMatch(i -> ChuteBlockEntity.tryTakeInItemFromSlot(hopper, container, i, direction));
        }
        for (ItemEntity itemEntity : HopperBlockEntity.getItemsAtAndAbove(level, hopper)) {
            if (!HopperBlockEntity.addItem(hopper, itemEntity)) continue;
            return true;
        }
        return false;
    }

    private static boolean tryTakeInItemFromSlot(Hopper hopper, @Nonnull Container container, int slot, Direction direction) {
        ItemStack itemStack = container.getItem(slot);
        int amount = itemStack.getCount();
        if (!itemStack.isEmpty() && HopperBlockEntity.canTakeItemFromContainer(hopper, container, itemStack, slot, direction)) {
            ItemStack itemStack3 = HopperBlockEntity.addItem(container, hopper, itemStack, null);
            if (itemStack3.isEmpty()) {
                container.removeItem(slot, amount);
            } else {
                container.setItem(slot, itemStack3);
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isOnCooldown() {
        return this.cooldown > 0;
    }

    private boolean inventoryFull() {
        for (ItemStack itemStack : this.items) {
            if (!itemStack.isEmpty() && itemStack.getCount() == itemStack.getMaxStackSize()) continue;
            return false;
        }
        return true;
    }

    @Override
    public Direction getDirection() {
        if (this.level == null) return Direction.UP;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (state.is(ModBlocks.CHUTE.get())) return state.getValue(ChuteBlock.FACING);
        return Direction.UP;
    }

    @Override
    public void setDirection(Direction direction) {
        if (direction == Direction.UP) return;
        BlockPos pos = this.getBlockPos();
        Level level = this.getLevel();
        if (null == level) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.CHUTE.get())) return;
        level.setBlockAndUpdate(pos, state.setValue(ChuteBlock.FACING, direction));
    }

    private void refreshCooldown() {
        this.cooldown = AnvilCraft.config.chuteMaxCooldown;
    }

    @Override
    public double getLevelX() {
        return this.getBlockPos().getCenter().x;
    }

    @Override
    public double getLevelY() {
        return this.getBlockPos().getCenter().y;
    }

    @Override
    public double getLevelZ() {
        return this.getBlockPos().getCenter().z;
    }

    public int getRedstoneSignal() {
        int i = 0;
        for (int j = 0; j < this.getContainerSize(); ++j) {
            ItemStack itemStack = this.getItem(j);
            if (!itemStack.isEmpty() && !this.getDisabled().get(j)) {
                ++i;
            }
        }
        return i;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @Nonnull Inventory inventory, @Nonnull Player player) {
        return ChuteMenu.serverOf(i, inventory, this);
    }
}

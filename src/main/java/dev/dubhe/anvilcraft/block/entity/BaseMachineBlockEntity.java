package dev.dubhe.anvilcraft.block.entity;

import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class BaseMachineBlockEntity extends RandomizableContainerBlockEntity {
    protected NonNullList<ItemStack> items;
    @Setter
    protected Direction direction = Direction.DOWN;

    protected BaseMachineBlockEntity(BlockEntityType<? extends BaseMachineBlockEntity> type, BlockPos pos, BlockState blockState, int size) {
        super(type, pos, blockState);
        items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items);
        this.direction = Direction.valueOf(tag.getString("direction").toUpperCase());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.put("direction", StringTag.valueOf(this.direction.getName().toLowerCase()));
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.items) {
            if (itemStack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.items, slot, amount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemStacks) {
        this.items = itemStacks;
    }

    @SuppressWarnings("UnstableApiUsage")
    protected boolean insertOrDropItem(Direction direction, Level level, @NotNull BlockPos pos, @NotNull Container container, int slot, boolean drop, boolean momentum, boolean needEmpty) {
        ItemStack item = container.getItem(slot);
        BlockPos curPos = pos.relative(direction);
        boolean flag = false;
        if (canPlaceItem(level, curPos, item, direction)) {
            flag = this.insertItem(direction, level, curPos, container, slot);
            if (flag) return true;
        }
        if (!drop) {
            if (HopperBlockEntity.getContainerAt(level, curPos) != null) return false;
            if (ItemStorage.SIDED.find(level, curPos, direction.getOpposite()) != null) return false;
        }
        if (!needEmpty || level.isEmptyBlock(curPos)) {
            flag = this.dropItem(direction, level, pos, container, slot, momentum);
        }
        return flag;
    }

    @SuppressWarnings("UnstableApiUsage")
    private boolean insertItem(Direction direction, Level level, @NotNull BlockPos pos, @NotNull Container container, int slot) {
        ItemStack item = container.getItem(slot);
        Container outContainer = HopperBlockEntity.getContainerAt(level, pos);
        long count = item.getCount();
        if (null != outContainer) {
            ItemStack itemStack2 = HopperBlockEntity.addItem(container, outContainer, item, direction.getOpposite());
            count = itemStack2.getCount();
            container.setItem(slot, itemStack2);
            return count == 0;
        }
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, direction.getOpposite());
        if (target == null) return false;
        return StorageUtil.move(
                InventoryStorage.of(container, null).getSlot(slot),
                target,
                iv -> true,
                item.getCount(),
                null
        ) == count;
    }

    @SuppressWarnings("UnstableApiUsage")
    private boolean canPlaceItem(Level level, BlockPos pos, @NotNull ItemStack stack, Direction direction) {
        Container outContainer = HopperBlockEntity.getContainerAt(level, pos);
        int count = stack.getCount();
        if (null != outContainer) {
            for (int i = 0; i < outContainer.getContainerSize(); i++) {
                if (!outContainer.canPlaceItem(i, stack)) continue;
                ItemStack item = outContainer.getItem(i);
                if (item.isEmpty()) return true;
                if (!ItemStack.isSameItemSameTags(stack, item)) continue;
                count -= item.getMaxStackSize() - item.getCount();
                if (count <= 0) return true;
            }
            return count <= 0;
        }
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, direction.getOpposite());
        if (target == null) return false;
        if (!target.supportsInsertion()) return false;
        return StorageUtil.simulateInsert(target, ItemVariant.of(stack), count, null) == count;
    }

    private boolean dropItem(Direction direction, Level level, @NotNull BlockPos pos, @NotNull Container container, int slot, boolean momentum) {
        ItemStack item = container.getItem(slot);
        BlockPos out = pos.relative(direction);
        Vec3 vec3 = out.getCenter();
        Vec3 move = Vec3.ZERO;
        if (momentum && direction != Direction.UP && direction != Direction.DOWN) {
            vec3 = vec3.relative(direction, -0.25);
            vec3 = vec3.subtract(0.0, 0.25, 0.0);
        }
        if (momentum) move = out.getCenter().subtract(vec3);
        ItemEntity entity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, item, move.x, move.y, move.z);
        if (level.addFreshEntity(entity)) {
            container.setItem(slot, ItemStack.EMPTY);
            return true;
        }
        return false;
    }
}

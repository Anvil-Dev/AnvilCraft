package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import lombok.Getter;
import lombok.Setter;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nonnull;

@Getter
public abstract class BaseMachineBlockEntity extends RandomizableContainerBlockEntity {
    protected NonNullList<ItemStack> items;
    @Setter
    protected Direction direction = Direction.DOWN;

    protected BaseMachineBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState, int size) {
        super(type, pos, blockState);
        items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items);
        this.direction = Direction.valueOf(tag.getString("direction").toUpperCase());
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
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
    public @Nonnull ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public @Nonnull ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.items, slot, amount);
    }

    @Override
    public @Nonnull ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, @Nonnull ItemStack stack) {
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
    protected void setItems(@Nonnull NonNullList<ItemStack> itemStacks) {
        this.items = itemStacks;
    }

    protected boolean insertOrDropItem(Direction direction, Level level, @Nonnull BlockPos pos, @Nonnull Container container, int slot, boolean drop, boolean momentum, boolean needEmpty) {
        ItemStack item = container.getItem(slot);
        BlockPos curPos = pos.relative(direction);
        boolean flag = false;
        if (canPlaceItem(level, curPos, item, direction)) {
            flag = this.insertItem(direction, level, curPos, container, slot);
            if (flag) return true;
        }
        if (!drop) {
            if (ItemDepository.getItemDepository(level, pos, direction.getOpposite()) != null) return false;
        }
        if (!needEmpty || level.isEmptyBlock(curPos)) {
            flag = this.dropItem(direction, level, pos, container, slot, momentum);
        }
        return flag;
    }

    private boolean insertItem(@Nonnull Direction direction, Level level, @Nonnull BlockPos pos, @Nonnull Container container, int slot) {
        ItemStack item = container.getItem(slot);
        ItemDepository itemDepository = ItemDepository.getItemDepository(level, pos, direction.getOpposite());
        if (itemDepository == null) return false;
        long count = itemDepository.inject(item.copy(), item.getCount());
        item.setCount((int) count);
        container.setItem(slot, item);
        return true;
    }

    private boolean canPlaceItem(Level level, BlockPos pos, @Nonnull ItemStack stack, @Nonnull Direction direction) {
        ItemDepository itemDepository = ItemDepository.getItemDepository(level, pos, direction.getOpposite());
        if (itemDepository == null) return false;
        return itemDepository.canInject(stack.copy(), stack.getCount());
    }

    private boolean dropItem(Direction direction, Level level, @Nonnull BlockPos pos, @Nonnull Container container, int slot, boolean momentum) {
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

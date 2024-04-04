package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
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
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class BaseMachineBlockEntity extends RandomizableContainerBlockEntity {
    protected NonNullList<ItemStack> items;

    protected BaseMachineBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState, int size) {
        super(type, pos, blockState);
        items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public abstract Direction getDirection();

    public abstract void setDirection(Direction direction);

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
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
    public void setItem(int slot, @NotNull ItemStack stack) {
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
    protected void setItems(@NotNull NonNullList<ItemStack> itemStacks) {
        this.items = itemStacks;
    }

    /**
     * 输出或丢出物品
     *
     * @param direction 输出目标方向
     * @param level     维度
     * @param pos       坐标
     * @param container 输出物品的容器
     * @param slot      从哪个槽位输出
     * @param part      是否允许只插入一部分物品
     * @param drop      是否强制丢出物品
     * @param momentum  是否有动量
     * @param needEmpty 丢出物品的位置是否需要为空
     * @return 操作是否成功
     */
    protected final boolean insertOrDropItem(Direction direction, Level level, @NotNull BlockPos pos, @NotNull Container container, int slot, boolean part, boolean drop, boolean momentum, boolean needEmpty) {
        ItemStack item = container.getItem(slot);
        BlockPos curPos = pos.relative(direction);
        boolean flag = false;
        if ((part && getCountCanPlace(level, curPos, item, direction) > 0) || canPlaceAllItem(level, curPos, item, direction)) {
            flag = this.insertItem(direction, level, curPos, container, slot);
            if (flag) return true;
        }
        if (!drop) {
            if (ItemDepository.getItemDepository(level, curPos, direction.getOpposite()) != null) return false;
        }
        if (!needEmpty || this.canDropItem(level, curPos)) {
            flag = this.dropItem(direction, level, pos, container, slot, momentum);
        }
        return flag;
    }

    private boolean canDropItem(@NotNull Level level, @NotNull BlockPos pos) {
        Vec3 vec3 = pos.getCenter();
        ItemEntity entity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, ItemStack.EMPTY);
        return level.noCollision(entity);
    }

    private boolean insertItem(@NotNull Direction direction, @NotNull Level level, @NotNull BlockPos pos, @NotNull Container container, int slot) {
        ItemStack item = container.getItem(slot);
        ItemDepository itemDepository = ItemDepository.getItemDepository(level, pos, direction.getOpposite());
        if (itemDepository == null) return false;
        long count = itemDepository.inject(item.copy(), item.getCount(), false);
        item.setCount((int) count);
        container.setItem(slot, item);
        return true;
    }

    public final long getCountCanPlace(Level level, BlockPos pos, @NotNull ItemStack stack, @NotNull Direction direction) {
        ItemDepository itemDepository = ItemDepository.getItemDepository(level, pos, direction.getOpposite());
        if (itemDepository == null) return 0;
        int count = stack.getCount();
        return count - itemDepository.inject(stack.copy(), count, true);
    }

    private boolean canPlaceAllItem(Level level, BlockPos pos, @NotNull ItemStack stack, @NotNull Direction direction) {
        ItemDepository itemDepository = ItemDepository.getItemDepository(level, pos, direction.getOpposite());
        if (itemDepository == null) return false;
        return itemDepository.canInject(stack.copy(), stack.getCount());
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

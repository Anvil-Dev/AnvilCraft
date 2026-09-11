package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.block.ItemSplitterBlock;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品分配器的方块实体。
 *
 * <p>内部有 {@value #SLOT_COUNT} 格容量且只能容纳同一种物品。正前方有容器时每
 * {@value #SPLIT_INTERVAL} gt 把内部物品按方块均分给前方连成一线的容器；
 * 正前方没有容器时被铁砧砸到，则按铁砧下落高度均分到前方没有遮挡的空间中。</p>
 *
 * <p>两种均分都采用严格计算，除不尽的余数留在自身内部。</p>
 */
public class ItemSplitterBlockEntity extends BlockEntity implements IItemHandlerHolder {
    /**
     * 内部容量（格）
     */
    public static final int SLOT_COUNT = 16;
    /**
     * 主动均分间隔（gt）
     */
    public static final int SPLIT_INTERVAL = 8;
    /**
     * 均分时最远选取距离（方块）
     */
    public static final int MAX_DISTANCE = 16;

    @Getter
    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        /**
         * 只能容纳同一种物品：内部已有其它物品时全部拒收。
         */
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            for (int i = 0; i < this.getSlots(); i++) {
                ItemStack existing = this.getStackInSlot(i);
                if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
                    return stack;
                }
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int cooldown = 0;

    public ItemSplitterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Cooldown", this.cooldown);
        tag.put("Inventory", this.itemHandler.serializeNBT(provider));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.cooldown = tag.getInt("Cooldown");
        this.itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
    }

    /**
     * 每 {@value #SPLIT_INTERVAL} gt 尝试向正前方的容器均分一次。
     */
    public void tick() {
        if (this.level == null || this.level.isClientSide) return;
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.cooldown > 0) return;
        this.cooldown = SPLIT_INTERVAL;
        this.splitToContainers();
    }

    /**
     * 把内部物品按方块均分给正前方连成一线的容器。
     *
     * <p>只有一个方向上的、连续的容器会被选取；遇到断开或不是容器的方块就停止。
     * 容器占多个格子就按多个格子计数，因此沿选取方向放置的大箱子会被选取两次。</p>
     */
    public void splitToContainers() {
        if (this.level == null) return;
        List<BlockPos> targets = this.collectContainerTargets();
        if (targets.isEmpty()) return;
        int share = this.getTotalCount() / targets.size();
        if (share <= 0) return;
        for (BlockPos target : targets) {
            IItemHandler handler = this.getItemHandlerAt(target);
            if (handler == null) continue;
            ItemStack portion = this.extractTotal(share);
            if (portion.isEmpty()) break;
            ItemStack leftover = ItemHandlerHelper.insertItem(handler, portion, false);
            this.insertBack(leftover);
        }
    }

    /**
     * 把内部物品均分到正前方没有遮挡的空间中（铁砧砸到时触发）。
     *
     * <p>份数即铁砧下落高度；遇到遮挡就往前顺延，最远 {@value #MAX_DISTANCE} 格，
     * 未能落位的份额留在自身内部。</p>
     *
     * @param shares 均分份数
     * @return 是否有物品被分出
     */
    public boolean splitToSpace(int shares) {
        if (this.level == null || shares <= 0) return false;
        // 正前方有容器时由主动均分负责，铁砧不介入
        if (this.getItemHandlerAt(this.getBlockPos().relative(this.getFacing())) != null) return false;
        List<BlockPos> targets = this.collectSpaceTargets(shares);
        if (targets.isEmpty()) return false;
        // 严格计算：除不尽或没能落位的份额都留在自身
        int share = this.getTotalCount() / shares;
        if (share <= 0) return false;
        boolean moved = false;
        for (BlockPos target : targets) {
            ItemStack portion = this.extractTotal(share);
            if (portion.isEmpty()) break;
            this.dropAt(target, portion);
            moved = true;
        }
        return moved;
    }

    /**
     * 收集正前方连成一线的容器位置。
     *
     * @return 容器位置列表，可能为空
     */
    private List<BlockPos> collectContainerTargets() {
        List<BlockPos> targets = new ArrayList<>();
        Direction facing = this.getFacing();
        BlockPos origin = this.getBlockPos();
        for (int i = 1; i <= MAX_DISTANCE; i++) {
            BlockPos target = origin.relative(facing, i);
            if (this.getItemHandlerAt(target) == null) break;
            targets.add(target);
        }
        return targets;
    }

    /**
     * 收集正前方没有遮挡的空间位置。
     *
     * @param shares 需要收集的份数
     * @return 空间位置列表，数量不超过份数
     */
    private List<BlockPos> collectSpaceTargets(int shares) {
        List<BlockPos> targets = new ArrayList<>();
        Direction facing = this.getFacing();
        BlockPos origin = this.getBlockPos();
        for (int i = 1; i <= MAX_DISTANCE && targets.size() < shares; i++) {
            BlockPos target = origin.relative(facing, i);
            // 有遮挡则往前顺延
            if (!this.isUnobstructed(target)) continue;
            targets.add(target);
        }
        return targets;
    }

    /**
     * 判断某格是否可以把物品吐出，判定方式与溜槽出口一致。
     *
     * @param target 目标位置
     * @return 该格中心没有碰撞体积时返回 {@code true}
     */
    private boolean isUnobstructed(BlockPos target) {
        if (this.level == null) return false;
        Vec3 center = target.getCenter();
        AABB aabb = new AABB(
            center.add(-0.125, -0.125, -0.125),
            center.add(0.125, 0.125, 0.125)
        );
        return this.level.noCollision(aabb);
    }

    /**
     * 取得某格容器的物品能力。
     *
     * @param target 目标位置
     * @return 物品能力，没有容器时返回 {@code null}
     */
    @Nullable
    private IItemHandler getItemHandlerAt(BlockPos target) {
        if (this.level == null) return null;
        return this.level.getCapability(
            Capabilities.ItemHandler.BLOCK,
            target,
            this.getFacing().getOpposite()
        );
    }

    /**
     * 内部物品总数。
     *
     * @return 所有格子的物品数量之和
     */
    public int getTotalCount() {
        int total = 0;
        for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) total += stack.getCount();
        }
        return total;
    }

    /**
     * 从内部取出指定数量的物品。
     *
     * @param amount 取出数量上限
     * @return 取出的物品，可能为空
     */
    private ItemStack extractTotal(int amount) {
        ItemStack result = ItemStack.EMPTY;
        int remaining = amount;
        for (int i = 0; i < this.itemHandler.getSlots() && remaining > 0; i++) {
            ItemStack extracted = this.itemHandler.extractItem(i, remaining, false);
            if (extracted.isEmpty()) continue;
            if (result.isEmpty()) {
                result = extracted.copy();
            } else {
                result.grow(extracted.getCount());
            }
            remaining -= extracted.getCount();
        }
        return result;
    }

    /**
     * 把未能送出的物品放回内部。
     *
     * @param stack 待放回的物品
     */
    private void insertBack(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack leftover = ItemHandlerHelper.insertItem(this.itemHandler, stack, false);
        if (leftover.isEmpty() || this.level == null) return;
        Vec3 center = this.getBlockPos().getCenter();
        Containers.dropItemStack(this.level, center.x, center.y, center.z, leftover);
    }

    /**
     * 在指定位置以零动量抛出一份物品。
     *
     * @param target 目标位置
     * @param stack  待抛出的物品
     */
    private void dropAt(BlockPos target, ItemStack stack) {
        if (this.level == null) return;
        Vec3 center = target.getCenter();
        // 显式传 0,0,0：5 参数构造器会自带随机动量与向上初速
        ItemEntity itemEntity = new ItemEntity(
            this.level,
            center.x,
            center.y,
            center.z,
            stack,
            0,
            0,
            0
        );
        itemEntity.setDefaultPickUpDelay();
        this.level.addFreshEntity(itemEntity);
    }

    /**
     * 当前朝向，即均分方向。
     *
     * @return 方块朝向
     */
    public Direction getFacing() {
        BlockState state = this.getBlockState();
        if (state.getBlock() instanceof ItemSplitterBlock && state.hasProperty(ItemSplitterBlock.FACING)) {
            return state.getValue(ItemSplitterBlock.FACING);
        }
        return Direction.NORTH;
    }
}

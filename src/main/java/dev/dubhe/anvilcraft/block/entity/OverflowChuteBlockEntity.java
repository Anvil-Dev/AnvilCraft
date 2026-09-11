package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.OverflowChuteBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 溢流溜槽的方块实体。
 *
 * <p>出口能输出时物品照常走出口；出口堵住时把物品尽量均分地从各个溢流口送出。
 * 溢流口优先直接送入前方容器，没有容器时才作为掉落物抛出且不带动量。</p>
 */
public class OverflowChuteBlockEntity extends BaseChuteBlockEntity {
    /**
     * 溢流溜槽只存一组物品
     */
    private static final int SLOT_COUNT = 1;

    public OverflowChuteBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState, SLOT_COUNT);
    }

    @Override
    public void tick() {
        this.sanitizeOverflowPorts();
        super.tick();
    }

    /**
     * 出入口方向不可能有溢流口，把这类无效状态清除掉。
     */
    private void sanitizeOverflowPorts() {
        if (this.level == null || this.level.isClientSide) return;
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof OverflowChuteBlock)) return;
        BlockState sanitized = OverflowChuteBlock.sanitizeOverflowPorts(state);
        if (sanitized != state) {
            this.level.setBlock(this.getBlockPos(), sanitized, 2);
        }
    }

    /**
     * 当前开启的溢流口方向，出入口方向永远不会出现在其中。
     *
     * @return 溢流口方向列表
     */
    public List<Direction> getOverflowPorts() {
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof OverflowChuteBlock)) return List.of();
        Direction output = this.getOutputDirection();
        List<Direction> ports = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (direction == output || direction == output.getOpposite()) continue;
            if (state.getValue(OverflowChuteBlock.overflowProperty(direction))) {
                ports.add(direction);
            }
        }
        return ports;
    }

    @Override
    protected boolean tryOverflowOutput() {
        List<Direction> ports = this.getOverflowPorts();
        if (ports.isEmpty()) return false;
        boolean moved = false;
        for (int slot = 0; slot < this.getItemHandler().getSlots(); slot++) {
            ItemStack stack = this.getItemHandler().getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            int remaining = stack.getCount();
            for (int i = 0; i < ports.size() && remaining > 0; i++) {
                // 均分给尚未处理的溢流口；某个口送不出去时由后面的口补上，只求尽量清空
                int share = Math.max(1, remaining / (ports.size() - i));
                remaining -= this.pushToPort(stack.copyWithCount(Math.min(share, remaining)), ports.get(i));
            }
            if (remaining == stack.getCount()) continue;
            this.getItemHandler().setStackInSlot(
                slot,
                remaining <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining)
            );
            moved = true;
        }
        return moved;
    }

    /**
     * 把一份物品送往某个溢流口：优先直接送入前方容器，没有容器时才作为掉落物抛出。
     *
     * @param stack     待送出的物品，其数量即为本次上限
     * @param direction 溢流口方向
     * @return 实际送出的数量
     */
    private int pushToPort(ItemStack stack, Direction direction) {
        if (this.level == null) return 0;
        BlockPos targetPos = this.getBlockPos().relative(direction);
        List<IItemHandler> targets = ItemHandlerUtil.getTargetItemHandlerList(
            targetPos,
            direction.getOpposite(),
            this.level
        );
        if (targets == null || targets.isEmpty()) {
            return this.dropWithoutMomentum(stack, direction) ? stack.getCount() : 0;
        }
        BlockEntity targetEntity = this.level.getBlockEntity(targetPos);
        BlockEntity cdTarget = targetEntity != null && this.isTargetEmpty(targetEntity) ? targetEntity : null;
        int amount = stack.getCount();
        int moved = 0;
        for (IItemHandler target : targets) {
            ItemStack leftover = ItemHandlerHelper.insertItem(target, stack.copyWithCount(amount - moved), false);
            moved = amount - leftover.getCount();
            if (moved >= amount) break;
        }
        if (moved > 0 && cdTarget != null) this.setChuteCD(cdTarget);
        return moved;
    }

    /**
     * 在溢流口位置抛出物品，不带任何初始动量。
     *
     * @param stack     待抛出的物品
     * @param direction 溢流口方向
     * @return 是否成功抛出
     */
    private boolean dropWithoutMomentum(ItemStack stack, Direction direction) {
        if (this.level == null) return false;
        Vec3 center = this.getBlockPos().relative(direction).getCenter();
        AABB aabb = new AABB(
            center.add(-0.125, -0.125, -0.125),
            center.add(0.125, 0.125, 0.125)
        );
        if (!this.level.noCollision(aabb)) return false;
        // 必须显式传 0,0,0：5 参数构造器会自带随机横向动量与 0.2 的向上初速
        ItemEntity itemEntity = new ItemEntity(
            this.level,
            center.x,
            center.y,
            center.z,
            stack.copy(),
            0,
            0,
            0
        );
        itemEntity.setDefaultPickUpDelay();
        this.level.addFreshEntity(itemEntity);
        return true;
    }

    @Override
    protected boolean shouldSkipDirection(Direction direction) {
        return false;
    }

    @Override
    protected boolean validateBlockState(BlockState state) {
        return state.is(ModBlocks.OVERFLOW_CHUTE.get());
    }

    @Override
    protected boolean isEnabled() {
        return this.getBlockState().getValue(OverflowChuteBlock.ENABLED);
    }

    /**
     * 红石只关闭主出口，吸取物品不受影响。
     */
    @Override
    protected boolean isInputEnabled() {
        return true;
    }

    @Override
    protected DirectionProperty getFacingProperty() {
        return OverflowChuteBlock.FACING;
    }

    @Override
    protected Direction getOutputDirection() {
        return this.getDirection();
    }

    @Override
    protected Direction getInputDirection() {
        return this.getOutputDirection().getOpposite();
    }

    @Override
    protected void applySpeed(ItemEntity itemEntity, Direction direction) {
        itemEntity.setDeltaMovement(MagneticChuteBlockEntity.getOutputSpeed(direction));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.overflow_chute");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return null;
    }
}

package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
public abstract class BaseChuteBlockEntity
    extends BaseMachineBlockEntity
    implements IFilterBlockEntity, IDiskCloneable, IItemResourceHandlerHolder {
    private static final int EJECTED_ITEM_TRACK_TICKS = 20;

    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            assert BaseChuteBlockEntity.this.level != null;
            if (BaseChuteBlockEntity.this.level.isClientSide()) return;
            BaseChuteBlockEntity.this.setChanged();
        }
    };
    @Setter
    private int cooldown = 0;
    private long tickedGameTime;
    private final List<TrackedEjectedItem> trackedEjectedItems = new ArrayList<>();

    private static final class TrackedEjectedItem {
        private final ItemEntity item;
        private boolean wasOnGround;
        private int ticksLeft;

        private TrackedEjectedItem(ItemEntity item) {
            this.item = item;
            this.wasOnGround = item.onGround();
            this.ticksLeft = BaseChuteBlockEntity.EJECTED_ITEM_TRACK_TICKS;
        }
    }

    protected BaseChuteBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public Direction getDirection() {
        if (this.level == null) return Direction.UP;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (this.validateBlockState(state)) return state.getValue(this.getFacingProperty());
        return Direction.UP;
    }

    @Override
    public void setDirection(Direction direction) {
        if (this.shouldSkipDirection(direction)) return;
        BlockPos pos = this.getBlockPos();
        Level level = this.getLevel();
        if (null == level) return;
        BlockState state = level.getBlockState(pos);
        if (!this.validateBlockState(state)) return;
        level.setBlockAndUpdate(pos, state.setValue(this.getFacingProperty(), direction));
    }

    protected abstract boolean shouldSkipDirection(Direction direction);

    protected abstract boolean validateBlockState(BlockState state);

    protected abstract EnumProperty<Direction> getFacingProperty();

    protected abstract Direction getOutputDirection();

    protected abstract Direction getInputDirection();

    protected abstract boolean isEnabled();

    @Override
    public FilteredItemStackHandler getFilteredItemStackHandler() {
        return this.itemHandler;
    }

    @Override
    public abstract Component getDisplayName();

    @Nullable
    @Override
    public abstract AbstractContainerMenu createMenu(int i, Inventory inventory, Player player);

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Cooldown", this.cooldown);
        this.itemHandler.serialize(output.child("Inventory"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.cooldown = input.getIntOr("Cooldown", 0);
        this.itemHandler.deserialize(input.childOrEmpty("Inventory"));
    }

    /// 溜槽 tick
    public void tick() {
        if (this.level == null) return;
        this.tickEjectedItemTracking();
        if (this.cooldown > 0) this.cooldown--;
        this.tickedGameTime = this.level.getGameTime();
        boolean resetCD = false;
        if (this.cooldown > 0) {
            this.level.updateNeighbourForOutputSignal(this.getBlockPos(), this.getBlockState().getBlock());
            return;
        }
        if (!this.isEnabled()) {
            this.level.updateNeighbourForOutputSignal(this.getBlockPos(), this.getBlockState().getBlock());
            return;
        }
        BlockPos targetPos = this.getBlockPos().relative(this.getOutputDirection());
        // 尝试向朝向容器输出
        List<ResourceHandler<ItemResource>> targetList = ItemHandlerUtil.getTargetItemHandlerList(
            targetPos,
            this.getOutputDirection().getOpposite(),
            this.level
        );
        if (targetList != null && !targetList.isEmpty()) {
            for (ResourceHandler<ItemResource> target : targetList) {
                BlockEntity targetBE = this.level.getBlockEntity(targetPos);
                boolean setChuteCD = targetBE != null && this.isTargetEmpty(targetBE);
                boolean success = ItemHandlerUtil.exportToTarget(this.getItemHandler(), 64, (_, _) -> true, target);
                if (success) {
                    // 特判溜槽cd7gt
                    if (setChuteCD) this.setChuteCD(targetBE);
                    resetCD = true;
                    break;
                }
            }
        } else {
            Vec3 center = this.getBlockPos().relative(this.getOutputDirection()).getCenter();
            AABB aabb = new AABB(center.add(-0.125, -0.125, -0.125), center.add(0.125, 0.125, 0.125));
            if (Objects.requireNonNull(this.getLevel()).noCollision(aabb)) {
                List<ItemEntity> itemEntities = this.getLevel().getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(this.getBlockPos().relative(this.getOutputDirection())).expandTowards(0, -0.5, 0),
                    itemEntity -> !itemEntity.getItem().isEmpty()
                );
                for (int i = 0; i < this.itemHandler.size(); i++) {
                    ItemResource resource = this.itemHandler.getResource(i);
                    if (resource.isEmpty()) continue;
                    int slotLimit = this.itemHandler.getSlotLimit(i);
                    int sameItemCount = 0;
                    for (ItemEntity entity : itemEntities) {
                        if (entity.getItem().getItem() != resource.getItem()) continue;
                        sameItemCount += entity.getItem().getCount();
                    }
                    if (sameItemCount >= slotLimit) continue;
                    int accessible = this.itemHandler.getAmountAsInt(i);
                    int dropping = Math.min(accessible, slotLimit - sameItemCount);
                    ItemStack remaining = resource.toStack(dropping);
                    if (resource.isEmpty()) remaining = ItemStack.EMPTY;
                    ItemEntity itemEntity = new ItemEntity(
                        this.getLevel(),
                        center.x,
                        center.y,
                        center.z,
                        remaining,
                        0,
                        0,
                        0
                    );
                    this.applySpeed(itemEntity, this.getOutputDirection());
                    itemEntity.setDefaultPickUpDelay();
                    this.getLevel().addFreshEntity(itemEntity);
                    this.trackEjectedItem(itemEntity);
                    this.itemHandler.set(i, resource, accessible - dropping);
                    resetCD = true;
                    break;
                }
            }
        }
        // 尝试从上方容器输入
        if (this.inventoryFull()) {
            this.level.updateNeighbourForOutputSignal(this.getBlockPos(), this.getBlockState().getBlock());
            if (resetCD) this.cooldown = AnvilCraft.CONFIG.chuteMaxCooldown;
            return;
        }
        ResourceHandler<ItemResource> source = ItemHandlerUtil.getSourceItemHandler(
            this.getBlockPos().relative(this.getInputDirection()),
            this.getInputDirection().getOpposite(),
            this.level
        );
        if (source != null) {
            resetCD |= ItemHandlerUtil.importFromTarget(this.getItemHandler(), 64, (_, _) -> true, source);
        } else {
            List<ItemEntity> itemEntities = Objects.requireNonNull(this.getLevel()).getEntitiesOfClass(
                ItemEntity.class,
                new AABB(this.getBlockPos().relative(this.getInputDirection())),
                itemEntity -> !itemEntity.getItem().isEmpty()
            );
            for (ItemEntity item : itemEntities) {
                ItemStack stack = item.getItem();
                int inserted;
                try (Transaction transaction = Transaction.openRoot()) {
                    inserted = this.getItemHandler().insert(ItemResource.of(stack), stack.count(), transaction);
                    if (inserted <= 0) {
                        continue;
                    }
                    transaction.commit();
                    if (inserted == stack.count()) {
                        item.discard();
                    } else {
                        item.setItem(stack.copyWithCount(stack.count() - inserted));
                    }
                }
                resetCD = true;
            }
        }
        this.level.updateNeighbourForOutputSignal(this.getBlockPos(), this.getBlockState().getBlock());
        if (resetCD) this.cooldown = AnvilCraft.CONFIG.chuteMaxCooldown;
    }

    private boolean isTargetEmpty(BlockEntity blockEntity) {
        if (blockEntity instanceof SimpleChuteBlockEntity chute) {
            return chute.isEmpty();
        }
        if (blockEntity instanceof BaseChuteBlockEntity chute) {
            return chute.isEmpty();
        }
        return false;
    }

    private void setChuteCD(BlockEntity targetBE) {
        if (targetBE instanceof BaseChuteBlockEntity chute) {
            int k = 0;
            if (chute.getTickedGameTime() >= this.tickedGameTime) k++;
            chute.setCooldown(AnvilCraft.CONFIG.chuteMaxCooldown - k);
        }
        if (targetBE instanceof SimpleChuteBlockEntity chute) {
            int k = 0;
            if (chute.getTickedGameTime() >= this.tickedGameTime) k++;
            chute.setCooldown(AnvilCraft.CONFIG.chuteMaxCooldown - k);
        }
    }

    /// 获取红石信号强度
    ///
    /// @return 红石信号强度
    public int getRedstoneSignal() {
        int strength = 0;
        for (int index = 0; index < this.itemHandler.size(); index++) {
            ItemResource resource = this.itemHandler.getResource(index);
            // 槽位为未设置过滤的已禁用槽位
            if (this.itemHandler.isSlotDisabled(index) && !this.itemHandler.isFilterEnabled()) {
                strength++;
                continue;
            }
            // 槽位上没有物品
            if (resource.isEmpty()) continue;
            strength++;
        }
        return strength;
    }

    protected void applySpeed(ItemEntity itemEntity, Direction direction) {

    }

    private void trackEjectedItem(ItemEntity itemEntity) {
        if (this.level == null || this.level.isClientSide()) return;
        if (itemEntity.getDeltaMovement().horizontalDistanceSqr() < 1.0E-7) return;
        this.trackedEjectedItems.add(new TrackedEjectedItem(itemEntity));
    }

    private void tickEjectedItemTracking() {
        if (!(this.level instanceof ServerLevel serverLevel) || this.trackedEjectedItems.isEmpty()) return;
        Iterator<TrackedEjectedItem> iterator = this.trackedEjectedItems.iterator();
        while (iterator.hasNext()) {
            TrackedEjectedItem tracked = iterator.next();
            ItemEntity item = tracked.item;
            if (!item.isAlive()) {
                iterator.remove();
                continue;
            }
            boolean onGround = item.onGround();
            if (onGround && !tracked.wasOnGround) {
                serverLevel.getChunkSource().sendToTrackingPlayersAndSelf(
                    item,
                    ClientboundTeleportEntityPacket.teleport(
                        item.getId(), PositionMoveRotation.of(item), Set.of(), item.onGround()
                    )
                );
                serverLevel.getChunkSource().sendToTrackingPlayersAndSelf(
                    item,
                    new ClientboundSetEntityMotionPacket(item)
                );
                iterator.remove();
                continue;
            }
            tracked.wasOnGround = onGround;
            if (--tracked.ticksLeft <= 0) {
                iterator.remove();
            }
        }
    }

    @Override
    public void storeDiskData(ValueOutput output) {
        this.itemHandler.serializeFiltering(output.child("Filtering"));
    }

    @Override
    public void applyDiskData(ValueInput input) {
        this.itemHandler.deserializeFiltering(input.childOrEmpty("Filtering"));
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public List<String> getDiskCompatibleGroups() {
        return List.of("anvilcraft:has_filter");
    }

    public boolean isEmpty() {
        for (int i = 0; i < this.itemHandler.size(); i++) {
            if (!this.itemHandler.getResource(i).isEmpty()) return false;
        }
        return true;
    }

    private boolean inventoryFull() {
        for (int i = 0; i < this.itemHandler.size(); i++) {
            ItemResource resource = this.itemHandler.getResource(i);
            if (resource.isEmpty() || this.itemHandler.getAmountAsInt(i) < resource.getMaxStackSize()) return false;
        }
        return true;
    }

    /// 获取更新标签 (由服务端调用，决定发给客户端什么数据)
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(new ProblemReporter.Collector(this.problemPath()), registries);
        this.saveAdditional(output);
        return output.buildResult();
    }

    /// 获取更新数据包 (区块加载时调用)
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        this.loadAdditional(input);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.itemHandler.getStacks());
        }
    }
}

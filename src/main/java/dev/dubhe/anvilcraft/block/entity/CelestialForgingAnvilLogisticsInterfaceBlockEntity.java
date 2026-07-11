package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 锻星砧物流接口。
 * 最多存储 16 种物品，每种物品固定占用一个槽位，不会溢出到其他槽位。
 * 主动模式由铁砧锤切换，会朝接口朝向自动输出物品。
 */
public class CelestialForgingAnvilLogisticsInterfaceBlockEntity extends BlockEntity implements IItemResourceHandlerHolder {
    private static final int TYPE_COUNT = 16;
    @Setter
    private boolean syncing = false; // 防止虫洞同步重入
    // 同一游戏刻内的多次库存和提示状态变化合并为一个完整更新包。
    private boolean clientSyncPending = false;

    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(TYPE_COUNT) {
        @Override
        public boolean isValid(int slot, ItemResource resource) {
            ItemResource current = this.getResource(slot);
            if (current.isEmpty()) {
                for (int i = 0; i < TYPE_COUNT; i++) {
                    if (i != slot) {
                        ItemResource other = this.getResource(i);
                        if (!other.isEmpty() && ItemStack.isSameItemSameComponents(other.toStack(), resource.toStack())) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return ItemStack.isSameItemSameComponents(current.toStack(), resource.toStack());
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack stack) {
            CelestialForgingAnvilLogisticsInterfaceBlockEntity.this.setChanged();
            if (!CelestialForgingAnvilLogisticsInterfaceBlockEntity.this.syncing) {
                CelestialForgingAnvilLogisticsInterfaceBlockEntity.this.triggerWormholeSync(slot);
            }
        }
    };

    public CelestialForgingAnvilLogisticsInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public CelestialForgingAnvilLogisticsInterfaceBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE.get(), pos, blockState);
    }

    public static CelestialForgingAnvilLogisticsInterfaceBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState state
    ) {
        return new CelestialForgingAnvilLogisticsInterfaceBlockEntity(type, pos, state);
    }

    // === 网络同步 ===

    /**
     * 将方块实体数据同步给所有正在追踪此区块的客户端。
     */
    public void syncToClients() {
        CfaBlockEntitySync.sendToTracking(this, this.getUpdatePacket());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            this.clientSyncPending = true;
        }
    }

    private void flushClientSync() {
        if (!this.clientSyncPending) return;
        this.clientSyncPending = false;
        this.syncToClients();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @SuppressWarnings("unused")
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.itemHandler;
    }

    // === 虫洞同步 ===

    /**
     * 玩家插入或取出物品时由 {@code onContentsChanged} 调用。
     * 立即通知所属锻星砧，把本接口的变更在同一刻写入虫洞权威状态并推送到其他锻星砧。
     */
    private void triggerWormholeSync(int changedSlot) {
        if (level == null || level.isClientSide()) return;
        BlockPos cfaPos = this.findParentCfa();
        if (cfaPos == null) return;
        if (level.getBlockEntity(cfaPos) instanceof CelestialForgingAnvilBlockEntity cfa) {
            cfa.syncLogisticsOnChange(worldPosition, changedSlot);
        }
    }

    /**
     * 沿接口朝向的反方向查找所属锻星砧控制器。
     * 接口始终背向锻星砧，相邻方块应为锻星砧部件，再通过 HALF 偏移定位到底部中心控制器。
     */
    @Nullable
    private BlockPos findParentCfa() {
        if (level == null) return null;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof CelestialForgingAnvilInterfaceBlock)) return null;
        Direction towardsCfa = state.getValue(CelestialForgingAnvilInterfaceBlock.FACING).getOpposite();
        BlockPos cfaBlockPos = worldPosition.relative(towardsCfa);
        BlockState cfaState = level.getBlockState(cfaBlockPos);
        if (cfaState.getBlock() instanceof CelestialForgingAnvilBlock) {
            Cube323PartHalf half = cfaState.getValue(CelestialForgingAnvilBlock.HALF);
            BlockPos controllerPos = cfaBlockPos.offset(half.getOffset().multiply(-1));
            if (level.getBlockEntity(controllerPos) instanceof CelestialForgingAnvilBlockEntity) {
                return controllerPos;
            }
        }
        return null;
    }

    // === 自动输出 ===

    private static final int MAX_EJECT_PER_OP = 64; // 每次最多输出一组
    public static final int EJECT_COOLDOWN = 8;     // 两次输出间隔 8 游戏刻

    private int ejectCooldown = 0;
    private int lastEjectSlot = 0;

    /// 服务器端 tick：在主动模式（由铁砧锤切换的 ACTIVE 属性，而非红石信号）下，
    /// 每 8gt 向 FACING 方向从内部物品栏自动弹出物品，每次最多 1 组，
    /// 弹射速度类似磁力滑槽（MagneticChute）。槽位轮询以避免某一槽被饿死。
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        try {
            BlockState state = getBlockState();
            if (!state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;
            if (!state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;

            if (this.ejectCooldown > 0) {
                this.ejectCooldown--;
                return;
            }

            Direction facing = state.getValue(CelestialForgingAnvilInterfaceBlock.FACING);
            BlockPos targetPos = worldPosition.relative(facing);
            boolean ejected = false;
            int totalSlots = this.itemHandler.size();

            // 从上次输出槽位开始轮询，避免靠后槽位长期得不到处理。
            for (int offset = 0; offset < totalSlots; offset++) {
                int slot = (this.lastEjectSlot + offset) % totalSlots;
                ItemResource resource = this.itemHandler.getResource(slot);
                if (resource.isEmpty()) continue;
                int amount = this.itemHandler.getAmountAsInt(slot);
                int toExtract = Math.min(amount, MAX_EJECT_PER_OP);
                ItemStack stackToMove = resource.toStack(toExtract);

                // 优先尝试插入正前方容器。
                ResourceHandler<ItemResource> targetHandler = level.getCapability(
                    Capabilities.Item.BLOCK, targetPos, facing.getOpposite()
                );
                if (targetHandler != null) {
                    ItemStack remainder = ItemHandlerUtil.insertItem(targetHandler, stackToMove, false);
                    int inserted = toExtract - remainder.getCount();
                    if (inserted > 0) {
                        try (Transaction tx = Transaction.openRoot()) {
                            this.itemHandler.extract(slot, resource, inserted, tx);
                            tx.commit();
                        }
                    }
                    if (remainder.getCount() < stackToMove.getCount()) {
                        ejected = true;
                        this.lastEjectSlot = (slot + 1) % totalSlots;
                        break;
                    }
                } else {
                    // 前方没有容器时，将物品以朝向速度弹入世界。
                    try (Transaction tx = Transaction.openRoot()) {
                        int extracted = this.itemHandler.extract(slot, resource, toExtract, tx);
                        if (extracted > 0) {
                            tx.commit();
                            ItemStack toEject = resource.toStack(extracted);
                            Vec3 ejectPos = worldPosition.relative(facing).getCenter();
                            Vec3 velocity = new Vec3(
                                facing.getStepX() * 0.25,
                                facing.getStepY() * 0.25,
                                facing.getStepZ() * 0.25
                            );
                            ItemEntity entity = new ItemEntity(level, ejectPos.x, ejectPos.y, ejectPos.z, toEject);
                            entity.setDeltaMovement(velocity);
                            entity.setDefaultPickUpDelay();
                            level.addFreshEntity(entity);
                            ejected = true;
                            this.lastEjectSlot = (slot + 1) % totalSlots;
                            break;
                        }
                    }
                }
            }

            if (ejected) {
                this.ejectCooldown = EJECT_COOLDOWN;
                this.setChanged();
            }
        } finally {
            this.flushClientSync();
        }
    }

    // === 神庙需求显示状态，由锻星砧控制器推送 ===
    @Getter
    private ItemStack templeDemandItem = ItemStack.EMPTY;
    @Getter
    private int templeDemandCount = 0;
    @Getter
    private int templeDemandProgress = 0;
    @Getter
    private boolean templeDemandSatisfied = false;

    // === 对撞机目标物品显示状态，由锻星砧控制器推送 ===
    @Getter
    private List<ItemStack> colliderTargetItems = new ArrayList<>();
    @Getter
    private boolean colliderProcessing = false;
    @Getter
    private boolean colliderStarMissing = false;

    // 下列 setter 会触发客户端同步，以便铁砧锤提示及时刷新。

    public void setTempleDemandItem(ItemStack templeDemandItem) {
        if (ItemStack.matches(this.templeDemandItem, templeDemandItem)) return;
        this.templeDemandItem = templeDemandItem.copy();
        this.setChanged();
    }

    public void setTempleDemandCount(int templeDemandCount) {
        if (this.templeDemandCount == templeDemandCount) return;
        this.templeDemandCount = templeDemandCount;
        this.setChanged();
    }

    public void setTempleDemandProgress(int templeDemandProgress) {
        if (this.templeDemandProgress == templeDemandProgress) return;
        this.templeDemandProgress = templeDemandProgress;
        this.setChanged();
    }

    public void setTempleDemandSatisfied(boolean templeDemandSatisfied) {
        if (this.templeDemandSatisfied == templeDemandSatisfied) return;
        this.templeDemandSatisfied = templeDemandSatisfied;
        this.setChanged();
    }

    public void setColliderTargetItems(List<ItemStack> colliderTargetItems) {
        boolean unchanged = this.colliderTargetItems.size() == colliderTargetItems.size()
            && IntStream.range(0, this.colliderTargetItems.size())
                .allMatch(index -> ItemStack.matches(
                    this.colliderTargetItems.get(index),
                    colliderTargetItems.get(index)
                ));
        if (unchanged) return;
        this.colliderTargetItems = new ArrayList<>(colliderTargetItems);
        this.setChanged();
    }

    public void setColliderProcessing(boolean colliderProcessing) {
        if (this.colliderProcessing == colliderProcessing) return;
        this.colliderProcessing = colliderProcessing;
        this.setChanged();
    }

    public void setColliderStarMissing(boolean colliderStarMissing) {
        if (this.colliderStarMissing == colliderStarMissing) return;
        this.colliderStarMissing = colliderStarMissing;
        this.setChanged();
    }

    public void setEjectCooldown(int ejectCooldown) {
        if (this.ejectCooldown == ejectCooldown) return;
        this.ejectCooldown = ejectCooldown;
        // 冷却仅参与服务端逻辑和磁盘持久化，不需要触发客户端完整库存包。
        super.setChanged();
    }

    // === 持久化：26.1 使用 ValueOutput / ValueInput ===

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("ejectCooldown", this.ejectCooldown);
        this.itemHandler.serialize(output.child("inventory"));
        if (!this.templeDemandItem.isEmpty()) {
            output.store("templeDemandItem", ItemStack.OPTIONAL_CODEC, this.templeDemandItem);
        }
        output.putInt("templeDemandCount", this.templeDemandCount);
        output.putInt("templeDemandProgress", this.templeDemandProgress);
        output.putBoolean("templeDemandSatisfied", this.templeDemandSatisfied);
        if (!this.colliderTargetItems.isEmpty()) {
            ValueOutput.ValueOutputList list = output.childrenList("colliderTargetItems");
            for (ItemStack stack : this.colliderTargetItems) {
                if (!stack.isEmpty()) {
                    list.addChild().store("item", ItemStack.OPTIONAL_CODEC, stack);
                }
            }
        }
        output.putBoolean("colliderProcessing", this.colliderProcessing);
        output.putBoolean("colliderStarMissing", this.colliderStarMissing);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.ejectCooldown = input.getIntOr("ejectCooldown", 0);
        this.itemHandler.deserialize(input.childOrEmpty("inventory"));
        this.templeDemandItem = input.read("templeDemandItem", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.templeDemandCount = input.getIntOr("templeDemandCount", 0);
        this.templeDemandProgress = input.getIntOr("templeDemandProgress", 0);
        this.templeDemandSatisfied = input.getBooleanOr("templeDemandSatisfied", false);
        this.colliderTargetItems.clear();
        input.childrenList("colliderTargetItems").ifPresent(list -> {
            for (ValueInput child : list) {
                child.read("item", ItemStack.OPTIONAL_CODEC).ifPresent(this.colliderTargetItems::add);
            }
        });
        this.colliderProcessing = input.getBooleanOr("colliderProcessing", false);
        this.colliderStarMissing = input.getBooleanOr("colliderStarMissing", false);
    }

    // === 网络同步：getUpdateTag 发送 CompoundTag，客户端经 loadAdditional 读取 ===

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 同步物品栏，供客户端铁砧锤提示显示。
        TagValueOutput invOutput = TagValueOutput.createWithContext(
            new ProblemReporter.Collector(this.problemPath()), registries);
        this.itemHandler.serialize(invOutput);
        tag.put("inventory", invOutput.buildResult());
        if (!this.templeDemandItem.isEmpty()) {
            tag.put("templeDemandItem", ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, this.templeDemandItem).getOrThrow());
        }
        tag.putInt("templeDemandCount", this.templeDemandCount);
        tag.putInt("templeDemandProgress", this.templeDemandProgress);
        tag.putBoolean("templeDemandSatisfied", this.templeDemandSatisfied);
        if (!this.colliderTargetItems.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : this.colliderTargetItems) {
                if (!stack.isEmpty()) {
                    list.add(ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow());
                }
            }
            tag.put("colliderTargetItems", list);
        }
        tag.putBoolean("colliderProcessing", this.colliderProcessing);
        tag.putBoolean("colliderStarMissing", this.colliderStarMissing);
        return tag;
    }
}

package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/// 锻星砧的物流接口。最多存储 16 种不同的物品类型，每种类型一个堆叠。物品自动路由到对应类型的槽位，不会溢出到其他槽位。
public class CelestialForgingAnvilLogisticsInterfaceBlockEntity extends BlockEntity {
    private static final int TYPE_COUNT = 16;
    @Setter
    private boolean syncing = false; /// 重入保护

    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(TYPE_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            ItemStack current = getStackInSlot(slot);
            if (current.isEmpty()) {
                for (int i = 0; i < TYPE_COUNT; i++) {
                    if (i != slot && ItemStack.isSameItemSameComponents(getStackInSlot(i), stack)) {
                        return false;
                    }
                }
                return true;
            }
            return ItemStack.isSameItemSameComponents(current, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            CelestialForgingAnvilLogisticsInterfaceBlockEntity.this.setChanged();
            if (!syncing) {
                CelestialForgingAnvilLogisticsInterfaceBlockEntity.this.triggerWormholeSync(slot);
            }
        }
    };

    public CelestialForgingAnvilLogisticsInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /// 将方块实体数据同步到所有追踪的客户端。
    public void syncToClients() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = getUpdatePacket();
            if (packet != null) {
                for (ServerPlayer player : serverLevel.getChunkSource().chunkMap
                    .getPlayers(serverLevel.getChunkAt(worldPosition).getPos(), false)) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncToClients();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @SuppressWarnings("unused")
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /// 当玩家放入或取出物品时从 onContentsChanged 调用。立即触发父 CFA 对此特定接口的虫洞同步，在同一 tick 内将更改推送到规范存储和其它 CFA。
    private void triggerWormholeSync(int changedSlot) {
        if (level == null || level.isClientSide()) return;
        BlockPos cfaPos = findParentCfa();
        if (cfaPos == null) return;
        if (level.getBlockEntity(cfaPos) instanceof CelestialForgingAnvilBlockEntity cfa) {
            cfa.syncLogisticsOnChange(worldPosition, changedSlot);
        }
    }

    /// 通过沿 FACING 方向追踪来查找父 CFA 控制器。接口背对 CFA，因此反方向的相邻方块始终是 CFA 部件。从那里通过 HALF 偏移导航到控制器（BOTTOM_CENTER）。
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

    private static final int MAX_EJECT_PER_OP = 64; /// 每次弹出最多 1 组
    public static final int EJECT_COOLDOWN = 8;     /// 弹出间隔 8gt（类似磁力溜槽）

    @Setter
    private int ejectCooldown = 0;
    private int lastEjectSlot = 0;

    /// 服务器端 tick。当主动模式（红石激活）时，每 8gt 自动将物品从内部库存向面向方向弹出，每次最多 1 组，速度类似于磁力溜槽。使用跨槽位轮询以防某个槽位被饿死。
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;
        if (!state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;

        if (ejectCooldown > 0) {
            ejectCooldown--;
            return;
        }

        Direction facing = state.getValue(CelestialForgingAnvilInterfaceBlock.FACING);
        BlockPos targetPos = worldPosition.relative(facing);
        boolean ejected = false;
        int totalSlots = itemHandler.getSlots();

        /// 轮询：从 lastEjectSlot 开始，遍历所有槽位
        for (int offset = 0; offset < totalSlots; offset++) {
            int slot = (lastEjectSlot + offset) % totalSlots;
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            int toExtract = Math.min(stack.getCount(), MAX_EJECT_PER_OP);
            ItemStack extracted = itemHandler.extractItem(slot, toExtract, false);
            if (extracted.isEmpty()) continue;

            /// 尝试插入到目标容器中
            IItemHandler targetHandler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, targetPos, facing.getOpposite()
            );
            if (targetHandler != null) {
                ItemStack remainder = ItemHandlerHelper.insertItem(targetHandler, extracted, false);
                if (!remainder.isEmpty()) {
                    itemHandler.insertItem(slot, remainder, false);
                }
                if (remainder.getCount() < extracted.getCount()) {
                    ejected = true;
                    lastEjectSlot = (slot + 1) % totalSlots;
                    break;
                }
            } else {
                /// 无目标容器——将物品以速度弹出到世界中
                Vec3 ejectPos = worldPosition.relative(facing).getCenter();
                Vec3 velocity = new Vec3(
                    facing.getStepX() * 0.25,
                    facing.getStepY() * 0.25,
                    facing.getStepZ() * 0.25
                );
                ItemEntity entity = new ItemEntity(level, ejectPos.x, ejectPos.y, ejectPos.z, extracted);
                entity.setDeltaMovement(velocity);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
                ejected = true;
                lastEjectSlot = (slot + 1) % totalSlots;
                break;
            }
        }

        if (ejected) {
            ejectCooldown = EJECT_COOLDOWN;
            setChanged();
        }
    }

    /// === 神殿需求显示（由 CFA 控制器推送）===
    @Getter @Setter
    private ItemStack templeDemandItem = ItemStack.EMPTY;
    @Getter @Setter
    private int templeDemandCount = 0;
    @Getter @Setter
    private int templeDemandProgress = 0;
    @Getter @Setter
    private boolean templeDemandSatisfied = false;

    /// === 对撞机目标物品显示（由 CFA 控制器推送）===
    @Getter @Setter
    private List<ItemStack> colliderTargetItems = new ArrayList<>();
    @Getter @Setter
    private boolean colliderProcessing = false;
    @Getter @Setter
    private boolean colliderStarMissing = false;

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ejectCooldown", ejectCooldown);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemandItem", templeDemandItem.save(registries));
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putInt("templeDemandProgress", templeDemandProgress);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        if (!colliderTargetItems.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : colliderTargetItems) {
                if (!stack.isEmpty()) {
                    list.add(stack.save(registries));
                }
            }
            tag.put("colliderTargetItems", list);
        }
        tag.putBoolean("colliderProcessing", colliderProcessing);
        tag.putBoolean("colliderStarMissing", colliderStarMissing);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.ejectCooldown = tag.getInt("ejectCooldown");
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("templeDemandItem")) {
            this.templeDemandItem = ItemStack.parse(registries, tag.getCompound("templeDemandItem"))
                .orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandProgress = tag.getInt("templeDemandProgress");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        this.colliderTargetItems.clear();
        if (tag.contains("colliderTargetItems")) {
            ListTag list = tag.getList("colliderTargetItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack.parse(registries, list.getCompound(i)).ifPresent(colliderTargetItems::add);
            }
        }
        this.colliderProcessing = tag.getBoolean("colliderProcessing");
        this.colliderStarMissing = tag.getBoolean("colliderStarMissing");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemandItem", templeDemandItem.save(registries));
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putInt("templeDemandProgress", templeDemandProgress);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        if (!colliderTargetItems.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : colliderTargetItems) {
                if (!stack.isEmpty()) {
                    list.add(stack.save(registries));
                }
            }
            tag.put("colliderTargetItems", list);
        }
        tag.putBoolean("colliderProcessing", colliderProcessing);
        tag.putBoolean("colliderStarMissing", colliderStarMissing);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("templeDemandItem")) {
            this.templeDemandItem = ItemStack.parse(registries, tag.getCompound("templeDemandItem"))
                .orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandProgress = tag.getInt("templeDemandProgress");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        this.colliderTargetItems.clear();
        if (tag.contains("colliderTargetItems")) {
            ListTag list = tag.getList("colliderTargetItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack.parse(registries, list.getCompound(i)).ifPresent(colliderTargetItems::add);
            }
        }
        this.colliderProcessing = tag.getBoolean("colliderProcessing");
        this.colliderStarMissing = tag.getBoolean("colliderStarMissing");
    }
}

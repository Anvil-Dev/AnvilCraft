package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

/// 锻星砧的物流接口。最多存储 16 种不同的物品类型，每种类型一个堆叠。物品自动路由到对应类型的槽位，不会溢出到其他槽位。
public class CelestialForgingAnvilLogisticsInterfaceBlockEntity extends BlockEntity {
    private static final int TYPE_COUNT = 16;
    private static final int MAX_COLLIDER_TOOLTIP_ITEMS = 256;
    private static final int TOOLTIP_REQUEST_INTERVAL = 5;
    private static final int TOOLTIP_REQUEST_EXPIRY = 200;
    private static final StreamCodec<ByteBuf, BlockPos> POS_STREAM_CODEC = ByteBufCodecs.VAR_LONG
        .map(BlockPos::of, BlockPos::asLong);
    @Setter
    private boolean syncing = false; /// 重入保护
    // Tooltip state is fetched on demand; coalesce changes made in one game tick.
    private boolean tooltipDataDirty = false;
    private long tooltipDataVersion = 1;
    private @Nullable TooltipData cachedTooltipData;
    private final Map<UUID, Long> tooltipRequestTicks = new HashMap<>();

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

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            this.tooltipDataDirty = true;
            this.cachedTooltipData = null;
        }
    }

    private void flushTooltipDataVersion() {
        if (!this.tooltipDataDirty) return;
        this.tooltipDataDirty = false;
        this.tooltipDataVersion++;
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

    private int ejectCooldown = 0;
    private int lastEjectSlot = 0;

    /// 服务器端 tick。当主动模式（红石激活）时，每 8gt 自动将物品从内部库存向面向方向弹出，每次最多 1 组，速度类似于磁力溜槽。使用跨槽位轮询以防某个槽位被饿死。
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        try {
            BlockState state = getBlockState();
            if (!state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;
            if (!state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;

            if (ejectCooldown > 0) {
                ejectCooldown--;
                return;
            }

            Direction facing = state.getValue(CelestialForgingAnvilInterfaceBlock.FACING);
            BlockPos targetPos = worldPosition.relative(facing);
            IItemHandler targetHandler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, targetPos, facing.getOpposite()
            );
            Vec3 ejectPos = targetPos.getCenter();
            if (targetHandler == null) {
                AABB ejectArea = new AABB(
                    ejectPos.add(-0.125, -0.125, -0.125),
                    ejectPos.add(0.125, 0.125, 0.125)
                );
                if (!level.noCollision(ejectArea)) return;
            }
            boolean ejected = false;
            int totalSlots = itemHandler.getSlots();

            /// 轮询：从 lastEjectSlot 开始，遍历所有槽位
            for (int offset = 0; offset < totalSlots; offset++) {
                int slot = (lastEjectSlot + offset) % totalSlots;
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                int toExtract = Math.min(stack.getCount(), MAX_EJECT_PER_OP);
                if (targetHandler == null) {
                    AABB itemDetectionArea = new AABB(targetPos).expandTowards(0, -0.5, 0);
                    int existingCount = level.getEntitiesOfClass(ItemEntity.class, itemDetectionArea).stream()
                        .map(ItemEntity::getItem)
                        .filter(existing -> ItemStack.isSameItemSameComponents(existing, stack))
                        .mapToInt(ItemStack::getCount)
                        .sum();
                    toExtract = Math.min(toExtract, stack.getMaxStackSize() - existingCount);
                    if (toExtract <= 0) continue;
                }
                ItemStack extracted = itemHandler.extractItem(slot, toExtract, false);
                if (extracted.isEmpty()) continue;

                /// 尝试插入到目标容器中
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
        } finally {
            this.flushTooltipDataVersion();
        }
    }

    /// === 神殿需求显示（由 CFA 控制器推送）===
    @Getter
    private ItemStack templeDemandItem = ItemStack.EMPTY;
    @Getter
    private int templeDemandCount = 0;
    @Getter
    private int templeDemandProgress = 0;
    @Getter
    private boolean templeDemandSatisfied = false;

    /// === 对撞机目标物品显示（由 CFA 控制器推送）===
    @Getter
    private List<ItemStack> colliderTargetItems = new ArrayList<>();
    @Getter
    private boolean colliderProcessing = false;
    @Getter
    private boolean colliderStarMissing = false;

    // Tooltip data is server-owned and fetched on demand by the client.
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
                    this.colliderTargetItems.get(index), colliderTargetItems.get(index)
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
        // Cooldown is server-side state and does not belong in tooltip snapshots.
        super.setChanged();
    }

    @RemoteCallable(validator = TooltipDataValidator.class)
    public static TooltipSyncResult syncTooltipData(
        @CallableParam(clazz = CelestialForgingAnvilLogisticsInterfaceBlockEntity.class, field = "POS_STREAM_CODEC") BlockPos pos,
        long knownVersion
    ) {
        CelestialForgingAnvilLogisticsInterfaceBlockEntity logistics = TooltipDataValidator.TARGET.get();
        try {
            if (logistics == null || !logistics.getBlockPos().equals(pos)) return TooltipSyncResult.EMPTY;
            logistics.flushTooltipDataVersion();
            return new TooltipSyncResult(
                logistics.tooltipDataVersion,
                knownVersion == logistics.tooltipDataVersion ? null : logistics.getOrCreateTooltipData()
            );
        } finally {
            TooltipDataValidator.TARGET.remove();
        }
    }

    private TooltipData getOrCreateTooltipData() {
        if (this.cachedTooltipData == null) {
            this.cachedTooltipData = this.createTooltipData();
        }
        return this.cachedTooltipData;
    }

    private boolean allowTooltipRequest(ServerPlayer player) {
        if (this.level == null || this.level.isClientSide()) return false;
        long gameTime = this.level.getGameTime();
        Long lastRequest = this.tooltipRequestTicks.get(player.getUUID());
        if (lastRequest != null && gameTime >= lastRequest && gameTime - lastRequest < TOOLTIP_REQUEST_INTERVAL) {
            return false;
        }
        if (this.tooltipRequestTicks.size() >= 64) {
            this.tooltipRequestTicks.entrySet().removeIf(entry -> gameTime - entry.getValue() >= TOOLTIP_REQUEST_EXPIRY);
        }
        this.tooltipRequestTicks.put(player.getUUID(), gameTime);
        return true;
    }

    private TooltipData createTooltipData() {
        List<ItemStack> storedItems = new ArrayList<>(TYPE_COUNT);
        for (int slot = 0; slot < this.itemHandler.getSlots(); slot++) {
            ItemStack stack = this.itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) storedItems.add(stack.copy());
        }

        boolean hasTempleDemand = !this.templeDemandSatisfied && !this.templeDemandItem.isEmpty();
        List<ItemStack> colliderTargets = new ArrayList<>();
        if (!this.colliderProcessing && !this.colliderStarMissing) {
            int targetCount = Math.min(this.colliderTargetItems.size(), MAX_COLLIDER_TOOLTIP_ITEMS);
            for (int index = 0; index < targetCount; index++) {
                ItemStack target = this.colliderTargetItems.get(index);
                if (!target.isEmpty()) colliderTargets.add(target.copy());
            }
        }
        return new TooltipData(
            storedItems,
            hasTempleDemand ? this.templeDemandItem.copy() : ItemStack.EMPTY,
            hasTempleDemand ? this.templeDemandCount : 0,
            hasTempleDemand ? this.templeDemandProgress : 0,
            colliderTargets,
            this.colliderProcessing,
            this.colliderStarMissing
        );
    }

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

    public record TooltipData(
        List<ItemStack> storedItems,
        ItemStack templeDemandItem,
        int templeDemandCount,
        int templeDemandProgress,
        List<ItemStack> colliderTargetItems,
        boolean colliderProcessing,
        boolean colliderStarMissing
    ) {
        private static final int HAS_TEMPLE_DEMAND = 1;
        private static final int COLLIDER_PROCESSING = 1 << 1;
        private static final int COLLIDER_STAR_MISSING = 1 << 2;
        private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> STORED_ITEMS_STREAM_CODEC =
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(TYPE_COUNT));
        private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> COLLIDER_TARGETS_STREAM_CODEC =
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_COLLIDER_TOOLTIP_ITEMS));
        public static final StreamCodec<RegistryFriendlyByteBuf, TooltipData> STREAM_CODEC = StreamCodec.of(
            TooltipData::encode,
            TooltipData::decode
        );

        private static void encode(RegistryFriendlyByteBuf buf, TooltipData data) {
            TooltipData.STORED_ITEMS_STREAM_CODEC.encode(buf, data.storedItems);
            int flags = 0;
            if (!data.templeDemandItem.isEmpty()) flags |= TooltipData.HAS_TEMPLE_DEMAND;
            if (data.colliderProcessing) flags |= TooltipData.COLLIDER_PROCESSING;
            if (data.colliderStarMissing) flags |= TooltipData.COLLIDER_STAR_MISSING;
            buf.writeByte(flags);
            if ((flags & TooltipData.HAS_TEMPLE_DEMAND) != 0) {
                ItemStack.STREAM_CODEC.encode(buf, data.templeDemandItem);
                buf.writeVarInt(data.templeDemandCount);
                buf.writeVarInt(data.templeDemandProgress);
            }
            if ((flags & (TooltipData.COLLIDER_PROCESSING | TooltipData.COLLIDER_STAR_MISSING)) == 0) {
                TooltipData.COLLIDER_TARGETS_STREAM_CODEC.encode(buf, data.colliderTargetItems);
            }
        }

        private static TooltipData decode(RegistryFriendlyByteBuf buf) {
            List<ItemStack> storedItems = TooltipData.STORED_ITEMS_STREAM_CODEC.decode(buf);
            int flags = buf.readUnsignedByte();
            boolean hasTempleDemand = (flags & TooltipData.HAS_TEMPLE_DEMAND) != 0;
            ItemStack templeDemandItem = hasTempleDemand ? ItemStack.STREAM_CODEC.decode(buf) : ItemStack.EMPTY;
            int templeDemandCount = hasTempleDemand ? buf.readVarInt() : 0;
            int templeDemandProgress = hasTempleDemand ? buf.readVarInt() : 0;
            boolean colliderProcessing = (flags & TooltipData.COLLIDER_PROCESSING) != 0;
            boolean colliderStarMissing = (flags & TooltipData.COLLIDER_STAR_MISSING) != 0;
            List<ItemStack> colliderTargetItems = colliderProcessing || colliderStarMissing
                ? List.of()
                : TooltipData.COLLIDER_TARGETS_STREAM_CODEC.decode(buf);
            return new TooltipData(
                storedItems,
                templeDemandItem,
                templeDemandCount,
                templeDemandProgress,
                colliderTargetItems,
                colliderProcessing,
                colliderStarMissing
            );
        }
    }

    public record TooltipSyncResult(long version, @Nullable TooltipData data) {
        public static final TooltipSyncResult EMPTY = new TooltipSyncResult(0, null);
        public static final StreamCodec<RegistryFriendlyByteBuf, TooltipSyncResult> STREAM_CODEC = StreamCodec.of(
            TooltipSyncResult::encode,
            TooltipSyncResult::decode
        );

        private static void encode(RegistryFriendlyByteBuf buf, TooltipSyncResult result) {
            buf.writeVarLong(result.version);
            buf.writeBoolean(result.data != null);
            if (result.data != null) TooltipData.STREAM_CODEC.encode(buf, result.data);
        }

        private static TooltipSyncResult decode(RegistryFriendlyByteBuf buf) {
            long version = buf.readVarLong();
            return new TooltipSyncResult(version, buf.readBoolean() ? TooltipData.STREAM_CODEC.decode(buf) : null);
        }
    }

    private static final class TooltipDataValidator implements IRemoteCallableValidator {
        private static final ThreadLocal<CelestialForgingAnvilLogisticsInterfaceBlockEntity> TARGET =
            new ThreadLocal<>();

        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            TooltipDataValidator.TARGET.remove();
            if (
                ctx.flow() != PacketFlow.SERVERBOUND
                || !(ctx.player() instanceof ServerPlayer player)
                || args.length != 2
                || !(args[0] instanceof BlockPos pos)
                || !(args[1] instanceof Long knownVersion)
                || knownVersion < 0
            ) {
                return false;
            }
            if (!player.level().isLoaded(pos)) return false;
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            if (!(blockEntity instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logistics)) {
                return false;
            }
            boolean accessible = AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(player.level(), pos),
                player,
                logistics.getBlockState().getBlock()
            );
            if (accessible && logistics.allowTooltipRequest(player)) {
                TooltipDataValidator.TARGET.set(logistics);
                return true;
            }
            return false;
        }
    }
}

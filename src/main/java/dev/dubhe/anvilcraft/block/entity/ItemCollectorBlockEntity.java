package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.power.consumer.ItemCollectorBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import dev.dubhe.anvilcraft.util.ItemResourceHelper;
import dev.dubhe.anvilcraft.util.WatchableCyclingValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.TriState;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class ItemCollectorBlockEntity extends BlockEntity
    implements MenuProvider,
    IFilterBlockEntity,
    IPowerConsumer,
    IDiskCloneable,
    IHasAffectRange,
    IItemResourceHandlerHolder {

    private static final Table<Level, ChunkPos, Set<ItemCollectorBlockEntity>> POACHING_COLLECTORS = HashBasedTable.create();

    private final WatchableCyclingValue<Integer> rangeRadius = new WatchableCyclingValue<>(
        "rangeRadius", _ -> this.setChanged(),
        1,
        2,
        4,
        8
    );

    private final WatchableCyclingValue<Integer> cooldown = new WatchableCyclingValue<>(
        "cooldown",
        thiz -> {
            this.cd = thiz.get();
            this.setChanged();
        },
        0,
        2,
        10,
        60
    );

    @Setter
    private PowerGrid grid;
    private int cd = this.cooldown.next();
    private int oldCooldown = -1;
    private int oldRange = -1;
    @Nullable
    private AABB boundingBox;
    private boolean needFlush = false;

    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            if (level == null || level.isClientSide()) return;
            flushState(level, getBlockPos());
            level.blockEntityChanged(worldPosition);
            ItemCollectorBlockEntity.this.needFlush = true;
        }
    };

    public ItemCollectorBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    private static final Map<Integer, Map<Integer, Integer>> POWER_CONSUMPTION = Map.of(
        0,
        Map.of(1, 8, 2, 12, 4, 20, 8, 32),
        2,
        Map.of(1, 5, 2, 8, 4, 12, 8, 20),
        10,
        Map.of(1, 3, 2, 5, 4, 8, 8, 12),
        60,
        Map.of(1, 2, 2, 3, 4, 5, 8, 8)
    );

    @Override
    public int getInputPower() {
        int power = ItemCollectorBlockEntity.POWER_CONSUMPTION.get(this.cooldown.get()).get(this.rangeRadius.get());
        if (level == null) return power;
        return getBlockState().getValue(ItemCollectorBlock.POWERED) ? 0 : power;
    }

    @Override
    public FilteredItemStackHandler getFilteredItemStackHandler() {
        return this.itemHandler;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Containers.dropContents(this.level, pos, this.itemHandler.getStacks());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.item_collector.title");
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("Inventory").ifPresent(this.itemHandler::deserialize);
        this.cooldown.fromIndex(input.getIntOr("Cooldown", 0));
        this.rangeRadius.fromIndex(input.getIntOr("RangeRadius", 0));
        this.cd = input.getIntOr("cd", 0);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.itemHandler.serialize(output.child("Inventory"));
        output.putInt("Cooldown", this.cooldown.index());
        output.putInt("RangeRadius", this.rangeRadius.index());
        output.putInt("cd", this.cd);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new ItemCollectorMenu(ModMenuTypes.ITEM_COLLECTOR.get(), i, inventory, this);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(
            new ProblemReporter.Collector(this.problemPath()),
            registries
        );
        this.saveAdditional(output);
        return output.buildResult();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removePoachingCollector(this);
    }

    @Override
    public void gridTick() {
        if (level == null || level.isClientSide()) return;

        if (this.cd > 1) {
            this.cd--;
            return;
        }
        if (this.boundingBox == null) return;
        if (!this.isGridWorking()) return;
        BlockState state = level.getBlockState(getBlockPos());
        if (state.hasProperty(ItemCollectorBlock.POWERED) && state.getValue(ItemCollectorBlock.POWERED)) return;
        List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, this.boundingBox);
        for (ItemEntity itemEntity : itemEntities) {
            this.acceptItemEntity(itemEntity);
        }
        if (this.cooldown.get() > 0) {
            this.cd = this.cooldown.get();
        } else {
            this.cd = 5; // 这个地方是给“即便是截胡模式也主动吸取物品”的设定准备的，暂时随便写了个数值
        }
    }

    public TriState acceptItemEntity(ItemEntity itemEntity) {
        ItemStack itemStack = itemEntity.getItem();
        ItemResource resource = ItemResource.of(itemStack);
        boolean inserted = false;
        try (Transaction transaction = Transaction.openRoot()) {
            int amount;
            try (Transaction test = Transaction.open(transaction)) {
                amount = this.itemHandler.insert(resource, itemStack.count(), test);
            }
            if (amount != 0) {
                this.itemHandler.insert(resource, amount, transaction);
                itemStack.shrink(amount);
                transaction.commit();
                inserted = true;
            }
        }
        if (itemStack.isEmpty()) {
            itemEntity.discard();
            return TriState.TRUE;
        }
        itemEntity.setItem(itemStack);
        return inserted ? TriState.DEFAULT : TriState.FALSE;
    }

    public void tick(Level level, BlockPos blockPos) {
        this.flushState(level, blockPos);
        if (this.needFlush) {
            this.setChanged();
        }
        if (this.cooldown.get() != this.oldCooldown) {
            this.oldCooldown = this.cooldown.get();
            if (this.oldCooldown == 0) {
                addPoachingCollector(this);
            } else {
                removePoachingCollector(this);
            }
        }
        if (this.rangeRadius.get() != this.oldRange || this.boundingBox == null) {
            this.boundingBox = AABB.ofSize(
                Vec3.atCenterOf(getBlockPos()),
                this.rangeRadius.get() * 2.0 + 1,
                this.rangeRadius.get() * 2.0 + 1,
                this.rangeRadius.get() * 2.0 + 1
            );
            this.oldRange = this.rangeRadius.get();
        }
    }

    /// 获取红石信号
    public int getRedstoneSignal() {
        int i = 0;
        for (int j = 0; j < this.itemHandler.size(); ++j) {
            if (ItemResourceHelper.isSlotEmpty(this.itemHandler, j) && !this.itemHandler.isSlotDisabled(j)) continue;
            ++i;
        }
        return i;
    }

    @Override
    public void storeDiskData(ValueOutput output) {
        if (this.level == null) return;
        this.itemHandler.serialize(output.child("Inventory"));
        output.putInt("Cooldown", this.cooldown.index());
        output.putInt("RangeRadius", this.rangeRadius.index());
        output.putInt("cd", this.cd);
    }

    @Override
    public void applyDiskData(ValueInput input) {
        if (this.level == null) return;
        this.itemHandler.deserialize(input.childOrEmpty("Inventory"));
        this.cooldown.fromIndex(input.getIntOr("Cooldown", 0));
        this.rangeRadius.fromIndex(input.getIntOr("RangeRadius", 0));
        this.cd = input.getIntOr("cd", 0);
        this.setChanged();
        Vec3 center = this.getPos().getCenter();
        MinecraftServer server = level.getServer();
        if (server == null) return;
        Packet<ClientGamePacketListener> packet = this.getUpdatePacket();
        if (packet == null) return;
        server.getPlayerList().broadcast(null, center.x(), center.y(), center.z(), 256, this.level.dimension(), packet);
    }

    @Override
    public AABB shape() {
        if (this.boundingBox == null) {
            this.boundingBox = AABB.ofSize(
                Vec3.atCenterOf(getBlockPos()),
                this.rangeRadius.get() * 2.0 + 1,
                this.rangeRadius.get() * 2.0 + 1,
                this.rangeRadius.get() * 2.0 + 1
            );
        }
        return this.boundingBox;
    }

    public static void clearPoachingCollectors() {
        POACHING_COLLECTORS.clear();
    }

    @SuppressWarnings("DataFlowIssue")
    public static Set<ItemCollectorBlockEntity> getOrCreateCollectorList(ItemCollectorBlockEntity blockEntity) {
        ChunkPos chunkPos = ChunkPos.containing(blockEntity.worldPosition);
        Level level = blockEntity.level;
        return getOrCreateCollectorList(level, chunkPos);
    }

    public static Set<ItemCollectorBlockEntity> getOrCreateCollectorList(Level level, ChunkPos chunkPos) {
        Set<ItemCollectorBlockEntity> collectors = POACHING_COLLECTORS.get(
            level,
            chunkPos
        );
        if (collectors == null) {
            collectors = new HashSet<>();
            POACHING_COLLECTORS.put(
                level,
                chunkPos,
                collectors
            );
        }
        return collectors;
    }

    public static void addPoachingCollector(ItemCollectorBlockEntity blockEntity) {
        getOrCreateCollectorList(blockEntity).add(blockEntity);
    }

    public static void removePoachingCollector(ItemCollectorBlockEntity blockEntity) {
        getOrCreateCollectorList(blockEntity).remove(blockEntity);
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    public static void poachItemEntity(ItemEntity itemEntity) {
        Set<ItemCollectorBlockEntity> collectors = POACHING_COLLECTORS.get(
            itemEntity.level(),
            ChunkPos.containing(itemEntity.blockPosition())
        );
        if (collectors == null) return;
        for (ItemCollectorBlockEntity collector : collectors) {
            TriState state = collector.acceptItemEntity(itemEntity);
            if (state == TriState.TRUE) {
                break;
            }
        }
    }
}

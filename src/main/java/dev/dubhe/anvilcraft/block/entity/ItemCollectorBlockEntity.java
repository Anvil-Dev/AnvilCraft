package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.ItemCollectorBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ItemCollectorBlockEntity extends BlockEntity
    implements MenuProvider,
    IFilterBlockEntity,
    IPowerConsumer,
    IDiskCloneable,
    IHasAffectRange,
    IItemHandlerHolder {
    @Setter
    private PowerGrid grid;

    private final WatchableCyclingValue<Integer> rangeRadius = new WatchableCyclingValue<>(
        "rangeRadius",
        thiz -> {
            this.setChanged();
        },
        1,
        2,
        4,
        8
    );
    private final WatchableCyclingValue<Integer> cooldown = new WatchableCyclingValue<>(
        "cooldown",
        thiz -> {
            cd = thiz.get();
            this.setChanged();
        },
        0,
        2,
        10,
        60
    );
    private int cd = cooldown.get();

    public static final Map<Level, Map<ChunkPos, List<ItemCollectorBlockEntity>>> PoachingCollectorMap = new HashMap<>();

    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(9) {
        @Override
        public void onContentsChanged(int slot) {
            ItemCollectorBlockEntity.this.setChanged();
        }
    };

    public ItemCollectorBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public Level getCurrentLevel() {
        return level;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return getBlockPos();
    }


    private static final Map<Integer, Map<Integer, Integer>> powerConsumption =
        Map.of(
            0, Map.of(1, 8, 2, 12, 4, 20, 8, 32),
            2, Map.of(1, 5, 2, 8, 4, 12, 8, 20),
            10, Map.of(1, 3, 2, 5, 4, 8, 8, 12),
            60, Map.of(1, 2, 2, 3, 4, 5, 8, 8)
        );

    @Override
    public int getInputPower() {
        int power = powerConsumption.get(cooldown.get()).get(rangeRadius.get());
        if (level == null) return power;
        return getBlockState().getValue(ItemCollectorBlock.POWERED) ? 0 : power;
    }

    @Override
    public FilteredItemStackHandler getFilteredItemStackHandler() {
        return itemHandler;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.item_collector.title");
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.loadAdditional(tag, provider);
        itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
        cooldown.fromIndex(tag.getInt("Cooldown"));
        rangeRadius.fromIndex(tag.getInt("RangeRadius"));
        cd = tag.getInt("cd");
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Inventory", this.itemHandler.serializeNBT(provider));
        tag.putInt("Cooldown", cooldown.index());
        tag.putInt("RangeRadius", rangeRadius.index());
        tag.putInt("cd", cd);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        if (player.isSpectator()) return null;
        return new ItemCollectorMenu(ModMenuTypes.ITEM_COLLECTOR.get(), i, inventory, this);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put("Inventory", this.itemHandler.serializeNBT(provider));
        tag.putInt("Cooldown", cooldown.index());
        tag.putInt("RangeRadius", rangeRadius.index());
        return tag;
    }

    public List<ChunkPos> getPoachingMapPositions(int range) {
        List<ChunkPos> chunkPosList = new ArrayList<>();
        BlockPos center = getBlockPos();
        int d = range * 2 + 1;
        int minX = center.getX() - d;
        int maxX = center.getX() + d;
        int minZ = center.getZ() - d;
        int maxZ = center.getZ() + d;
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                chunkPosList.add(new ChunkPos(cx, cz));
            }
        }
        return chunkPosList;
    }

    public void updatePoachingMapForThis() {
        List<ChunkPos> chunkPosListMax = getPoachingMapPositions(8);
        List<ChunkPos> chunkPosListReal = getPoachingMapPositions(rangeRadius.get());
        for (ChunkPos chunkPos : chunkPosListMax) {
            if (cooldown.get() == 0 && chunkPosListReal.contains(chunkPos)) {
                if (!PoachingCollectorMap.containsKey(level)) PoachingCollectorMap.put(level, new HashMap<>());
                if (!PoachingCollectorMap.get(level).containsKey(chunkPos))
                    PoachingCollectorMap.get(level).put(chunkPos, new ArrayList<>());
                List<ItemCollectorBlockEntity> list = PoachingCollectorMap.get(level).get(chunkPos);
                if (!list.contains(this)) list.add(this);
            } else {
                if (PoachingCollectorMap.containsKey(level) && PoachingCollectorMap.get(level).containsKey(chunkPos)) {
                    List<ItemCollectorBlockEntity> list = PoachingCollectorMap.get(level).get(chunkPos);
                    list.remove(this);
                }
            }
        }

    }

    @Override
    public void gridTick() {
        if (level == null || level.isClientSide) return;
        this.updatePoachingMapForThis();
        //如果保持“截胡模式就不再主动吸取物品”的设定就把下面一行取消注释回来
        //if (cooldown.get() == 0) return;

        if (cd > 1) {
            cd--;
            return;
        }
        if (!this.isGridWorking()) return;
        BlockState state = level.getBlockState(getBlockPos());
        if (state.hasProperty(ItemCollectorBlock.POWERED) && state.getValue(ItemCollectorBlock.POWERED)) return;
        AABB box = AABB.ofSize(
            Vec3.atCenterOf(getBlockPos()),
            rangeRadius.get() * 2.0 + 1,
            rangeRadius.get() * 2.0 + 1,
            rangeRadius.get() * 2.0 + 1
        );
        List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, box);
        for (ItemEntity itemEntity : itemEntities) {
            ItemStack itemStack = itemEntity.getItem();
            int slotIndex = 0;
            while (itemStack != ItemStack.EMPTY && slotIndex < 9) {
                itemStack = itemHandler.insertItem(slotIndex++, itemStack, false);
            }
            if (itemStack != ItemStack.EMPTY) {
                itemEntity.setItem(itemStack);
            } else {
                itemEntity.remove(Entity.RemovalReason.DISCARDED);
            }
        }
        if (cooldown.get() > 0)
            cd = cooldown.get();
        else cd = 5; //这个地方是给“即便是截胡模式也主动吸取物品”的设定准备的，暂时随便写了个数值
    }

    public void tick(Level level, BlockPos blockPos) {
        this.flushState(level, blockPos);
    }

    /**
     * 获取红石信号
     */
    public int getRedstoneSignal() {
        int i = 0;
        for (int j = 0; j < itemHandler.getSlots(); ++j) {
            ItemStack itemStack = itemHandler.getStackInSlot(j);
            if (itemStack.isEmpty() && !itemHandler.isSlotDisabled(j)) continue;
            ++i;
        }
        return i;
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.put("Filtering", itemHandler.serializeFiltering());
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        itemHandler.deserializeFiltering(data.getCompound("Filtering"));
    }

    @Override
    public AABB shape() {
        return AABB.ofSize(
            Vec3.atCenterOf(getBlockPos()),
            rangeRadius.get() * 2.0 + 1,
            rangeRadius.get() * 2.0 + 1,
            rangeRadius.get() * 2.0 + 1
        );
    }
}

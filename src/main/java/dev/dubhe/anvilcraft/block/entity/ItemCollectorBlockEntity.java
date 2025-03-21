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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        60,
        10,
        2,
        0
    );
    private int cd = cooldown.get();

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

    @Override
    public int getInputPower() {
        int[] pList = {2, 3, 5, 8, 12, 20, 32};
        int tmp = rangeRadius.index() + cooldown.index();
        int power = pList[tmp];
        if (level == null) return power;
        return getBlockState().getValue(ItemCollectorBlock.POWERED) ? 0 : power;
    }

    @Override
    public FilteredItemStackHandler getFilteredItemDepository() {
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

    @Override
    public void gridTick() {
        if (this.level == null || this.level.isClientSide) return;
        if (cooldown.get() == 0) return;
        if (cd > 1) {
            cd--;
            return;
        }
        if (!this.isGridWorking()) return;
        BlockState state = this.level.getBlockState(getBlockPos());
        if (state.hasProperty(ItemCollectorBlock.POWERED) && state.getValue(ItemCollectorBlock.POWERED)) return;
        collectItems();
        cd = cooldown.get();
    }

    public void tick(Level level, BlockPos blockPos) {
        this.flushState(level, blockPos);
        if (this.level == null || this.level.isClientSide) return;
        if (cooldown.get() != 0) return;
        if (!this.isGridWorking()) return;
        BlockState state = this.level.getBlockState(getBlockPos());
        if (state.hasProperty(ItemCollectorBlock.POWERED) && state.getValue(ItemCollectorBlock.POWERED)) return;
        collectItems();
        cd = cooldown.get();
    }

    public void collectItems() {
        if (level == null || level.isClientSide) return;
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

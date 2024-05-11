package dev.dubhe.anvilcraft.block.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.dubhe.anvilcraft.api.depository.FilteredItemDepository;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.ItemCollectorBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import dev.dubhe.anvilcraft.util.WatchableCyclingValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
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

import static net.minecraft.world.level.block.Block.UPDATE_CLIENTS;

@Getter
public class ItemCollectorBlockEntity
        extends BlockEntity
        implements MenuProvider, IFilterBlockEntity, IPowerConsumer {
    @Setter
    private PowerGrid grid;
    private final WatchableCyclingValue<Integer> rangeRadius = new WatchableCyclingValue<>("rangeRadius",
            thiz -> {

                this.setChanged();
            },
            1, 2, 4, 8
    );
    private final WatchableCyclingValue<Integer> cooldown = new WatchableCyclingValue<>("cooldown",
            thiz -> {
                cd = thiz.get();
                this.setChanged();
            },
            1, 2, 5, 15, 60
    );
    private int cd = rangeRadius.get();

    private final FilteredItemDepository depository = new FilteredItemDepository.Pollable(9) {
        @Override
        public void onContentsChanged(int slot) {
            ItemCollectorBlockEntity.this.setChanged();
        }
    };

    protected ItemCollectorBlockEntity(
            BlockEntityType<? extends BlockEntity> type,
            BlockPos pos,
            BlockState blockState
    ) {
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
        return (int) (4 * rangeRadius.get() * (30.0 / cooldown.get()));
    }

    @Override
    public FilteredItemDepository getFilteredItemDepository() {
        return depository;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.item_collector.title");
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        depository.deserializeNbt(tag.getCompound("Inventory"));
        cooldown.fromIndex(tag.getInt("Cooldown"));
        rangeRadius.fromIndex(tag.getInt("RangeRadius"));
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", this.depository.serializeNbt());
        tag.putInt("Cooldown", cooldown.index());
        tag.putInt("RangeRadius", rangeRadius.index());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new ItemCollectorMenu(ModMenuTypes.ITEM_COLLECTOR.get(), i, inventory, this);
    }

    @ExpectPlatform
    public static ItemCollectorBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        throw new AssertionError();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("Inventory", this.depository.serializeNbt());
        tag.putInt("Cooldown", cooldown.index());
        tag.putInt("RangeRadius", rangeRadius.index());
        return tag;
    }

    @Override
    public void gridTick() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(getBlockPos());
        if (state.hasProperty(ItemCollectorBlock.POWERED)
                && state.getValue(ItemCollectorBlock.POWERED)) return;
        if (cd - 1 != 0) {
            cd--;
            return;
        }
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
                itemStack = depository.insert(slotIndex++, itemStack, false);
            }
            if (itemStack != ItemStack.EMPTY) {
                itemEntity.setItem(itemStack);
            } else {
                itemEntity.remove(Entity.RemovalReason.DISCARDED);
            }
        }
        cd = cooldown.get();
    }

    @ExpectPlatform
    public static void onBlockEntityRegister(BlockEntityType<ItemCollectorBlockEntity> type) {
        throw new AssertionError();
    }

    public void tick(Level level, BlockPos blockPos) {
        this.flushState(level, blockPos);
    }
}

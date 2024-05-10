package dev.dubhe.anvilcraft.block.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.dubhe.anvilcraft.api.depository.FilteredItemDepository;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.ItemCollectorBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import dev.dubhe.anvilcraft.util.CyclingValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class ItemCollectorBlockEntity extends BaseMachineBlockEntity implements IFilterBlockEntity, IPowerConsumer {
    @Setter
    private PowerGrid grid;
    private final CyclingValue<Integer> rangeRadius = new CyclingValue<>(1, 2, 4, 8);
    private final CyclingValue<Integer> cooldown = new CyclingValue<>(1, 2, 5, 15, 60);

    private final FilteredItemDepository depository = new FilteredItemDepository.Pollable(9) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
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
    public Direction getDirection() {
        return Direction.UP;
    }

    @Override
    public void setDirection(Direction direction) {

    }

    @Override
    public int getInputPower() {
        return 4 * rangeRadius.get() * (30 / cooldown.get());
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
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ItemCollectorMenu(ModMenuTypes.ITEM_COLLECTOR.get(), i, inventory, this);
    }

    @ExpectPlatform
    public static ItemCollectorBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        throw new AssertionError();
    }

    @Override
    public void gridTick() {
        BlockState state = level.getBlockState(getBlockPos());
        if (state.hasProperty(ItemCollectorBlock.POWERED)
                && !state.getValue(ItemCollectorBlock.POWERED)) return;
    }

    @ExpectPlatform
    public static void onBlockEntityRegister(BlockEntityType<ItemCollectorBlockEntity> type) {
        throw new AssertionError();
    }

    public void tick(Level level, BlockPos blockPos) {
        this.flushState(level, blockPos);
    }
}

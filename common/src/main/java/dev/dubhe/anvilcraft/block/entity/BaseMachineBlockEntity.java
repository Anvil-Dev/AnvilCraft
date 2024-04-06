package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class BaseMachineBlockEntity extends BlockEntity implements MenuProvider {
    protected final ItemDepository depository;

    protected BaseMachineBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState, int size) {
        super(type, pos, blockState);
        this.depository = new ItemDepository.Pollable(size);
    }

    public abstract Direction getDirection();

    public abstract void setDirection(Direction direction);

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.depository.deserializeNBT(tag.getCompound("Inventory"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", this.depository.serializeNBT());
    }

    protected boolean outputItem(@Nullable IItemDepository itemDepository, Direction direction, Level level, @NotNull BlockPos pos, @NotNull Container container, int slot, boolean part, boolean drop, boolean momentum, boolean needEmpty) {
        return false;
    }
}

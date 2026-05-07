package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

@Getter
public class CrabTrapBlockEntity extends BlockEntity implements IItemHandlerHolder {
    public CrabTrapBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    private final ItemStackHandler itemHandler = new ItemStackHandler(9);

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Inventory", CompoundTag.CODEC, itemHandler.serializeNBT(provider));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserializeNBT(provider, input.read("Inventory", CompoundTag.CODEC).orElse(new CompoundTag()));
    }
}

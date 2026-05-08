package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

@Getter
public class CrabTrapBlockEntity extends BlockEntity implements IItemHandlerHolder {
    public CrabTrapBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(9);

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        itemHandler.serialize(output.child("Inventory"));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserialize(input.childOrEmpty("Inventory"));
    }
}

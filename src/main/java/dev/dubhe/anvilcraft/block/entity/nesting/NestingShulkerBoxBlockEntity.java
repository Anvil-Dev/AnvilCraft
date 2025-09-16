package dev.dubhe.anvilcraft.block.entity.nesting;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class NestingShulkerBoxBlockEntity extends BaseNestingShulkerBoxBlockEntity {
    public NestingShulkerBoxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(27, type, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return ModBlocks.NESTING_SHULKER_BOX.get().getName();
    }
}

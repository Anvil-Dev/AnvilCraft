package dev.dubhe.anvilcraft.block.entity.fabric;

import dev.dubhe.anvilcraft.api.depository.fabric.ItemDepositoryHelperImpl;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CrabTrapBlockEntityImpl extends CrabTrapBlockEntity {

    public CrabTrapBlockEntityImpl(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static CrabTrapBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new CrabTrapBlockEntityImpl(type, pos, blockState);
    }

    public static void onBlockEntityRegister(BlockEntityType<CrabTrapBlockEntity> type) {
        ItemStorage.SIDED.registerForBlockEntity((blockEntity, direction) ->
            ItemDepositoryHelperImpl.toStorage(blockEntity.getDepository()), type);
    }
}

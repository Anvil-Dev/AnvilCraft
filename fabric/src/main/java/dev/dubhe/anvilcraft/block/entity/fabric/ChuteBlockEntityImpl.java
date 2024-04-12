package dev.dubhe.anvilcraft.block.entity.fabric;

import dev.dubhe.anvilcraft.api.depository.fabric.ItemDepositoryHelperImpl;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ChuteBlockEntityImpl extends ChuteBlockEntity {
    public ChuteBlockEntityImpl(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static @NotNull ChuteBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new ChuteBlockEntityImpl(type, pos, blockState);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void onBlockEntityRegister(BlockEntityType<ChuteBlockEntity> type) {
        ItemStorage.SIDED.registerForBlockEntity((blockEntity, direction) ->
            ItemDepositoryHelperImpl.toStorage(blockEntity.getDepository()), type);
    }
}

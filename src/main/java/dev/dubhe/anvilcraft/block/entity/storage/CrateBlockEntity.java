package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.saved.storage.CrateStorage;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CrateBlockEntity extends StorageBlockEntity {
    public CrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, StorageType.CRATE);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.refreshDispose();
    }

    public void refreshDispose() {
        if (this.level == null || this.level.isClientSide() || !(this.getBlockState().getBlock() instanceof CrateBlock)) return;
        CrateBlock.updateDisposeState(this.level, this.getBlockPos());
        if (this.getId() != null) {
            boolean dispose = this.getBlockState().getValue(CrateBlock.DISPOSE);
            Storages.get().get(this.getId(), CrateStorage.class).ifPresent(storage -> storage.getItems().setDispose(dispose));
        }
    }
}

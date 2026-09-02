package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import dev.dubhe.anvilcraft.saved.storage.CrateStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/**
 * 板条箱方块实体。相邻虚空物质时方块进入溢出销毁模式（{@code dispose} 方块状态），
 * 方块实体与存储保持不变，仅存储的处理器切换为销毁溢出物品。
 */
public class CrateBlockEntity extends StorageBlockEntity {
    public CrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, ModStorageTypes.CRATE);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!Objects.requireNonNull(this.level).isClientSide() && this.getBlockState().getBlock() instanceof CrateBlock) {
            // 世界 / 区块加载时先按当前相邻虚空物质重算 dispose 状态，
            // 再无条件把（可能未变化）状态同步进存储处理器，防止新加载的
            // 处理器残留默认的 dispose=false
            CrateBlock.updateDisposeState(this.level, this.getBlockPos());
            this.refreshDispose();
        }
    }

    /**
     * 方块状态（dispose）变化后调用，把状态同步到存储处理器，
     * 使 GUI / RPC / 管道各插入路径都按当前模式处理溢出物品。
     */
    public void refreshDispose() {
        this.syncDispose();
    }

    private void syncDispose() {
        if (this.level == null || this.level.isClientSide() || this.getId() == null) {
            return;
        }
        boolean dispose = this.getBlockState().getValue(CrateBlock.DISPOSE);
        Storages.get().get(this.getId(), CrateStorage.class)
            .ifPresent(storage -> storage.getItems().setDispose(dispose));
    }
}

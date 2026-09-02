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
            // 世界 / 区块加载时按当前相邻虚空物质重算并同步 dispose，
            // 防止存档中残留的旧状态在加载后与实际摆放不一致
            this.refreshDispose();
        }
    }

    /**
     * 按当前相邻虚空物质重算 dispose 方块状态（幂等，状态变化时内部会再次调用本方法），
     * 再把状态同步到存储处理器，使 GUI / RPC / 管道各插入路径都按当前模式处理溢出物品。
     *
     * <p>各外部入口（能力查询、存储端口、RPC getView）在拿到 handler 前必须调用本方法；
     * 先重算再同步可避免 blockstate 陈旧（如新放置的存储尚未经块事件同步）时
     * 把旧 dispose 值写进处理器。</p>
     */
    public void refreshDispose() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        if (this.getBlockState().getBlock() instanceof CrateBlock) {
            CrateBlock.updateDisposeState(this.level, this.getBlockPos());
        }
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

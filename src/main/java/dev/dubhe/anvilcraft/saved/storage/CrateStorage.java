package dev.dubhe.anvilcraft.saved.storage;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.OverflowDisposalItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import net.minecraft.core.Holder;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 板条箱存储。物品处理器为 {@link OverflowDisposalItemStacksResourceHandler}，
 * 其 {@code dispose} 标记是运行时瞬态值（不写入 NBT），由放置的板条箱方块状态驱动。
 *
 * <p>约定：所有拿到本存储 handler 的外部插入路径（能力查询、RPC、存储端口等）在
 * 使用前必须先调用对应 {@code CrateBlockEntity.refreshDispose()} 同步 dispose 标记；
 * 新增插入路径时不得直接 {@code Storages.get().get(id).getItems()} 后写入，
 * 否则会拿到残留 dispose=false 的 handler（新创建存储默认 false）。</p>
 */
public class CrateStorage extends BaseStorage<OverflowDisposalItemStacksResourceHandler> {
    public CrateStorage(UUID id) {
        super(id);
    }

    @Override
    protected OverflowDisposalItemStacksResourceHandler constructItemHandler(
        BiConsumer<Integer, UnlimitedItemStack> onContentsChanged
    ) {
        return new OverflowDisposalItemStacksResourceHandler(2048) {
            @Override
            protected void onContentsChanged(int index, UnlimitedItemStack original) {
                onContentsChanged.accept(index, original);
            }
        };
    }

    @Override
    public Holder<IStorageType<?>> getTypeHolder() {
        return ModStorageTypes.CRATE;
    }
}

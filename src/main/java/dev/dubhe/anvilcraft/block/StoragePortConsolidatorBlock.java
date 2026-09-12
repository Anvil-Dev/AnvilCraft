package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.entity.StoragePortConsolidatorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 仓储端口整合器方块。
 *
 * <p>把相连仓储端口的内容物并成一个物品能力暴露给外部物流（漏斗、溜槽、AE 等模组的仓储总线），
 * 相连关系只沿端口延伸、不跨集装箱 / 存储站，且没有连接核心也能工作；
 * 物品优先进入标记了对应物品的端口，双击右键把身上对应标记的物品全部存入。
 * 左右键行为本身定义在 {@link StoragePortConsolidatorBlockEntity}。</p>
 */
public class StoragePortConsolidatorBlock extends AbstractStoragePortBlock {
    public StoragePortConsolidatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(StoragePortConsolidatorBlock::new);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ModBlockEntities.STORAGE_PORT_CONSOLIDATOR.create(blockPos, blockState);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
            blockEntityType,
            ModBlockEntities.STORAGE_PORT_CONSOLIDATOR.get(),
            (level1, pos, state1, entity) -> entity.tickServer()
        );
    }
}

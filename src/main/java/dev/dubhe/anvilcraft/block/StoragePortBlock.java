package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 仓储端口方块。
 *
 * <p>面相邻潜影集装箱 / 超维存储站（可沿端口链延伸）使用；右键物品标记并塞入、
 * 双击塞入全部、左键取出；铁砧锤长按右键并滑动可去掉标记（普通锤右键不改变状态）；
 * 拆除时缓存与标记保留在掉落物中。左右键行为本身定义在
 * {@link StoragePortBlockEntity}，本类只负责掉落物、克隆与方块状态。</p>
 */
public class StoragePortBlock extends AbstractStoragePortBlock {
    /** 是否有标记的方块状态 */
    public static final BooleanProperty MARKED = BooleanProperty.create("marked");

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(StoragePortBlock::new);
    }

    public StoragePortBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(StoragePortBlock.MARKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(StoragePortBlock.MARKED);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof StoragePortBlockEntity port) {
            for (ItemStack drop : drops) {
                if (drop.is(this.asItem())) {
                    port.saveToDrop(drop, params.getLevel().registryAccess());
                }
            }
        }
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        LevelReader level,
        BlockPos pos,
        Player player
    ) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        // 中键克隆（或其它拾取路径）也保留缓存与标记，保证物品模型与 tooltip 有数据可显示
        if (
            level instanceof Level realLevel
            && realLevel.getBlockEntity(pos) instanceof StoragePortBlockEntity port
            && Screen.hasControlDown()
        ) {
            port.saveToDrop(stack, realLevel.registryAccess());
        }
        return stack;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // 创造模式敲掉非空端口：与潜影盒一致，掉落带缓存与标记数据的端口物品
        if (!level.isClientSide && player.hasInfiniteMaterials()
            && level.getBlockEntity(pos) instanceof StoragePortBlockEntity port
            && (!port.isBufferEmpty() || !port.getMarkedItem().isEmpty())) {
            ItemStack drop = new ItemStack(this);
            port.saveToDrop(drop, level.registryAccess());
            Block.popResource(level, pos, drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ModBlockEntities.STORAGE_PORT.create(blockPos, blockState);
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
            ModBlockEntities.STORAGE_PORT.get(),
            (level1, pos, state1, entity) -> entity.tickServer()
        );
    }
}

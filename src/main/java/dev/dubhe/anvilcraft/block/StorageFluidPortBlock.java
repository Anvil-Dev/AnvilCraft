package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 仓储流体端口方块。
 *
 * <p>作为面相邻潜影集装箱 / 超维存储站的外部流体存储，连接关系与仓储端口一致且可互相延伸；
 * 内部容量 128 B 且只能存储单一流体。手持门格海绵右键清除内部流体；拆除时流体随掉落物保留。
 * 左右键行为本身定义在 {@link StorageFluidPortBlockEntity}，本类只负责掉落物、克隆与同步。</p>
 */
public class StorageFluidPortBlock extends AbstractStoragePortBlock {
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(StorageFluidPortBlock::new);
    }

    public StorageFluidPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof StorageFluidPortBlockEntity port) {
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
        // 中键克隆（或其它拾取路径）也保留流体，保证物品模型与 tooltip 有数据可显示
        if (
            level instanceof Level realLevel
            && realLevel.getBlockEntity(pos) instanceof StorageFluidPortBlockEntity port
            && Screen.hasControlDown()
        ) {
            port.saveToDrop(stack, realLevel.registryAccess());
        }
        return stack;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // 创造模式敲掉非空端口：掉落带流体的端口物品
        if (!level.isClientSide && player.hasInfiniteMaterials()
            && level.getBlockEntity(pos) instanceof StorageFluidPortBlockEntity port
            && !port.getTank().isEmpty()) {
            ItemStack drop = new ItemStack(this);
            port.saveToDrop(drop, level.registryAccess());
            Block.popResource(level, pos, drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ModBlockEntities.STORAGE_FLUID_PORT.create(blockPos, blockState);
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
            ModBlockEntities.STORAGE_FLUID_PORT.get(),
            (level1, pos, state1, entity) -> entity.tickServer()
        );
    }
}

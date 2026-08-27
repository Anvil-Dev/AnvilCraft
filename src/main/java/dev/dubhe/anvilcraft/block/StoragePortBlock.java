package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 仓储端口方块。
 *
 * <p>面相邻潜影集装箱 / 超维存储站（可沿端口链延伸）使用；右键物品标记并塞入、
 * 双击塞入全部、左键取出；铁砧锤长按右键并滑动可去掉标记（普通锤右键不改变状态）；
 * 拆除时缓存与标记保留在掉落物中。</p>
 */
public class StoragePortBlock extends BaseEntityBlock implements IHammerRemovable {
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
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof StoragePortBlockEntity port)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 铁砧锤：普通右键不调整任何状态（去标记需长按右键并滑动，见客户端手势）
        if (stack.getItem() instanceof AnvilHammerItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        boolean doubleClick = port.isDoubleClick(player);
        ItemStack mark = port.getMarkedItem();
        if (mark.isEmpty()) {
            // 未标记：手持物品右键 → 标记并塞入最多一组
            if (!stack.isEmpty()) {
                port.setMarkedItem(stack);
                port.stuffFromHand(stack, stack.getMaxStackSize());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (stack.isEmpty()) {
            // 已标记 + 空手：单击不做任何事（取出走左键）；双击塞入身上全部
            // （第一次点击已把手上的物品塞入并完成标记，故第二次点击时手可能已空）
            if (doubleClick) {
                port.stuffAllFromPlayer(player);
                if (!level.isClientSide()) {
                    port.setChanged();
                    level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (ItemStack.isSameItemSameComponents(mark, stack)) {
            // 对应物品：单击塞入最多一组，双击塞入身上全部
            if (doubleClick) {
                port.stuffAllFromPlayer(player);
            } else {
                port.stuffFromHand(stack, stack.getMaxStackSize());
            }
            if (!level.isClientSide()) {
                port.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        // 不对应物品：不替换标记
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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

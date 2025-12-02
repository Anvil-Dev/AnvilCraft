package dev.dubhe.anvilcraft.block.multipart;

import dev.dubhe.anvilcraft.block.state.ISimpleMultiPartBlockState;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleMultiPartBlock<P extends Enum<P> & ISimpleMultiPartBlockState<P>>
    extends AbstractMultiPartBlock<P> {
    public SimpleMultiPartBlock(Properties properties) {
        super(properties);
    }

    public Vec3i getMainPartOffset() {
        return new Vec3i(0, 0, 0);
    }

    @Override
    public Vec3i offsetFrom(BlockState state, P part) {
        return part.getOffset().subtract(this.getOffset(state));
    }

    @Override
    public Vec3i getOffset(BlockState state) {
        return state.getValue(this.getPart()).getOffset();
    }

    @Override
    public boolean isMainPart(BlockState state) {
        return this.getOffset(state).equals(this.getMainPartOffset());
    }

    @Override
    public BlockPos getMainPartPos(BlockPos pos, BlockState state) {
        return pos.subtract(this.getOffset(state)).offset(this.getMainPartOffset());
    }

    /**
     * 获取多方块战利品表
     *
     * @param provider 提供器
     * @param block    方块
     */
    public static <T extends Enum<T> & ISimpleMultiPartBlockState<T>> void loot(
        BlockLootSubProvider provider, SimpleMultiPartBlock<T> block
    ) {
        for (T part : block.getParts()) {
            if (part.getOffset().distSqr(block.getMainPartOffset()) == 0) {

                provider.add(block, provider.createSinglePropConditionTable(block, block.getPart(), part));
                break;
            }
        }
    }

    @Nullable
    @Override
    public final BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!hasEnoughSpace(context.getClickedPos(), context.getLevel())) return null; // 判断是否有足够空间放置方块
        return this.getPlacementState(context);
    }

    @Nullable
    public BlockState getPlacementState(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    /**
     * 是否有足够的空间放下方块
     */
    public boolean hasEnoughSpace(BlockPos pos, LevelReader level) {
        for (P part : getParts()) {
            BlockPos pos1 = pos.offset(part.getOffset());
            if (level.isOutsideBuildHeight(pos1)) return false;
            BlockState state = level.getBlockState(pos1);
            if (!state.canBeReplaced()) {
                return false;
            }
        }
        return true;
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
        return Util.interactionResultConverter().apply(this.use(state, level, pos, player, hand, hitResult));
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        return this.use(state, level, pos, player, InteractionHand.MAIN_HAND, hitResult);
    }

    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        return InteractionResult.PASS;
    }
}

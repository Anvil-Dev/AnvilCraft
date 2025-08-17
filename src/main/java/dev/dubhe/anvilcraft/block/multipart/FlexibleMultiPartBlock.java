package dev.dubhe.anvilcraft.block.multipart;

import dev.dubhe.anvilcraft.block.state.IFlexibleMultiPartBlockState;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class FlexibleMultiPartBlock<
    P extends Enum<P> & IFlexibleMultiPartBlockState<P, E>,
    T extends Property<E>,
    E extends Comparable<E>
    > extends AbstractMultiPartBlock<P> {
    final P mainPart;

    public FlexibleMultiPartBlock(Properties properties) {
        super(properties);
        this.mainPart = Arrays.stream(getParts()).filter(IFlexibleMultiPartBlockState::isMain).findFirst().orElse(null);
    }

    public abstract Property<P> getPart();

    public abstract P[] getParts();

    public abstract T getAdditionalProperty();

    public <J extends Property<H>, H extends Comparable<H>> void updateState(Level level, BlockPos pos, J property, H value, int flag) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return;
        state = state.setValue(property, value);
        for (P part : getParts()) {
            BlockPos partPos = pos.offset(this.offsetFrom(state, part));
            if (level.getBlockState(partPos).is(this))
                level.setBlock(partPos, state.setValue(getPart(), part), flag);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(getPart(), getAdditionalProperty());
    }

    @Override
    public Vec3i offsetFrom(BlockState state, P part) {
        return part.getOffset(state.getValue(this.getAdditionalProperty())).subtract(this.getOffset(state));
    }

    @Override
    public Vec3i getOffset(@NotNull BlockState state) {
        return state.getValue(this.getPart()).getOffset(state.getValue(this.getAdditionalProperty()));
    }

    @Override
    public boolean isMainPart(BlockState state) {
        return state.getValue(this.getPart()).isMain();
    }

    @Override
    public BlockPos getMainPartPos(BlockPos pos, BlockState state) {
        return pos.subtract(this.getOffset(state))
            .offset(this.mainPart.getOffset(state.getValue(this.getAdditionalProperty())));
    }

    /**
     * 获取多方块战利品表
     *
     * @param provider 提供器
     * @param block    方块
     */
    public static <P extends Enum<P> & IFlexibleMultiPartBlockState<P, E>, T extends Property<E>, E extends Comparable<E>> void loot(
        BlockLootSubProvider provider, FlexibleMultiPartBlock<P, T, E> block
    ) {
        for (P part : block.getParts()) {
            if (part.isMain()) {
                provider.add(block, provider.createSinglePropConditionTable(block, block.getPart(), part));
                break;
            }
        }
    }

    @Nullable
    public BlockState getPlacementState(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    /**
     * 是否有足够的空间放下方块
     */
    public boolean hasEnoughSpace(BlockState originState, BlockPos pos, LevelReader level) {
        for (P part : getParts()) {
            BlockPos pos1 = pos.offset(this.offsetFrom(originState, part));
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
        ItemStack pStack,
        BlockState pState,
        Level pLevel,
        BlockPos pPos,
        Player pPlayer,
        InteractionHand pHand,
        BlockHitResult pHitResult) {
        return Util.interactionResultConverter().apply(this.use(pState, pLevel, pPos, pPlayer, pHand, pHitResult));
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState pState,
        Level pLevel,
        BlockPos pPos,
        Player pPlayer,
        BlockHitResult pHitResult
    ) {
        return this.use(pState, pLevel, pPos, pPlayer, InteractionHand.MAIN_HAND, pHitResult);
    }

    @SuppressWarnings("unused")
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

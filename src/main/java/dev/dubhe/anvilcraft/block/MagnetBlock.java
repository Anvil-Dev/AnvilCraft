package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MagnetBlock extends Block implements IHammerRemovable {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public MagnetBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(LIT, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override

    public void onPlace(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        this.attract(state, level, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide) {
            return;
        }
        this.attract(state, level, pos);
        boolean bl = state.getValue(LIT);
        if (bl != level.hasNeighborSignal(pos)) {
            if (bl) {
                level.scheduleTick(pos, this, 4);
            } else {
                level.setBlockAndUpdate(pos, state.cycle(LIT));
            }
        }
    }

    private void attract(BlockState state, Level level, BlockPos magnetPos) {
        if (level.isClientSide()) return;
        if (!(state.getBlock() instanceof MagnetBlock) || state.getValue(LIT)) return;
        if (level.getBlockState(magnetPos.below()).is(BlockTags.ANVIL)) return;
        int distance = AnvilCraft.CONFIG.magnetAttractsDistance;
        BlockPos currentPos = magnetPos;
        checkAnvil:
        for (int i = 0; i < distance; i++) {
            currentPos = currentPos.below();
            BlockState state1 = level.getBlockState(currentPos);

            if (state1.is(BlockTags.ANVIL) && !state1.is(ModBlockTags.NON_MAGNETIC)) {
                level.destroyBlock(magnetPos.below(), true);
                level.setBlockAndUpdate(magnetPos.below(), state1);
                level.setBlockAndUpdate(currentPos, Blocks.AIR.defaultBlockState());

                AnimateAscendingBlockEntity.animate(level, currentPos, state1, magnetPos.below());
                break;
            }
            List<FallingBlockEntity> entities =
                level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(currentPos));
            for (FallingBlockEntity entity : entities) {
                BlockState state2 = entity.getBlockState();
                if (state2.is(BlockTags.ANVIL) && !state2.is(ModBlockTags.NON_MAGNETIC)) {
                    level.destroyBlock(magnetPos.below(), true);
                    level.setBlockAndUpdate(magnetPos.below(), state2);
                    entity.remove(Entity.RemovalReason.DISCARDED);
                    AnimateAscendingBlockEntity.animate(level, currentPos, state2, magnetPos.below());
                    break checkAnvil;
                }
            }
            TriggerUtil.liftingAnvil(level, currentPos);
            BlockState blockState = level.getBlockState(currentPos);
            if (level.isEmptyBlock(currentPos) || blockState.getBlock() instanceof LiquidBlock) {
                continue;
            }
            return;
        }
    }


    @Override
    public void onRemove(
        BlockState state,
        Level level,
        BlockPos magnetPos,
        BlockState newState,
        boolean movedByPiston
    ) {
        super.onRemove(state, level, magnetPos, newState, movedByPiston);
        if (level.isClientSide()) return;
        int distance = AnvilCraft.CONFIG.magnetAttractsDistance;
        BlockPos currentPos = magnetPos;
        for (int i = 0; i < distance; i++) {
            currentPos = currentPos.below();
            List<AnimateAscendingBlockEntity> entities =
                level.getEntitiesOfClass(AnimateAscendingBlockEntity.class, new AABB(currentPos));
            for (AnimateAscendingBlockEntity entity : entities) {
                entity.discard();
            }
            if (!level.isEmptyBlock(currentPos)) return;
        }
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        if (state.getValue(LIT) && !level.hasNeighborSignal(pos)) {
            level.setBlockAndUpdate(pos, state.cycle(LIT));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (state.is(ModBlocks.MAGNET_BLOCK)) {
                if (player.isShiftKeyDown()) {
                    player.addItem(ModItems.MAGNET_INGOT.get().getDefaultInstance());
                    BlockState blockState = ModBlocks.HOLLOW_MAGNET_BLOCK.get().defaultBlockState();
                    if (blockState.hasProperty(LIT)) {
                        blockState = blockState.setValue(LIT, level.hasNeighborSignal(pos));
                    }
                    level.setBlockAndUpdate(pos, blockState);
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}

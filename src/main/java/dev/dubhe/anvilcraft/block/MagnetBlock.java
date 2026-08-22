package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.entity.IAnvilCraftEntityExtension;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MagnetBlock extends Block implements IHammerRemovable {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final Map<Block, Double> CHARGE_NUMS = new HashMap<>();

    static {
        CHARGE_NUMS.put(Blocks.COPPER_BLOCK, 1d / 4);
        CHARGE_NUMS.put(Blocks.EXPOSED_COPPER, 1d / 8);
        CHARGE_NUMS.put(Blocks.WEATHERED_COPPER, 1d / 16);
    }

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
        if (distance <= 0) return;
        BlockPos bottomPos = magnetPos.below(distance);
        List<FallingBlockEntity> entities = level.getEntitiesOfClass(
            FallingBlockEntity.class,
            new AABB(
                bottomPos.getX(), bottomPos.getY(), bottomPos.getZ(),
                magnetPos.getX() + 1, magnetPos.getY(), magnetPos.getZ() + 1
            )
        );
        BlockPos currentPos = magnetPos;
        checkAnvil:
        for (int i = 0; i < distance; i++) {
            currentPos = currentPos.below();
            BlockState currentState = level.getBlockState(currentPos);

            if (currentState.is(BlockTags.ANVIL) && !currentState.is(ModBlockTags.NON_MAGNETIC)) {
                level.destroyBlock(magnetPos.below(), true);
                level.setBlockAndUpdate(magnetPos.below(), currentState);
                level.setBlockAndUpdate(currentPos, Blocks.AIR.defaultBlockState());

                AnimateAscendingBlockEntity.animate(level, currentPos, currentState, magnetPos.below());
                TriggerUtil.liftingAnvil(level, currentPos);
                break;
            }
            AABB currentBox = new AABB(currentPos);
            for (FallingBlockEntity entity : entities) {
                if (!entity.getBoundingBox().intersects(currentBox)) continue;
                if (entity instanceof IAnvilCraftEntityExtension) continue;
                BlockState state2 = entity.getBlockState();
                if (state2.is(BlockTags.ANVIL) && !state2.is(ModBlockTags.NON_MAGNETIC)) {
                    level.destroyBlock(magnetPos.below(), true);
                    level.setBlockAndUpdate(magnetPos.below(), state2);
                    entity.discard();
                    AnimateAscendingBlockEntity.animate(level, currentPos, state2, magnetPos.below());
                    TriggerUtil.liftingAnvil(level, currentPos);
                    break checkAnvil;
                }
            }
            if (currentState.isAir() || currentState.getBlock() instanceof LiquidBlock) {
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
        if (movedByPiston && !state.getValue(LIT)) {
            chargeFromCopper(level, magnetPos);
        }
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

    private static void chargeFromCopper(Level level, BlockPos magnetPos) {
        for (Direction face : Direction.values()) {
            BlockPos copperPos = magnetPos.relative(face);
            Block block = level.getBlockState(copperPos).getBlock();
            if (!CHARGE_NUMS.containsKey(block)) continue;
            ChargeCollectorManager.charge(CHARGE_NUMS.get(block), level, copperPos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.is(ModBlocks.MAGNET_BLOCK) && player.isShiftKeyDown()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            player.addItem(ModItems.MAGNET_INGOT.get().getDefaultInstance());
            BlockState blockState = ModBlocks.HOLLOW_MAGNET_BLOCK.get().defaultBlockState();
            if (blockState.hasProperty(LIT)) {
                blockState = blockState.setValue(LIT, level.hasNeighborSignal(pos));
            }
            level.setBlockAndUpdate(pos, blockState);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}

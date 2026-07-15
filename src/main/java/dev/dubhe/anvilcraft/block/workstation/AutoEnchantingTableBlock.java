package dev.dubhe.anvilcraft.block.workstation;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class AutoEnchantingTableBlock extends BetterBaseEntityBlock {
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;

    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 12.0);

    public static final List<BlockPos> BOOKSHELF_OFFSETS = BlockPos.betweenClosedStream(-2, 0, -2, 2, 1, 2)
        .filter(pos -> Math.abs(pos.getX()) == 2 || Math.abs(pos.getZ()) == 2)
        .map(BlockPos::immutable)
        .toList();

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(AutoEnchantingTableBlock::new);
    }

    public AutoEnchantingTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(OVERLOAD, true));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof AutoEnchantingTableBlockEntity autoEnchantingTableBlockEntity) {
            if (autoEnchantingTableBlockEntity.onPlayerUse(player, hand)) {
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AutoEnchantingTableBlockEntity autoEnchantingTableBlockEntity) {
            if (player instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    return InteractionResult.PASS;
                }
                ModMenuTypes.open(serverPlayer, autoEnchantingTableBlockEntity, pos);
                autoEnchantingTableBlockEntity.setOpenMenu(true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        for (BlockPos offset : BOOKSHELF_OFFSETS) {

            if (random.nextInt(16) == 0 && EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                level.addParticle(
                    ParticleTypes.ENCHANT,
                    pos.getX() + 0.5,
                    pos.getY() + 2.0,
                    pos.getZ() + 0.5,
                    offset.getX() + random.nextFloat() - 0.5,
                    offset.getY() - random.nextFloat() - 1.0F,
                    offset.getZ() + random.nextFloat() - 0.5
                );
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return ModBlockEntities.AUTO_ENCHANTING_TABLE.create(worldPosition, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        return level.isClientSide()
            ? createTickerHelper(
                type, ModBlockEntities.AUTO_ENCHANTING_TABLE.get(),
            (world, pos, state, be) -> be.bookAnimationTick(world, pos, state))
            : createTickerHelper(
                type, ModBlockEntities.AUTO_ENCHANTING_TABLE.get(), (
                    world, pos, state, be) -> be.serverTick(world, pos, state));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OVERLOAD);
    }
}

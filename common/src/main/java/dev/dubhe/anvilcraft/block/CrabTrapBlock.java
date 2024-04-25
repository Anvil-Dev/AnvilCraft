package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import dev.dubhe.anvilcraft.api.depository.ItemDepositoryHelper;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModLootContextParamSet;
import dev.dubhe.anvilcraft.init.ModLootTables;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CrabTrapBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public CrabTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(WATERLOGGED);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int times = 0;
        for (Direction face : Direction.values()) {
            if (level.getFluidState(pos.relative(face)).is(Fluids.WATER)) times++;
        }

        if (times >= 3) {
            // 获取战利品并放入 block entity
            tryInsertLoot(state, level, pos, ModLootTables.CRAB_TRAP_COMMON);
            tryInsertLoot(state, level, pos, ModLootTables.CRAB_TRAP_RIVER);
            tryInsertLoot(state, level, pos, ModLootTables.CRAB_TRAP_OCEAN);
            tryInsertLoot(state, level, pos, ModLootTables.CRAB_TRAP_WARM_OCEAN);
            tryInsertLoot(state, level, pos, ModLootTables.CRAB_TRAP_SWAMP);
            tryInsertLoot(state, level, pos, ModLootTables.CRAB_TRAP_JUNGLE);
        }
    }

    @Override
    public InteractionResult use(
        BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    ) {
        if (!level.isClientSide()) {
            CrabTrapBlockEntity blockEntity = (CrabTrapBlockEntity) level.getBlockEntity(pos);
            if (blockEntity != null) {
                ItemDepository depository = blockEntity.getDepository();
                NonNullList<ItemStack> itemStacks = depository.getStacks();
                for (int i = 0; i < itemStacks.size(); i++) {
                    ItemStack stack = itemStacks.get(i);
                    if (stack.isEmpty()) continue;
                    Vec3 center = pos.relative(Direction.UP).getCenter();
                    ItemEntity itemEntity = new ItemEntity(level, center.x(), center.y(), center.z(), stack, 0, 0.2, 0);
                    itemEntity.setDefaultPickUpDelay();
                    level.addFreshEntity(itemEntity);
                    depository.extract(i, stack.getCount(), false);
                }
            }

        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CrabTrapBlockEntity.createBlockEntity(ModBlockEntities.CRAB_TRAP.get(), pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock())) return;
        if (level.getBlockEntity(pos) instanceof CrabTrapBlockEntity entity) {
            Vec3 vec3 = entity.getBlockPos().getCenter();
            ItemDepository depository = entity.getDepository();
            for (int slot = 0; slot < depository.getSlots(); slot++) {
                Containers.dropItemStack(level, vec3.x, vec3.y, vec3.z, depository.getStack(slot));
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void tryInsertLoot(BlockState state, ServerLevel level, BlockPos pos, ResourceLocation loot) {
        if (state.hasBlockEntity()) {
            LootParams lootParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                .create(ModLootContextParamSet.CRAB_TRAP);

            LootTable lootTable = level.getServer().getLootData().getLootTable(loot);
            ObjectArrayList<ItemStack> items = lootTable.getRandomItems(lootParams);
            if (items.isEmpty()) return;
            CrabTrapBlockEntity blockEntity = (CrabTrapBlockEntity) level.getBlockEntity(pos);
            for (ItemStack item : items) {
                ItemDepositoryHelper.insertItem(blockEntity.getDepository(), item, false);
            }
        }
    }
}

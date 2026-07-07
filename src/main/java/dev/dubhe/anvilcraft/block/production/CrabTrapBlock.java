package dev.dubhe.anvilcraft.block.production;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.loot.ModLootTables;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CrabTrapBlock extends Block implements SimpleWaterloggedBlock, IHammerRemovable {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final IntegerProperty FISHING = IntegerProperty.create("fishing", 0, 15);

    public CrabTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(FISHING, 0)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(
                WATERLOGGED,
                context.getLevel()
                    .getFluidState(context.getClickedPos())
                    .getType()
                    == Fluids.WATER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, FISHING);
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
    public void randomTick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        int times = 0;
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(Fluids.WATER)) {
                times++;
            }
        }

        if (times >= 3 && state.getValue(FISHING) < 15) {
            level.setBlock(pos, state.setValue(FISHING, state.getValue(FISHING) + 1), 2);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(FISHING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        List<ItemStack> items = new ObjectArrayList<>();
        for (int i = 1; i < state.getValue(FISHING) + 1; i++) {
            items.addAll(generateLoot((ServerLevel) level, pos, ModLootTables.CRAB_TRAP_COMMON));
            items.addAll(generateLoot((ServerLevel) level, pos, ModLootTables.CRAB_TRAP_RIVER));
            items.addAll(generateLoot((ServerLevel) level, pos, ModLootTables.CRAB_TRAP_OCEAN));
            items.addAll(generateLoot((ServerLevel) level, pos, ModLootTables.CRAB_TRAP_WARM_OCEAN));
            items.addAll(generateLoot((ServerLevel) level, pos, ModLootTables.CRAB_TRAP_SWAMP));
            items.addAll(generateLoot((ServerLevel) level, pos, ModLootTables.CRAB_TRAP_JUNGLE));
            if (i % 5 == 0) {
                items.add(ModItems.CRAB_CLAW.asStack());
            }
        }
        Vec3 center = pos.relative(Direction.UP).getCenter();
        for (ItemStack item : items) {
            ItemEntity itemEntity = new ItemEntity(level, center.x, center.y, center.z, item, 0, 0.2, 0);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
        level.setBlockAndUpdate(pos, state.setValue(FISHING, 0));
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return this.rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    public static List<ItemStack> generateLoot(ServerLevel level, BlockPos pos, ResourceKey<LootTable> loot) {
        LootParams lootParams = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, pos.getCenter())
            .create(LootContextParamSets.CHEST);

        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(loot);
        return lootTable.getRandomItems(lootParams);
    }
}

package dev.dubhe.anvilcraft.block.container.storage;

import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumBlockLootTables;
import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.storage.HyperdimensionStorageStationBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;

public class HyperdimensionStorageStationBlock
    extends SimpleMultiPartBlock<Cube3x3PartHalf>
    implements MultiPartBlockEntity<Cube3x3PartHalf, HyperdimensionStorageStationBlock>, IHammerRemovable {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);

    public static void loot(RegistrumBlockLootTables tables, HyperdimensionStorageStationBlock block) {
        for (Cube3x3PartHalf part : block.getParts()) {
            if (part.getOffset().distSqr(block.getMainPartOffset()) != 0) continue;
            tables.add(block, LootTable.lootTable()
                .withPool(tables.applyExplosionCondition(block, LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f)))
                    .add(LootItem.lootTableItem(block)
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                            .setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(block.getPart(), part)))
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                            .include(ModComponents.STORAGE)))));
            break;
        }
    }

    public HyperdimensionStorageStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Property<Cube3x3PartHalf> getPart() {
        return HyperdimensionStorageStationBlock.HALF;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HyperdimensionStorageStationBlock.HALF);
    }

    @Override
    public HyperdimensionStorageStationBlock getMultiBlock() {
        return this;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.HYPERDIMENSION_STORAGE_STATION.create(pos, state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel) {
            BlockPos mainPos = this.getMainPartPos(pos, state);
            BlockState mainState = level.getBlockState(mainPos);
            if (mainState.is(this) && level.getBlockEntity(mainPos) instanceof HyperdimensionStorageStationBlockEntity storage) {
                if (storage.getTotalCount() == 0) {
                    storage.clearId();
                }
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        BlockEntity blockEntity = level.getBlockEntity(this.getMainPartPos(pos, state));
        if (blockEntity instanceof HyperdimensionStorageStationBlockEntity entity) {
            if (player.isSpectator()) return InteractionResult.PASS;
            if (player instanceof ServerPlayer) {
                return InteractionResult.sidedSuccess(false);
            } else if (level.isClientSide()) {
                level.playSound(player, pos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                DistExecutor.run(Dist.CLIENT, () -> () -> StorageScreen.openScreen(entity.getBlockPos()));
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(state, level, pos, player, hand, hitResult);
    }

    // region VoxelShapes
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HALF)) {
            case BOTTOM_CENTER -> BOTTOM_CENTER;
            case BOTTOM_W -> BOTTOM_W;
            case BOTTOM_E -> BOTTOM_E;
            case BOTTOM_N -> BOTTOM_N;
            case BOTTOM_S -> BOTTOM_S;
            case BOTTOM_WN -> BOTTOM_NW;
            case BOTTOM_WS -> BOTTOM_SW;
            case BOTTOM_EN -> BOTTOM_NE;
            case BOTTOM_ES -> BOTTOM_SE;
            case MID_CENTER -> MID_CENTER;
            case MID_W -> MID_W;
            case MID_E -> MID_E;
            case MID_N -> MID_N;
            case MID_S -> MID_S;
            case MID_WN -> MID_NW;
            case MID_WS -> MID_SW;
            case MID_EN -> MID_NE;
            case MID_ES -> MID_SE;
            case TOP_CENTER -> TOP_CENTER;
            case TOP_W -> TOP_W;
            case TOP_E -> TOP_E;
            case TOP_N -> TOP_N;
            case TOP_S -> TOP_S;
            case TOP_WN -> TOP_NW;
            case TOP_WS -> TOP_SW;
            case TOP_EN -> TOP_NE;
            case TOP_ES -> TOP_SE;
        };
    }

    protected static final VoxelShape MID_CENTER = Shapes.block();

    protected static final VoxelShape BOTTOM_N = ShapeUtil.merge(
        new AABB(0, 3.5, 3.5, 2.5, 6.5, 6.5),
        new AABB(2.5, 2, 2, 13.5, 8, 8),
        new AABB(13.5, 3.5, 3.5, 16, 6.5, 6.5),

        new AABB(0, 14, 4, 16, 16, 16),
        new AABB(0, 4, 14, 16, 16, 16),

        new AABB(4, 7, 7, 12, 14, 14)
    );
    protected static final VoxelShape BOTTOM_W = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_N);
    protected static final VoxelShape BOTTOM_S = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_N);
    protected static final VoxelShape BOTTOM_E = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_N);

    protected static final VoxelShape BOTTOM_NW = ShapeUtil.merge(
        new AABB(0, 0, 0, 10, 10, 10),
        new AABB(3.5, 3.5, 11, 6.5, 6.5, 16),
        new AABB(3.5, 11, 3.5, 6.5, 16, 6.5),
        new AABB(11, 3.5, 3.5, 16, 6.5, 6.5),

        new AABB(4, 14, 14, 16, 16, 16),
        new AABB(14, 4, 14, 16, 16, 16),
        new AABB(14, 14, 4, 16, 16, 16)
    );
    protected static final VoxelShape BOTTOM_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_NW);
    protected static final VoxelShape BOTTOM_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_NW);
    protected static final VoxelShape BOTTOM_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_NW);

    protected static final VoxelShape MID_N = Block.box(0, 0, 0, 16, 16, 4);
    protected static final VoxelShape MID_W = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_N);
    protected static final VoxelShape MID_S = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_N);
    protected static final VoxelShape MID_E = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_N);
    protected static final VoxelShape BOTTOM_CENTER = ShapeUtil.rotate(Direction.Axis.X, 90, MID_N);
    protected static final VoxelShape TOP_CENTER = ShapeUtil.rotate(Direction.Axis.X, 270, MID_N);

    protected static final VoxelShape MID_NW = ShapeUtil.rotate(Direction.Axis.Z, 90, BOTTOM_N);
    protected static final VoxelShape MID_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_NW);
    protected static final VoxelShape MID_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_NW);
    protected static final VoxelShape MID_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_NW);

    protected static final VoxelShape TOP_N = ShapeUtil.rotate(Direction.Axis.Z, 180, BOTTOM_N);
    protected static final VoxelShape TOP_W = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_N);
    protected static final VoxelShape TOP_S = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_N);
    protected static final VoxelShape TOP_E = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_N);

    protected static final VoxelShape TOP_NW = ShapeUtil.rotate(Direction.Axis.X, 270, BOTTOM_NW);
    protected static final VoxelShape TOP_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_NW);
    protected static final VoxelShape TOP_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_NW);
    protected static final VoxelShape TOP_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_NW);
    // endregion
}

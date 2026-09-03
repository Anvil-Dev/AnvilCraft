package dev.dubhe.anvilcraft.block.container.storage;

import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumBlockLootTables;
import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.block.ITranscendiumBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.storage.HyperdimensionStorageStationBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import dev.dubhe.anvilcraft.item.HyperdimensionTerminalItem;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.saved.storage.Storages;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;

import java.util.UUID;

public class HyperdimensionStorageStationBlock
    extends SimpleMultiPartBlock<Cube3x3PartHalf>
    implements MultiPartBlockEntity<Cube3x3PartHalf, HyperdimensionStorageStationBlock>, IHammerRemovable, ITranscendiumBlock {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);

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
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        LevelReader level,
        BlockPos pos,
        Player player
    ) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        if (level instanceof Level realLevel) {
            StorageBlockEntity.applyPickStorageId(stack, realLevel, pos, state, ModStorageTypes.HYPERDIMENSION);
        }
        return stack;
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
        if (level instanceof ServerLevel serverLevel) {
            BlockPos mainPos = this.getMainPartPos(pos, state);
            BlockState mainState = level.getBlockState(mainPos);
            BlockEntity blockEntity = level.getBlockEntity(mainPos);
            if (mainState.is(this) && blockEntity instanceof HyperdimensionStorageStationBlockEntity storage) {
                boolean empty = storage.getTotalCount() == 0;
                if (empty) {
                    // 空容器：清除 id 并移除孤儿存储条目，避免存档膨胀
                    UUID id = storage.getId();
                    storage.clearId();
                    if (id != null) {
                        Storages.get().remove(id);
                    }
                } else if (player.hasInfiniteMaterials()) {
                    // 手动生成含 STORAGE 引用的容器物品掉落（与 tooltip 声明一致，拾取后放回仍可访问其存储内容）：
                    // 创造模式破坏从不产生原版战利品掉落；破坏子部件时其余 part 经 updateShape 逐个塌缩为空气，
                    // 不走产生掉落的破坏路径，且战利品表仅匹配主部件状态，因此这两种情况都不会自然掉落。
                    // 生存/冒险模式直接破坏主部件时由原版战利品表兜底，此处跳过以免重复掉落。
                    LootParams.Builder builder = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(mainPos))
                        .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
                    for (ItemStack stack : mainState.getDrops(builder)) {
                        Block.popResource(serverLevel, mainPos, stack);
                    }
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
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    HyperdimensionTerminalItem.bindToStation(serverPlayer, stack, entity);
                } else if (level.isClientSide()) {
                    TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
                    if (binding != null && binding.id().isPresent()) {
                        HyperdimensionTerminalItem.openBoundStorage(player, stack);
                    }
                }
                return InteractionResult.SUCCESS;
            }
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

    public static int getLightLevel(BlockState state) {
        return switch (state.getValue(HyperdimensionStorageStationBlock.HALF)) {
            case BOTTOM_CENTER, MID_N, MID_E, MID_S, MID_W, MID_CENTER, TOP_CENTER -> 2;
            case BOTTOM_WN, BOTTOM_EN, BOTTOM_ES, BOTTOM_WS, TOP_WN, TOP_EN, TOP_ES, TOP_WS -> 8;
            default -> 6;
        };
    }

    // region VoxelShapes
    @Override
    public VoxelShape getPartShape(BlockState state) {
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

    protected static final VoxelShape MID_N = Block.box(0, 0, 4, 16, 16, 16);
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

package dev.dubhe.anvilcraft.block.cfa;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.laser.PropelPistonBlock;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModMultiblockDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class CelestialForgingAnvilBlock
    extends SimpleMultiPartBlock<Cube323PartHalf>
    implements MultiPartBlockEntity<Cube323PartHalf, CelestialForgingAnvilBlock>, IHammerRemovable, IController {
    public static final EnumProperty<Cube323PartHalf> HALF = EnumProperty.create("half", Cube323PartHalf.class);
    public static final VoxelShape BOTTOM_NW = ShapeUtil.merge(
        new AABB(0, 0, 0, 16, 4, 16),
        new AABB(4, 4, 4, 16, 10, 16),
        new AABB(0, 10, 0, 10, 14, 10),

        new AABB(4, 14, 4, 8, 16, 8),
        new AABB(6, 14, 6, 10, 16, 10),
        new AABB(8, 14, 8, 12, 16, 12),

        new AABB(7, 10, 7, 16, 12.65, 16),
        new AABB(8, 12.65, 8, 16, 15.25, 16),

        new AABB(9, 10, 9, 16, 16, 16)
    );
    public static final VoxelShape BOTTOM_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_NW);
    public static final VoxelShape BOTTOM_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_NW);
    public static final VoxelShape BOTTOM_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_NW);

    public static final VoxelShape BOTTOM_N = ShapeUtil.merge(
        new AABB(0, 0, 0, 16, 4, 16),
        new AABB(0, 4, 4, 16, 10, 16),

        new AABB(0, 10, 7, 2, 12.65, 9),
        new AABB(0, 12.65, 8, 2, 15.25, 9),
        new AABB(4, 10, 7, 6, 12.65, 9),
        new AABB(4, 12.65, 8, 6, 15.25, 9),
        new AABB(7, 10, 7, 9, 12.65, 9),
        new AABB(7, 12.65, 8, 9, 15.25, 9),
        new AABB(10, 10, 7, 12, 12.65, 9),
        new AABB(10, 12.65, 8, 12, 15.25, 9),
        new AABB(14, 10, 7, 16, 12.65, 9),
        new AABB(14, 12.65, 8, 16, 15.25, 9),

        new AABB(0, 10, 9, 16, 16, 16)
    );
    public static final VoxelShape BOTTOM_W = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_N);
    public static final VoxelShape BOTTOM_S = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_N);
    public static final VoxelShape BOTTOM_E = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_N);

    public static final VoxelShape TOP_NW = ShapeUtil.merge(
        new AABB(9, 0, 9, 16, 6, 16),
        new AABB(14, 6, 14, 16, 10, 16),

        new AABB(4, 0, 4, 8, 13, 8),
        new AABB(6, 0, 6, 10, 13, 10),
        new AABB(8, 0, 8, 12, 13, 12),
        new AABB(10, 0, 10, 14, 13, 14)
    );
    public static final VoxelShape TOP_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_NW);
    public static final VoxelShape TOP_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_NW);
    public static final VoxelShape TOP_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_NW);

    public static final VoxelShape TOP_N = ShapeUtil.merge(
        new AABB(0, 0, 9, 16, 6, 16),
        new AABB(0, 6, 14, 16, 10, 16),
        new AABB(3, 0, 4, 13, 8, 16),
        new AABB(3, 8, 2, 13, 16, 12)
    );
    public static final VoxelShape TOP_W = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_N);
    public static final VoxelShape TOP_S = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_N);
    public static final VoxelShape TOP_E = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_N);

    public static final VoxelShape TOP_CENTER = Block.box(0, 0, 0, 16, 10, 16);

    public CelestialForgingAnvilBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(HALF, Cube323PartHalf.BOTTOM_CENTER));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HALF)) {
            case BOTTOM_CENTER -> Shapes.block();
            case BOTTOM_W -> BOTTOM_W;
            case BOTTOM_E -> BOTTOM_E;
            case BOTTOM_N -> BOTTOM_N;
            case BOTTOM_S -> BOTTOM_S;
            case BOTTOM_NW -> BOTTOM_NW;
            case BOTTOM_SW -> BOTTOM_SW;
            case BOTTOM_NE -> BOTTOM_NE;
            case BOTTOM_SE -> BOTTOM_SE;
            case TOP_CENTER -> TOP_CENTER;
            case TOP_W -> TOP_W;
            case TOP_E -> TOP_E;
            case TOP_N -> TOP_N;
            case TOP_S -> TOP_S;
            case TOP_NW -> TOP_NW;
            case TOP_SW -> TOP_SW;
            case TOP_NE -> TOP_NE;
            case TOP_SE -> TOP_SE;
        };
    }

    @Override
    public Property<Cube323PartHalf> getPart() {
        return HALF;
    }

    @Override
    public Cube323PartHalf[] getParts() {
        return Cube323PartHalf.values();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HALF, state.getValue(HALF).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(HALF, state.getValue(HALF).mirror(mirror));
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return PropelPistonBlock.createTickerHelper(
                type,
                ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(),
                (_, _, _, be) -> be.tick()
            );
        }
        return PropelPistonBlock.createTickerHelper(
            type,
            ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(),
            (_, _, _, be) -> be.serverTick()
        );
    }

    @Override
    public CelestialForgingAnvilBlock getMultiBlock() {
        return this;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CELESTIAL_FORGING_ANVIL.create(pos, state);
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public Identifier getDefinitionId() {
        return ModMultiblockDefinitions.CELESTIAL_FORGING_ANVIL.identifier();
    }

    // === 结构拆除：26.1 使用 playerWillDestroy 和 setRemoved ===

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos mainPos = getMainPartPos(pos, state);
        boolean isMain = state.hasProperty(HALF) && state.getValue(HALF) == Cube323PartHalf.BOTTOM_CENTER;
        if (isMain && level.getBlockEntity(mainPos) instanceof CelestialForgingAnvilBlockEntity be) {
            // 将全部内部物品栏内容掉落到世界。
            for (int i = 0; i < be.getAnvilInventory().getContainerSize(); i++) {
                ItemStack stack = be.getAnvilInventory().getItem(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                }
            }
            ItemStack matStack = be.getMaterialContainer().getItem(0);
            if (!matStack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, matStack);
            }

            // 掉落控制器物品并保留天体、巨构和匹配参数，使其重新放置后恢复；
            // 同时移除与当前位置或运行时绑定的临时字段。
            if (!level.isClientSide()) {
                final ItemStack blockStack = new ItemStack(asItem());
                CompoundTag beTag = be.saveCustomOnly(level.registryAccess());

                // 清除与原世界位置或瞬时运行状态绑定的数据。
                beTag.remove("anvils");               // inventory — already dropped above
                beTag.remove("materialFilter");       // UI state — resets on menu close
                beTag.remove("materialLimit");        // UI state
                beTag.remove("searchHistory");        // search history — not preserved
                beTag.remove("searching");            // runtime
                beTag.remove("searchTicks");          // runtime
                beTag.remove("searchFailed");         // runtime
                beTag.remove("powerInsufficient");    // runtime
                beTag.remove("amplifierPresent");     // depends on multiblock structure
                beTag.remove("activeMegastructure");  // depends on multiblock structure

                if (!beTag.isEmpty()) {
                    TagValueOutput output = TagValueOutput.createWithoutContext(
                        new ProblemReporter.ScopedCollector(be.problemPath(), LogUtils.getLogger()));
                    output.store(beTag);
                    BlockItem.setBlockEntityData(blockStack, be.getType(), output);
                }
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, blockStack);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // === 多方块结构生命周期 ===

    @Override
    public void onFormed(Level level, MultiblockState state) {
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.CELESTIAL_FORGING_ANVIL.get())
            .ifPresent(be -> {
                be.setAmplify(true);
                be.setAmplifierPresent(true);
                be.setChanged();
                if (!level.isClientSide()) {
                    be.syncToClient();
                }
            });
    }

    @Override
    public void onUnformed(Level level, MultiblockState state) {
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.CELESTIAL_FORGING_ANVIL.get())
            .ifPresent(be -> {
                be.setAmplifierPresent(false);
                if (be.getCelestialBodyData() instanceof StarData) {
                    be.removeGravitySource(); // invisible stars must stop producing gravity immediately
                    be.setLocked(true);
                    be.clearSearchHistory();
                    // 保留增幅标记和恒星数据；增幅器缺失时由渲染器隐藏天体。
                } else {
                    be.setAmplify(false);
                }
                be.setChanged();
                if (!level.isClientSide()) {
                    be.syncToClient();
                }
            });
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        // 使控制器红石缓存失效，让渲染器及时响应变化。
        if (level.getBlockEntity(getMainPartPos(pos, state)) instanceof CelestialForgingAnvilBlockEntity be) {
            be.markRedstoneSignalDirty();
        }
    }

    @Override
    public BlockPos correctPos(ServerLevel level, BlockPos pos, BlockState state) {
        return pos.offset(state.getValue(HALF).getOffset()).offset(this.getMainPartOffset());
    }

    // === 交互 ===

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        BlockPos mainPos = getMainPartPos(pos, state);
        BlockEntity be = level.getBlockEntity(mainPos);
        if (be instanceof CelestialForgingAnvilBlockEntity cfaBe) {
            // 磁盘右键交给 DiskItem.useOn 处理。
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult diskResult = cfaBe.useDisk(level, player, hand, stack, hit);
            if (diskResult == InteractionResult.SUCCESS) {
                return InteractionResult.SUCCESS;
            }
            if (diskResult == InteractionResult.FAIL) {
                return InteractionResult.FAIL;
            }
            // 奇点晶体右键只写入快照，应用仍必须通过种子槽。
            if (stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem())) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
                if (player.isShiftKeyDown()) return InteractionResult.PASS;
                CompoundTag stored = CelestialForgingAnvilBlockEntity.loadSnapshotFromStack(stack);
                if (stored != null) {
                    // 已有数据时不在此覆盖，要求玩家通过种子槽应用。
                    return InteractionResult.PASS;
                }
                TagValueOutput output = TagValueOutput.createWithoutContext(
                    new ProblemReporter.ScopedCollector(be.problemPath(), LogUtils.getLogger()));
                cfaBe.storeDiskData(output);
                CelestialForgingAnvilBlockEntity.saveSnapshotToStack(stack, output.buildResult());
                player.sendSystemMessage(Component.translatable("message.anvilcraft.disk.data_stored"));
                return InteractionResult.SUCCESS;
            }
        }

        // 未使用特殊物品时打开锻星砧界面。
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (be instanceof CelestialForgingAnvilBlockEntity cfaBe && player instanceof ServerPlayer sp) {
            ModMenuTypes.open(sp, cfaBe, mainPos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

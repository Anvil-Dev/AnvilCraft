package dev.dubhe.anvilcraft.block.cfa;

import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.PropelPiston;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModMultiblockDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

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
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return PropelPiston.createTickerHelper(
            type,
            ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(),
            (level1, blockPos, blockState, blockEntity) -> {
                if (level.isClientSide()) {
                    blockEntity.tick();
                } else {
                    blockEntity.serverTick();
                }
            }
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
    public ResourceLocation getDefinitionId() {
        return ModMultiblockDefinitions.CELESTIAL_FORGING_ANVIL.location();
    }

    @Override
    public void onFormed(Level level, MultiblockState state) {
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.CELESTIAL_FORGING_ANVIL.get())
            .ifPresent(be -> {
                be.setAmplify(true);
                be.setAmplifierPresent(true);
                be.setChanged();
                level.sendBlockUpdated(
                    state.getControllerPos(),
                    be.getBlockState(),
                    be.getBlockState(),
                    3
                );
            });
    }

    @Override
    public void onUnformed(Level level, MultiblockState state) {
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.CELESTIAL_FORGING_ANVIL.get())
            .ifPresent(be -> {
                be.setAmplifierPresent(false);
                if (be.getCelestialBodyData() instanceof StarData) {
                    be.setLocked(true);
                    be.getSearchHistory().clear();
                    // Keep isAmplify true so stellar body stays in data,
                    // but the renderer hides it when amplifier is missing
                } else {
                    be.setAmplify(false);
                }
                be.setChanged();
                level.sendBlockUpdated(
                    state.getControllerPos(),
                    be.getBlockState(),
                    be.getBlockState(),
                    3
                );
            });
    }

    @Override
    public BlockPos correctPos(ServerLevel level, BlockPos pos, BlockState state) {
        return pos.offset(state.getValue(HALF).getOffset()).offset(this.getMainPartOffset());
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock())) return;
        BlockPos mainPos = getMainPartPos(pos, state);
        boolean isMain = state.hasProperty(HALF) && state.getValue(HALF) == Cube323PartHalf.BOTTOM_CENTER;
        if (isMain && level.getBlockEntity(mainPos) instanceof CelestialForgingAnvilBlockEntity be) {
            // 1. Drop all inventory contents into the world
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

            // 2. Drop the block item, preserving celestial body, megastructure,
            //    and matching parameters so the body reappears when placed elsewhere.
            //    Runtime & position-dependent flags are stripped from the item tag.
            if (!level.isClientSide) {
                ItemStack blockStack = new ItemStack(asItem());
                CompoundTag beTag = new CompoundTag();
                be.saveAdditional(beTag, level.registryAccess());

                // Strip data that is tied to the current world position or transient runtime
                beTag.remove("anvils");               // inventory — already dropped above
                beTag.remove("materialFilter");       // UI state — resets on menu close
                beTag.remove("materialLimit");        // UI state
                beTag.remove("searchHistory");        // search history — not preserved
                beTag.remove("searching");            // runtime
                beTag.remove("searchTicks");          // runtime
                beTag.remove("searchFailed");         // runtime
                beTag.remove("powerInsufficient");    // runtime
                beTag.remove("excavatorLaserActive"); // depends on nearby laser blocks
                beTag.remove("amplifierPresent");     // depends on multiblock structure

                if (!beTag.isEmpty()) {
                    BlockItem.setBlockEntityData(blockStack, be.getType(), beTag);
                }
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, blockStack);
            }

            // 3. Wipe the block entity so a fresh CFA placed at this position
            //    starts with a clean slate. Chunk save/load is unaffected —
            //    saveAdditional/loadAdditional handle that path independently.
            be.clearPositionDependentData();
        }
        // Don't call super.onRemove() — we handle drops manually above
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
        BlockPos mainPos = getMainPartPos(pos, state);
        BlockEntity be = level.getBlockEntity(mainPos);
        if (be instanceof CelestialForgingAnvilBlockEntity cfaBe) {
            // Disk right-click: delegate to DiskItem.useOn
            if (cfaBe.useDisk(level, player, hand, stack, hitResult) == InteractionResult.SUCCESS) {
                return ItemInteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos mainPos = getMainPartPos(pos, state);
        BlockEntity be = level.getBlockEntity(mainPos);
        if (be instanceof CelestialForgingAnvilBlockEntity cfaBe && player instanceof ServerPlayer sp) {
            if (sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return InteractionResult.PASS;
            ModMenuTypes.open(sp, cfaBe, mainPos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

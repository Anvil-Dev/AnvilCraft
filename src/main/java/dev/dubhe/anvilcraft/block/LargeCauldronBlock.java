package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.block.ICauldron;
import dev.dubhe.anvilcraft.api.block.ICauldronGeometry;
import dev.dubhe.anvilcraft.api.event.LargeCauldronEvent;
import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static dev.dubhe.anvilcraft.block.PropelPistonBlock.createTickerHelper;

public class LargeCauldronBlock
    extends SimpleMultiPartBlock<Cube3x3PartHalf>
    implements MultiPartBlockEntity<Cube3x3PartHalf, LargeCauldronBlock>, IHammerRemovable, ICauldron, ICauldronGeometry {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);
    private static final double WALL_THICKNESS = 0.25;
    private static final double BOTTOM_WALL_MIN_Y = 0.5;
    private static final double CLIMBING_EPSILON = 1.0E-5;
    private static final Map<Cube3x3PartHalf, VoxelShape> SHAPES = createShapes();

    public LargeCauldronBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, Cube3x3PartHalf.BOTTOM_CENTER));
    }

    @Override
    public Vec3i getMainPartOffset() {
        return new Vec3i(0, 1, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
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
    public Property<Cube3x3PartHalf> getPart() {
        return HALF;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    public LargeCauldronBlock getMultiBlock() {
        return this;
    }

    @Override
    public boolean supportsMultipleFluidOutputs() {
        return true;
    }

    @Override
    public AABB getCauldronInnerArea(BlockPos pos, BlockState state) {
        BlockPos mainPos = this.getMainPartPos(pos, state);
        return new AABB(
            mainPos.getX() - 0.75,
            mainPos.getY() - 0.5,
            mainPos.getZ() - 0.75,
            mainPos.getX() + 1.75,
            mainPos.getY() + 1.75,
            mainPos.getZ() + 1.75
        );
    }

    @Override
    public List<BlockPos> getCauldronBottomPositions(BlockPos pos, BlockState state) {
        BlockPos center = this.getMainPartPos(pos, state).below(2);
        return BlockPos.betweenClosedStream(center.offset(-1, 0, -1), center.offset(1, 0, 1))
            .map(BlockPos::immutable)
            .toList();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.createBlockEntity(pos, state);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.LARGE_CAULDRON.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type
    ) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.LARGE_CAULDRON.get(), LargeCauldronBlockEntity::serverTick);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Cube3x3PartHalf part = state.getValue(HALF);
        if (part.getOffsetY() == 2 && context.isHoldingItem(ModBlocks.GIANT_ANVIL.asItem())) {
            return Shapes.block();
        }
        // 每个部件都要独立参与射线检测，否则整口锅的组合形状会抢占相邻部件的交互命中。
        return this.getPartShape(state);
    }

    @Override
    public VoxelShape getPartShape(BlockState state) {
        return SHAPES.get(state.getValue(HALF));
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return touchesWall(state, pos, entity.getBoundingBox());
    }

    public static @Nullable BlockPos findClimbableWall(LevelReader level, LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        BlockPos min = BlockPos.containing(
            box.minX - CLIMBING_EPSILON,
            box.minY - CLIMBING_EPSILON,
            box.minZ - CLIMBING_EPSILON
        );
        BlockPos max = BlockPos.containing(
            box.maxX + CLIMBING_EPSILON,
            box.maxY + CLIMBING_EPSILON,
            box.maxZ + CLIMBING_EPSILON
        );
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof LargeCauldronBlock && touchesWall(state, pos, box)) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static boolean touchesWall(BlockState state, BlockPos pos, AABB box) {
        Cube3x3PartHalf part = state.getValue(HALF);
        double wallMinY = pos.getY() + (part.getOffsetY() == 0 ? BOTTOM_WALL_MIN_Y : 0.0);
        double wallMaxY = pos.getY() + 1.0;
        if (!overlaps(box.minY, box.maxY, wallMinY, wallMaxY)) return false;

        if (part.getOffsetX() != 0 && overlaps(box.minZ, box.maxZ, pos.getZ(), pos.getZ() + 1.0)) {
            double wallMinX = pos.getX() + (part.getOffsetX() < 0 ? 0.0 : 1.0 - WALL_THICKNESS);
            double wallMaxX = wallMinX + WALL_THICKNESS;
            if (touches(box.minX, box.maxX, wallMinX, wallMaxX)) return true;
        }
        if (part.getOffsetZ() != 0 && overlaps(box.minX, box.maxX, pos.getX(), pos.getX() + 1.0)) {
            double wallMinZ = pos.getZ() + (part.getOffsetZ() < 0 ? 0.0 : 1.0 - WALL_THICKNESS);
            double wallMaxZ = wallMinZ + WALL_THICKNESS;
            return touches(box.minZ, box.maxZ, wallMinZ, wallMaxZ);
        }
        return false;
    }

    private static boolean touches(double min, double max, double wallMin, double wallMax) {
        return Math.abs(max - wallMin) <= CLIMBING_EPSILON || Math.abs(min - wallMax) <= CLIMBING_EPSILON;
    }

    private static boolean overlaps(double min, double max, double otherMin, double otherMax) {
        return max > otherMin + CLIMBING_EPSILON && min < otherMax - CLIMBING_EPSILON;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        BlockPos mainPos = this.getMainPartPos(pos, state);
        return level.getBlockEntity(mainPos) instanceof LargeCauldronBlockEntity cauldron
            ? cauldron.getLightLevel()
            : 0;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        LargeCauldronEvent.UseItem useItem = NeoForge.EVENT_BUS.post(new LargeCauldronEvent.UseItem(
            level,
            pos,
            state,
            player,
            hand,
            hit,
            stack
        ));
        if (useItem.isCanceled()) {
            return useItem.getResult();
        }
        if (stack.is(ModItemTags.ANVIL_HAMMER)) return ItemInteractionResult.SUCCESS;
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
        if (cauldron == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (cauldron.interactWithFluid(player, hand, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (FluidHandlerWrapper.isFluidInteractionItem(stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (isExtractionSurface(state, hit) && hand == InteractionHand.MAIN_HAND) {
            int slot = LargeCauldronBlockEntity.inputSlotForPart(state.getValue(HALF));
            if (stack.isEmpty()) {
                return cauldron.extractItemsToHand(player, hand, slot)
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide()) cauldron.insertFromHand(stack, slot);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (stack.is(ModBlocks.MENGER_SPONGE.asItem())) {
            if (cauldron.clearFluids() && !level.isClientSide()) {
                level.playSound(
                    null,
                    cauldron.getBlockPos(),
                    SoundEvents.SPONGE_ABSORB,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
                );
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (hand != InteractionHand.MAIN_HAND || stack.isEmpty() || !canInsertAt(state, hit)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int preferredSlot = LargeCauldronBlockEntity.inputSlotForPart(state.getValue(HALF));
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        return cauldron.insertFromHand(stack, preferredSlot)
            ? ItemInteractionResult.SUCCESS
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static boolean canInsertAt(BlockState state, BlockHitResult hit) {
        Cube3x3PartHalf part = state.getValue(HALF);
        if (part.getOffsetY() != 2) return false;
        return hit.getDirection() == Direction.UP || hit.getDirection().getAxis().isHorizontal();
    }

    private static boolean isExtractionSurface(BlockState state, BlockHitResult hit) {
        return state.getValue(HALF).getOffsetY() == 0 && hit.getDirection() == Direction.UP;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        Cube3x3PartHalf part = state.getValue(HALF);
        if (!isExtractionSurface(state, hit)) return InteractionResult.PASS;
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
        if (cauldron == null) return InteractionResult.PASS;
        int slot = LargeCauldronBlockEntity.inputSlotForPart(part);
        return cauldron.extractItemsToHand(player, InteractionHand.MAIN_HAND, slot)
            ? InteractionResult.sidedSuccess(level.isClientSide())
            : InteractionResult.PASS;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof ItemEntity item) {
            LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
            if (cauldron != null) {
                cauldron.absorbItem(item, LargeCauldronBlockEntity.inputSlotForPart(state.getValue(HALF)));
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        NeoForge.EVENT_BUS.post(new LargeCauldronEvent.EntityInside(level, pos, state, entity));
        if (level.isClientSide() || !(entity instanceof ItemEntity item)) return;
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
        if (cauldron != null) {
            cauldron.absorbItem(item, LargeCauldronBlockEntity.inputSlotForPart(state.getValue(HALF)));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && this.isMainPart(state)) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof LargeCauldronBlockEntity cauldron) cauldron.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static Map<Cube3x3PartHalf, VoxelShape> createShapes() {
        Map<Cube3x3PartHalf, VoxelShape> result = new EnumMap<>(Cube3x3PartHalf.class);
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            VoxelShape shape = part.getOffsetY() == 0
                ? Block.box(0, 0, 0, 16, 8, 16)
                : Shapes.empty();
            double minY = part.getOffsetY() == 0 ? 8 : 0;
            if (part.getOffsetX() == -1) shape = Shapes.or(shape, Block.box(0, minY, 0, 4, 16, 16));
            if (part.getOffsetX() == 1) shape = Shapes.or(shape, Block.box(12, minY, 0, 16, 16, 16));
            if (part.getOffsetZ() == -1) shape = Shapes.or(shape, Block.box(0, minY, 0, 16, 16, 4));
            if (part.getOffsetZ() == 1) shape = Shapes.or(shape, Block.box(0, minY, 12, 16, 16, 16));
            result.put(part, shape);
        }
        return result;
    }

    public static class Item extends SimpleMultiPartBlockItem<Cube3x3PartHalf> {
        public Item(LargeCauldronBlock block, Properties properties) {
            super(block, properties);
        }
    }
}

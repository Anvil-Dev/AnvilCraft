package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.block.ICauldron;
import dev.dubhe.anvilcraft.api.block.ICauldronGeometry;
import dev.dubhe.anvilcraft.api.event.LargeCauldronEvent;
import dev.dubhe.anvilcraft.api.fluid.FluidInteractionItems;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.laser.PropelPistonBlock;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.block.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.util.BlockPlacementPicking;
import dev.dubhe.anvilcraft.util.CauldronIgnition;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class LargeCauldronBlock
    extends SimpleMultiPartBlock<Cube3x3PartHalf>
    implements MultiPartBlockEntity<Cube3x3PartHalf, LargeCauldronBlock>, IHammerRemovable, ICauldronGeometry, ICauldron {
    @Override
    public boolean supportsMultipleFluidOutputs() {
        return true;
    }

    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);
    private static final double BOTTOM_WALL_MIN_Y = 0.5;
    private static final double CLIMBING_EPSILON = 1.0E-5;
    private static final Map<Cube3x3PartHalf, VoxelShape> SHAPES = LargeCauldronBlock.createShapes();

    public LargeCauldronBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LargeCauldronBlock.HALF, Cube3x3PartHalf.BOTTOM_CENTER));
    }

    @Override
    public Vec3i getMainPartOffset() {
        return new Vec3i(0, 1, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LargeCauldronBlock.HALF);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(LargeCauldronBlock.HALF, state.getValue(LargeCauldronBlock.HALF).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(LargeCauldronBlock.HALF, state.getValue(LargeCauldronBlock.HALF).mirror(mirror));
    }

    @Override
    public Property<Cube3x3PartHalf> getPart() {
        return LargeCauldronBlock.HALF;
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
        if (level.isClientSide() || !this.isMainPart(state)) {
            return null;
        }
        return PropelPistonBlock.createTickerHelper(
            type,
            ModBlockEntities.LARGE_CAULDRON.get(),
            LargeCauldronBlockEntity::serverTick
        );
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (BlockPlacementPicking.hasFullPlacementShape(state, context)) {
            return Shapes.block();
        }
        return this.getPartShape(state);
    }

    @Override
    public VoxelShape getPartShape(BlockState state) {
        return LargeCauldronBlock.SHAPES.get(state.getValue(LargeCauldronBlock.HALF));
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return LargeCauldronBlock.touchesWall(state, pos, entity.getBoundingBox());
    }

    public static @Nullable BlockPos findClimbableWall(LevelReader level, LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        BlockPos min = BlockPos.containing(
            box.minX - LargeCauldronBlock.CLIMBING_EPSILON,
            box.minY - LargeCauldronBlock.CLIMBING_EPSILON,
            box.minZ - LargeCauldronBlock.CLIMBING_EPSILON
        );
        BlockPos max = BlockPos.containing(
            box.maxX + LargeCauldronBlock.CLIMBING_EPSILON,
            box.maxY + LargeCauldronBlock.CLIMBING_EPSILON,
            box.maxZ + LargeCauldronBlock.CLIMBING_EPSILON
        );
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof LargeCauldronBlock && LargeCauldronBlock.touchesWall(state, pos, box)) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static boolean touchesWall(BlockState state, BlockPos pos, AABB box) {
        Cube3x3PartHalf part = state.getValue(HALF);
        double minY = part.getOffsetY() == 0 ? BOTTOM_WALL_MIN_Y : 0;
        for (AABB shape : SHAPES.get(part).toAabbs()) {
            AABB wall = shape.move(pos);
            double wallMinY = Math.max(wall.minY, pos.getY() + minY);
            if (wall.maxY <= wallMinY || !overlaps(box.minY, box.maxY, wallMinY, wall.maxY)) {
                continue;
            }
            if (overlaps(box.minZ, box.maxZ, wall.minZ, wall.maxZ)
                && touches(box.minX, box.maxX, wall.minX, wall.maxX)) {
                return true;
            }
            if (overlaps(box.minX, box.maxX, wall.minX, wall.maxX)
                && touches(box.minZ, box.maxZ, wall.minZ, wall.maxZ)) {
                return true;
            }
        }
        return false;
    }

    private static boolean touches(double min, double max, double wallMin, double wallMax) {
        return Math.abs(max - wallMin) <= LargeCauldronBlock.CLIMBING_EPSILON || Math.abs(min - wallMax)
                                                                                 <= LargeCauldronBlock.CLIMBING_EPSILON;
    }

    private static boolean overlaps(double min, double max, double otherMin, double otherMax) {
        return max > otherMin + LargeCauldronBlock.CLIMBING_EPSILON && min < otherMax - LargeCauldronBlock.CLIMBING_EPSILON;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
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
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
        if (cauldron == null) return 0;
        long amount = cauldron.getFluids().getTotalAmount();
        return amount == 0 ? 0 : (int) (14 * amount / LargeCauldronFluidHandler.totalCapacity()) + 1;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        var event = NeoForge.EVENT_BUS.post(new LargeCauldronEvent.UseItem(level, pos, state, player, hand, hit, stack));
        if (event.isCanceled()) return event.getResult();
        if (stack.is(ModItemTags.ANVIL_HAMMER)) return InteractionResult.SUCCESS;
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
        if (cauldron == null) return InteractionResult.PASS;
        if (CauldronIgnition.isShovel(stack) && cauldron.isIgnited()) {
            return CauldronIgnition.useShovelToExtinguish(
                level, pos, player, hand, stack, cauldron::extinguish);
        }
        if (CauldronIgnition.isIgnitionItem(stack) && cauldron.tryIgnite(level.isClientSide())) {
            return CauldronIgnition.finishIgnition(level, pos, player, hand, stack, 1);
        }
        if (cauldron.interactWithFluid(player, hand, hit)) {
            return Util.sidedSuccess(level);
        }
        // 手持桶/瓶等流体交互物品时吞掉交互，避免落到默认方块行为里误放置
        if (FluidInteractionItems.isFluidInteractionItem(stack)) {
            return Util.sidedSuccess(level);
        }
        if (LargeCauldronBlock.isExtractionSurface(state, hit) && hand == InteractionHand.MAIN_HAND) {
            int slot = LargeCauldronBlockEntity.inputSlotForPart(state.getValue(LargeCauldronBlock.HALF));
            if (stack.isEmpty()) {
                return cauldron.extractItemsToHand(player, hand, slot)
                    ? Util.sidedSuccess(level)
                    : InteractionResult.PASS;
            }
            if (!level.isClientSide()) cauldron.insertFromHand(stack, slot);
            return Util.sidedSuccess(level);
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
            return Util.sidedSuccess(level);
        }
        if (hand != InteractionHand.MAIN_HAND || stack.isEmpty() || !LargeCauldronBlock.canInsertAt(state, hit)) {
            return InteractionResult.PASS;
        }
        int preferredSlot = LargeCauldronBlockEntity.inputSlotForPart(state.getValue(LargeCauldronBlock.HALF));
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return cauldron.insertFromHand(stack, preferredSlot)
            ? InteractionResult.SUCCESS
            : InteractionResult.PASS;
    }

    private static boolean canInsertAt(BlockState state, BlockHitResult hit) {
        Cube3x3PartHalf part = state.getValue(LargeCauldronBlock.HALF);
        if (part.getOffsetY() != 2) return false;
        return hit.getDirection() == Direction.UP || hit.getDirection().getAxis().isHorizontal();
    }

    private static boolean isExtractionSurface(BlockState state, BlockHitResult hit) {
        return state.getValue(LargeCauldronBlock.HALF).getOffsetY() == 0 && hit.getDirection() == Direction.UP;
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
        Cube3x3PartHalf part = state.getValue(LargeCauldronBlock.HALF);
        if (!LargeCauldronBlock.isExtractionSurface(state, hit)) return InteractionResult.PASS;
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
        if (cauldron == null) return InteractionResult.PASS;
        int slot = LargeCauldronBlockEntity.inputSlotForPart(part);
        return cauldron.extractItemsToHand(player, InteractionHand.MAIN_HAND, slot)
            ? Util.sidedSuccess(level)
            : InteractionResult.PASS;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof ItemEntity item) {
            LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
            if (cauldron != null && this.getCauldronInnerArea(pos, state).intersects(item.getBoundingBox())) {
                cauldron.absorbItem(item, LargeCauldronBlockEntity.inputSlotForPart(state.getValue(LargeCauldronBlock.HALF)));
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier effectApplier,
        boolean isPrecise
    ) {
        NeoForge.EVENT_BUS.post(new LargeCauldronEvent.EntityInside(level, pos, state, entity));
        if (level.isClientSide() || !(entity instanceof ItemEntity item)) return;
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
        if (cauldron != null && this.getCauldronInnerArea(pos, state).intersects(item.getBoundingBox())) {
            cauldron.absorbItem(item, LargeCauldronBlockEntity.inputSlotForPart(state.getValue(LargeCauldronBlock.HALF)));
        }
    }

    private static Map<Cube3x3PartHalf, VoxelShape> createShapes() {
        // 模型的反向内壁表示空腔，必须先扣除内腔，再按部件切分以保留每格的交互含义。
        VoxelShape walls = Shapes.join(
            Block.box(-14, -8, -14, 30, 28, 30),
            Block.box(-12, -8, -12, 28, 28, 28),
            BooleanOp.ONLY_FIRST
        );
        VoxelShape model = Shapes.or(
            walls,
            Block.box(-16, -16, -16, -12, -12, -8),
            Block.box(-12, -16, -16, -8, -12, -12),
            Block.box(24, -16, -16, 32, -12, -12),
            Block.box(28, -16, -12, 32, -12, -8),
            Block.box(28, -16, 24, 32, -12, 32),
            Block.box(24, -16, 28, 28, -12, 32),
            Block.box(-16, -16, 28, -8, -12, 32),
            Block.box(-16, -16, 24, -12, -12, 28),
            Block.box(-16, -12, -16, 32, -8, 32),
            Block.box(-16, 28, -16, -12, 32, 28),
            Block.box(-12, 28, -16, 32, 32, -12),
            Block.box(28, 28, -12, 32, 32, 32),
            Block.box(-16, 28, 28, 28, 32, 32),
            Block.box(-16, -6, -16, -12, 26, -1),
            Block.box(-12, -6, -16, -1, 26, -12),
            Block.box(-16, -6, 17, -12, 26, 28),
            Block.box(-16, -6, 28, -1, 26, 32),
            Block.box(17, -6, 28, 28, 26, 32),
            Block.box(28, -6, 17, 32, 26, 32),
            Block.box(28, -6, -12, 32, 26, -1),
            Block.box(17, -6, -16, 32, 26, -12)
        );
        Map<Cube3x3PartHalf, VoxelShape> result = new EnumMap<>(Cube3x3PartHalf.class);
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            VoxelShape local = model.move(-part.getOffsetX(), 1 - part.getOffsetY(), -part.getOffsetZ());
            result.put(part, Shapes.join(local, Shapes.block(), BooleanOp.AND));
        }
        return result;
    }

    public static class Item extends SimpleMultiPartBlockItem<Cube3x3PartHalf> {
        public Item(LargeCauldronBlock block, Properties properties) {
            super(block, properties);
        }
    }
}

package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

import static dev.dubhe.anvilcraft.block.PropelPiston.createTickerHelper;

public class LargeCauldronBlock
    extends SimpleMultiPartBlock<Cube3x3PartHalf>
    implements MultiPartBlockEntity<Cube3x3PartHalf, LargeCauldronBlock>, IHammerRemovable {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);
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
        return SHAPES.get(state.getValue(HALF));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        if (stack.is(ModItemTags.ANVIL_HAMMER)) return ItemInteractionResult.SUCCESS;
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
        if (cauldron == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (cauldron.interactWithFluid(player, hand, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (hand != InteractionHand.MAIN_HAND || stack.isEmpty() || !canInsertAt(state, hit)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int preferredSlot = LargeCauldronBlockEntity.slotForPart(state.getValue(HALF));
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

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        Cube3x3PartHalf part = state.getValue(HALF);
        if (part.getOffsetY() != 0 || hit.getDirection() != Direction.UP) return InteractionResult.PASS;
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
        if (cauldron == null) return InteractionResult.PASS;
        int slot = LargeCauldronBlockEntity.slotForPart(part);
        return cauldron.extractItemsToHand(player, InteractionHand.MAIN_HAND, slot)
            ? InteractionResult.sidedSuccess(level.isClientSide())
            : InteractionResult.PASS;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof ItemEntity item) {
            LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
            if (cauldron != null) {
                cauldron.absorbItem(item, LargeCauldronBlockEntity.slotForPart(state.getValue(HALF)));
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof ItemEntity item)) return;
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
        if (cauldron != null) {
            cauldron.absorbItem(item, LargeCauldronBlockEntity.slotForPart(state.getValue(HALF)));
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

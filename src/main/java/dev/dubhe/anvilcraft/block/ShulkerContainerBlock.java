package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.CrateStorageReference;
import dev.dubhe.anvilcraft.util.ShapeUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ShulkerContainerBlock
    extends FlexibleMultiPartBlock<OpenedCube3x3PartHalf, BooleanProperty, Boolean>
    implements EntityBlock, IHammerRemovable {
    public static final EnumProperty<OpenedCube3x3PartHalf> HALF = EnumProperty.create("half", OpenedCube3x3PartHalf.class);
    public static final BooleanProperty OPENED = BooleanProperty.create("opened");
    protected static final VoxelShape BOTTOM_NW = ShapeUtil.join(
        Block.box(2, 2, 2, 16, 16, 16),
        Block.box(0, 0, 0, 12, 8, 8),
        Block.box(0, 8, 0, 8, 12, 8),
        Block.box(0, 0, 8, 8, 8, 12)
    );
    protected static final VoxelShape BOTTOM_W = ShapeUtil.join(
        Block.box(2, 2, 0, 16, 16, 16),
        Block.box(0, 0, 0, 5, 5, 16),
        Block.box(0, 0, 0, 8, 8, 4),
        Block.box(0, 0, 12, 8, 8, 16)
    );
    protected static final VoxelShape BOTTOM_SW = ShapeUtil.join(
        Block.box(2, 2, 0, 16, 16, 14),
        Block.box(0, 0, 4, 8, 8, 16),
        Block.box(0, 8, 8, 8, 12, 16),
        Block.box(0, 0, 8, 12, 8, 16)
    );
    protected static final VoxelShape BOTTOM_N = ShapeUtil.join(
        Block.box(0, 2, 2, 16, 16, 16),
        Block.box(0, 0, 0, 16, 5, 5),
        Block.box(0, 0, 0, 4, 8, 8),
        Block.box(12, 0, 0, 16, 8, 8)
    );
    protected static final VoxelShape BOTTOM_CENTER = Block.box(0, 2, 0, 16, 16, 16);
    protected static final VoxelShape BOTTOM_S = ShapeUtil.join(
        Block.box(0, 2, 0, 16, 16, 14),
        Block.box(0, 0, 11, 16, 5, 16),
        Block.box(0, 0, 8, 4, 8, 16),
        Block.box(12, 0, 8, 16, 8, 16)
    );
    protected static final VoxelShape BOTTOM_NE = ShapeUtil.join(
        Block.box(0, 2, 2, 14, 16, 16),
        Block.box(4, 0, 0, 16, 8, 8),
        Block.box(8, 8, 0, 16, 12, 8),
        Block.box(8, 0, 0, 16, 8, 12)
    );
    protected static final VoxelShape BOTTOM_E = ShapeUtil.join(
        Block.box(0, 2, 0, 14, 16, 16),
        Block.box(11, 0, 0, 16, 5, 16),
        Block.box(8, 0, 0, 16, 8, 4),
        Block.box(8, 0, 12, 16, 8, 16)
    );
    protected static final VoxelShape BOTTOM_SE = ShapeUtil.join(
        Block.box(0, 2, 0, 14, 16, 14),
        Block.box(4, 0, 8, 16, 8, 16),
        Block.box(8, 8, 8, 16, 12, 16),
        Block.box(8, 0, 4, 16, 8, 12)
    );
    protected static final VoxelShape MID_NW = ShapeUtil.join(
        Block.box(2, 0, 2, 16, 16, 16),
        Block.box(0, 0, 0, 5, 16, 5),
        Block.box(0, 0, 0, 8, 4, 8),
        Block.box(0, 12, 0, 8, 16, 8)
    );
    protected static final VoxelShape MID_W = Block.box(2, 0, 0, 16, 16, 16);
    protected static final VoxelShape MID_SW = ShapeUtil.join(
        Block.box(2, 0, 0, 16, 16, 14),
        Block.box(0, 0, 11, 5, 16, 16),
        Block.box(0, 0, 8, 8, 4, 16),
        Block.box(0, 12, 8, 8, 16, 16)
    );
    protected static final VoxelShape MID_N = Block.box(0, 0, 2, 16, 16, 16);
    protected static final VoxelShape MID_CENTER = Shapes.block();
    protected static final VoxelShape MID_S = Block.box(0, 0, 0, 16, 16, 14);
    protected static final VoxelShape MID_NE = ShapeUtil.join(
        Block.box(0, 0, 2, 14, 16, 16),
        Block.box(11, 0, 0, 16, 16, 5),
        Block.box(8, 0, 0, 16, 4, 8),
        Block.box(8, 12, 0, 16, 16, 8)
    );
    protected static final VoxelShape MID_E = Block.box(0, 0, 0, 14, 16, 16);
    protected static final VoxelShape MID_SE = ShapeUtil.join(
        Block.box(0, 0, 0, 14, 16, 14),
        Block.box(11, 0, 11, 16, 16, 16),
        Block.box(8, 0, 8, 16, 4, 16),
        Block.box(8, 12, 8, 16, 16, 16)
    );
    protected static final VoxelShape TOP_NW = ShapeUtil.join(
        Block.box(2, 0, 2, 16, 14, 16),
        Block.box(0, 4, 0, 8, 16, 8),
        Block.box(0, 8, 0, 12, 16, 8),
        Block.box(0, 8, 0, 8, 16, 12)
    );
    protected static final VoxelShape TOP_W = ShapeUtil.join(
        Block.box(2, 0, 0, 16, 14, 16),
        Block.box(0, 11, 0, 5, 16, 16),
        Block.box(0, 8, 0, 8, 16, 4),
        Block.box(0, 8, 12, 8, 16, 16)
    );
    protected static final VoxelShape TOP_SW = ShapeUtil.join(
        Block.box(2, 0, 0, 16, 14, 14),
        Block.box(0, 4, 8, 8, 16, 16),
        Block.box(0, 8, 8, 12, 16, 16),
        Block.box(0, 8, 4, 8, 16, 16)
    );
    protected static final VoxelShape TOP_N = ShapeUtil.join(
        Block.box(0, 0, 2, 16, 14, 16),
        Block.box(0, 11, 0, 16, 16, 5),
        Block.box(0, 8, 0, 4, 16, 8),
        Block.box(12, 8, 0, 16, 16, 8)
    );
    protected static final VoxelShape TOP_CENTER = Block.box(0, 0, 0, 16, 14, 16);
    protected static final VoxelShape TOP_S = ShapeUtil.join(
        Block.box(0, 0, 2, 16, 14, 16),
        Block.box(0, 11, 11, 16, 16, 16),
        Block.box(0, 8, 8, 4, 16, 16),
        Block.box(12, 8, 8, 16, 16, 16)
    );
    protected static final VoxelShape TOP_NE = ShapeUtil.join(
        Block.box(0, 0, 2, 14, 14, 16),
        Block.box(8, 4, 0, 16, 16, 8),
        Block.box(4, 8, 0, 16, 16, 8),
        Block.box(8, 8, 0, 16, 16, 12)
    );
    protected static final VoxelShape TOP_E = ShapeUtil.join(
        Block.box(0, 0, 0, 14, 14, 16),
        Block.box(11, 11, 0, 16, 16, 16),
        Block.box(8, 8, 0, 16, 16, 4),
        Block.box(8, 8, 12, 16, 16, 16)
    );
    protected static final VoxelShape TOP_SE = ShapeUtil.join(
        Block.box(0, 0, 0, 14, 14, 14),
        Block.box(8, 4, 8, 16, 16, 16),
        Block.box(4, 8, 8, 16, 16, 16),
        Block.box(8, 8, 4, 16, 16, 16)
    );

    public ShulkerContainerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(HALF, OpenedCube3x3PartHalf.BOTTOM_CENTER)
                .setValue(OPENED, false)
        );
    }

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

    @Override
    public void removePartsAndUpdate(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        if (!blockState.is(this)) return;
        BlockPos bottomCenterPos = this.getMainPartPos(pos, blockState).below();
        for (OpenedCube3x3PartHalf part : this.getParts()) {
            BlockPos bp = bottomCenterPos.offset(part.getOffset());
            level.setBlock(bp, level.getBlockState(bp).getFluidState().createLegacyBlock(), 3, 0);
        }
        GiantAnvilBlock.UPDATE_OFFSET.forEach((direction, offsetList) -> offsetList.forEach(offset -> {
            BlockPos updatedPos = bottomCenterPos.offset(offset);
            BlockPos fromPos = updatedPos.relative(direction);
            level.neighborShapeChanged(
                direction,
                level.getBlockState(fromPos),
                updatedPos,
                fromPos,
                3,
                512
            );
        }));
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.isClientSide || state.getValue(OPENED)) return InteractionResult.SUCCESS_NO_ITEM_USED;
        level.playSound(null, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS);
        this.updateState(level, pos, OPENED, true, 3);
        level.scheduleTick(pos, this, 16);
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.playSound(null, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS);
        this.updateState(level, pos, OPENED, false, 3);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        level.getBlockEntity(pos, ModBlockEntities.SHULKER_CRATE.get())
            .ifPresent(crate -> {
                if (!level.isClientSide && player.isCreative() && state.getValue(HALF).isMain()) {
                    ItemStack stack = this.asItem().getDefaultInstance();
                    stack.applyComponents(crate.collectComponents());
                    ItemEntity itemEntity = new ItemEntity(
                        level,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        stack
                    );
                    itemEntity.setDefaultPickUpDelay();
                    level.addFreshEntity(itemEntity);
                }
            });

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltips, flag);
        if (!flag.isAdvanced()) return;
        Optional<UUID> uuid = Optional.ofNullable(stack.get(ModComponents.CRATE_STORAGE)).flatMap(CrateStorageReference::id);
        if (uuid.isEmpty()) return;
        tooltips.add(Component.translatable("tooltip.anvilcraft.shulker_crate.uuid", uuid.get().toString()).withStyle(ChatFormatting.ITALIC));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        level.getBlockEntity(pos, ModBlockEntities.SHULKER_CRATE.get())
            .ifPresent(be -> be.saveToItem(stack, level.registryAccess()));
        return stack;
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
    protected float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter getter, @NotNull BlockPos pos) {
        return 1.0F;
    }

    @Override
    public Property<OpenedCube3x3PartHalf> getPart() {
        return ShulkerContainerBlock.HALF;
    }

    @Override
    public OpenedCube3x3PartHalf[] getParts() {
        return OpenedCube3x3PartHalf.values();
    }

    @Override
    public BooleanProperty getAdditionalProperty() {
        return ShulkerContainerBlock.OPENED;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.SHULKER_CRATE.create(pos, state);
    }
}

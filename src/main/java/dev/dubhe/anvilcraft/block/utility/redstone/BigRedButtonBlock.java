package dev.dubhe.anvilcraft.block.utility.redstone;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.ButtonCap;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.MountedButtonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BigRedButtonBlock extends BaseEntityBlock implements IMoveableEntityBlock, IHammerRemovable {
    public static final MapCodec<BigRedButtonBlock> CODEC = simpleCodec(BigRedButtonBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");

    public BigRedButtonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(PRESSED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PRESSED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData, player);
        if (level.getBlockEntity(pos) instanceof BigRedButtonBlockEntity button) {
            stack.copyFrom(DataComponents.BASE_COLOR, button.components());
            stack.copyFrom(ModComponents.BUTTON_CAP, button.components());
            stack.copyFrom(ModComponents.MOUNTED_BLOCK, button.components());
        }
        return stack;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BigRedButtonBlockEntity button)) {
            return drops;
        }
        MountedButtonBlock mounted = button.getMountedBlock();
        if (mounted == null && !button.isBareBase()) {
            return drops;
        }
        List<ItemStack> result = new ArrayList<>(drops.size() + 1);
        boolean returnedMaterial = false;
        for (ItemStack drop : drops) {
            if (drop.is(this.asItem())) {
                drop.remove(ModComponents.BUTTON_CAP);
                drop.remove(ModComponents.MOUNTED_BLOCK);
                if (mounted != null && !returnedMaterial) {
                    result.add(mounted.material().create());
                    returnedMaterial = true;
                }
            }
            result.add(drop);
        }
        return result;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        ButtonMount mount = mount(level, pos);
        return mount == null ? ButtonMount.base(facing) : mount.withBase(facing, state.getValue(PRESSED));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ButtonMount.base(state.getValue(FACING));
    }

    private static @Nullable ButtonMount mount(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BigRedButtonBlockEntity button) {
            var mounted = button.getMountedBlock();
            if (mounted != null) {
                // 嵌套按钮的临时几何视图只查询方块状态，避免递归展开安装物的物品组件。
                return level instanceof LevelReader reader ? ButtonMount.forMounted(mounted, reader.registryAccess())
                    : ButtonMount.forBlock(mounted.state());
            }
            if (button.isBareBase()) {
                return null;
            }
            ButtonCap cap = button.getCap();
            if (cap != null && cap.isHammerCap()) {
                return ButtonMount.forHammer();
            }
        }
        return ButtonMount.COLOR;
    }

    public static boolean isBaseHit(BlockState state, BlockGetter level, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
        Vec3 inside = local.relative(hit.getDirection().getOpposite(), 1E-5);
        Direction facing = state.getValue(FACING);
        if (!ButtonMount.base(facing).bounds().inflate(1E-7).contains(inside)) {
            return false;
        }
        ButtonMount mount = mount(level, pos);
        if (mount == null) {
            return true;
        }
        for (AABB box : mount.outline(facing, state.getValue(PRESSED)).toAabbs()) {
            if (box.inflate(1E-7).contains(inside)) {
                return false;
            }
        }
        return true;
    }

    public static @Nullable BlockHitResult clipCap(BlockState state, BlockGetter level, BlockPos pos, Vec3 from, Vec3 to) {
        ButtonMount mount = mount(level, pos);
        return mount == null ? null : mount.outline(state.getValue(FACING), state.getValue(PRESSED)).clip(from, to, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BigRedButtonBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.BIG_RED_BUTTON.get(),
            level.isClientSide() ? BigRedButtonBlockEntity::clientTick : BigRedButtonBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() || player.getMainHandItem().is(ModItems.BUILDING_ROD)
            || !(level.getBlockEntity(pos) instanceof BigRedButtonBlockEntity button) || button.isBareBase()) {
            return InteractionResult.PASS;
        }
        MountedButtonBlock mounted = button.getMountedBlock();
        boolean adjusting = mounted != null && mounted.canAdjustWithHammer(player) && isBaseHit(state, level, hit);
        if (!level.isClientSide() && !adjusting) {
            button.press(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    ) {
        if (player.isShiftKeyDown() || player.getMainHandItem().is(ModItems.BUILDING_ROD)) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof BigRedButtonBlockEntity button && button.isBareBase()
            && stack.getItem() instanceof BlockItem item) {
            if (!player.mayBuild() || !level.mayInteract(player, pos) || !item.getBlock().isEnabled(level.enabledFeatures())) {
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide()) {
                BlockPlaceContext context = new BlockPlaceContext(level, player, hand, stack, hit);
                Block block = item.getBlock();
                BlockState placement = block instanceof SimpleMultiPartBlock<?> multipart
                    ? multipart.getPlacementState(context) : block.getStateForPlacement(context);
                BlockState mounted = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                    .apply(placement == null ? block.defaultBlockState() : placement);
                if (button.mountBlock(new MountedButtonBlock(mounted, ItemStackTemplate.fromNonEmptyStack(stack)))) {
                    stack.consume(1, player);
                    var sound = mounted.getSoundType();
                    level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
                }
            }
            return InteractionResult.SUCCESS;
        }
        return this.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(PRESSED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(PRESSED) && state.getValue(FACING) == direction ? 15 : 0;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BigRedButtonBlockEntity button) {
            button.checkPressed();
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moving) {
        super.affectNeighborsAfterRemoval(state, level, pos, moving);
        if (!state.is(level.getBlockState(pos).getBlock()) && state.getValue(PRESSED)) {
            this.updateNeighbours(state, level, pos);
        }
    }

    public void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), this);
    }
}

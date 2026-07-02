package dev.dubhe.anvilcraft.block.workstation;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.block.IIgnitableCauldron;
import dev.dubhe.anvilcraft.api.fluid.FluidStackResourceHandler;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.block.special.PlasmaJetsBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public class FishTankBlock extends Block implements IMoveableEntityBlock, HammerRotateBehavior, IHammerRemovable, IIgnitableCauldron {
    public static final BooleanProperty TROPICAL = BooleanProperty.create("tropical");
    public static final BooleanProperty OUTLET = BooleanProperty.create("outlet");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE = ShapeUtil.cut(
        Shapes.block(),
        Block.box(2.0, 14.0, 2.0, 14.0, 16.0, 14.0),
        Block.box(1.0, 1.0, 1.0, 15.0, 14.0, 15.0)
    );

    public FishTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FishTankBlock.TROPICAL, false)
                .setValue(FishTankBlock.OUTLET, false)
                .setValue(FishTankBlock.FACING, Direction.NORTH)
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return Block.simpleCodec(FishTankBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FishTankBlock.TROPICAL, FishTankBlock.OUTLET, FishTankBlock.FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FishTankBlock.SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FishTankBlock.FACING, facing);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos, ModBlockEntities.FISH_TANK.get())
            .map(FishTankBlockEntity::getSignal)
            .orElse(0);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return false;
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
        AuxiliaryLightManager manager = level.getAuxLightManager(pos);
        if (manager == null) return 0;
        return manager.getLightAt(pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (this.isIgnited(new BlockCache(level), pos) && level.getBlockState(pos.below()).is(ModBlocks.HEATER)) {
            level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block block,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        if (this.isIgnited(new BlockCache(level), pos) && level.getBlockState(pos.below()).is(ModBlocks.HEATER)) {
            level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected void tick(BlockState cauldronState, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(ModBlocks.HEATER) && !below.getValue(HeaterBlock.OVERLOAD) && !PlasmaJetsBlock.trySpawn(pos.above(), level)) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide()) return;
        if (entity.isOnFire()) {
            this.tryIgnite(level, pos);
        }
        if (!(entity instanceof ItemEntity itemEntity)) return;
        if (itemEntity.getItem().is(ModItemTags.FIRE_STARTER)) {
            if (this.tryIgnite(level, pos)) {
                itemEntity.getItem().setCount(itemEntity.getItem().getCount() - 1);
            }
        } else if (itemEntity.getItem().is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
            this.tryIgnite(level, pos);
        }
        ResourceHandler<ItemResource> items = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        FishTankBlockEntity.insertItemToTank(items, itemEntity);
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
        if (level.isClientSide()) return;
        if (entity.isOnFire()) {
            this.tryIgnite(level, pos);
        }
        if (!(entity instanceof ItemEntity itemEntity)) {
            level.getBlockEntity(pos, ModBlockEntities.FISH_TANK.get())
                .ifPresent(be -> be.entityInsideFluidContent(level, pos, entity, effectApplier));
            return;
        }
        if (itemEntity.getItem().is(ModItemTags.FIRE_STARTER)) {
            if (this.tryIgnite(level, pos)) {
                itemEntity.getItem().setCount(itemEntity.getItem().getCount() - 1);
            }
        } else if (itemEntity.getItem().is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
            this.tryIgnite(level, pos);
        }
        ResourceHandler<ItemResource> items = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        FishTankBlockEntity.insertItemToTank(items, itemEntity);
        if (this.isIgnited(new BlockCache(level), pos) && !itemEntity.isRemoved()) {
            itemEntity.discard();
        }
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (stack.is(ModItemTags.ANVIL_HAMMER)) {
            Direction clickedFace = hitResult.getDirection();
            // 侧面直接对应输出口方向；从顶部开口对着内部某个边右键时按玩家朝向选取输出口的面
            if (clickedFace.getAxis() != Direction.Axis.Y) {
                return this.changeOutlet(level, pos, state, player, clickedFace);
            } else if (clickedFace == Direction.UP) {
                return this.changeOutlet(level, pos, state, player, player.getDirection());
            }
        }
        CauldronInteraction interaction = ModInteractionMap.FISH_TANK.get(stack);
        if (interaction != CauldronInteraction.DEFAULT) return interaction.interact(state, level, pos, player, hand, stack);
        return this.useItemOnTank(stack, state, level, pos, player, hand, hitResult);
    }

    public InteractionResult changeOutlet(Level level, BlockPos pos, BlockState state, Player player, Direction outletDir) {
        if (!level.isClientSide()) {
            // 水平的四个方向根据被右键的方向转换
            boolean hasOutlet = outletDir != state.getValue(FishTankBlock.FACING) || !state.getValue(FishTankBlock.OUTLET);
            BlockState newState = state.setValue(FishTankBlock.OUTLET, hasOutlet).setValue(FishTankBlock.FACING, outletDir);

            level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
            if (hasOutlet) {
                level.getBlockEntity(pos, ModBlockEntities.FISH_TANK.get())
                    .ifPresent(FishTankBlockEntity::tryAutoOutputResults);
            }
        }
        level.playSound(player, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    public boolean tryIgnite(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FishTankBlockEntity tank)) return false;
        if (!FishTankBlockEntity.canIgnite(tank.getFluidHandler().getStack())) return false;
        if (tank.isIgnited()) {
            if (this.isIgnited(new BlockCache(level), pos) && level.getBlockState(pos.below()).is(ModBlocks.HEATER)) {
                level.scheduleTick(pos, this, 2);
            }
            return false;
        }
        tank.setIgnited(true);
        if (this.isIgnited(new BlockCache(level), pos) && level.getBlockState(pos.below()).is(ModBlocks.HEATER)) {
            level.scheduleTick(pos, this, 2);
        }
        return true;
    }

    private InteractionResult useItemOnTank(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        InteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        if (result != InteractionResult.TRY_WITH_EMPTY_HAND) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
            if (tank.tryInteractWithTank(player, hand, hitResult)) {
                return dev.dubhe.anvilcraft.util.Util.sidedSuccess(level);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.FISH_TANK.create(pos, state);
    }

    @Override
    public boolean isEmpty(BlockCache cache, BlockPos pos) {
        return Util.castSafely(cache.getBlockEntity(pos), FishTankBlockEntity.class)
            .map(be -> be.getFluidHandler().getAmountAsLong(0) == 0)
            .orElseThrow();
    }

    @Override
    public boolean isIgnited(BlockCache cache, BlockPos pos) {
        return Util.castSafely(cache.getBlockEntity(pos), FishTankBlockEntity.class)
            .map(FishTankBlockEntity::isIgnited)
            .orElseThrow();
    }

    @Override
    public void setIgnited(BlockCache cache, BlockPos pos, boolean ignited) {
        Util.castSafely(cache.getBlockEntity(pos), FishTankBlockEntity.class)
            .ifPresent(be -> be.setIgnited(ignited));
    }

    @Override
    public Fluid getFluid(BlockCache cache, BlockPos pos) {
        return Util.castSafely(cache.getBlockEntity(pos), FishTankBlockEntity.class)
            .map(be -> be.getFluidHandler().getStack().getFluid())
            .orElseThrow();
    }

    @Override
    public boolean consumeOnce(BlockCache cache, BlockPos pos) {
        BlockEntity blockEntity = cache.getBlockEntity(pos);
        if (!(blockEntity instanceof FishTankBlockEntity be)) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            FluidStackResourceHandler handler = be.getFluidHandler();
            FluidResource resource = handler.getResource(0);
            if (resource.isEmpty()) {
                return false;
            }
            int extracted = handler.extract(resource, 250, transaction);
            if (extracted < 250) {
                return false;
            }
            transaction.commit();
            return true;
        }
    }
}

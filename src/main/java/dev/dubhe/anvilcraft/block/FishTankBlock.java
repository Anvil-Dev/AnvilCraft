package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.block.IIgnitableCauldron;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class FishTankBlock extends Block implements IMoveableEntityBlock, HammerRotateBehavior, IHammerRemovable, IIgnitableCauldron {
    public static final BooleanProperty TROPICAL = BooleanProperty.create("tropical");
    public static final BooleanProperty OUTLET = BooleanProperty.create("outlet");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

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
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos, ModBlockEntities.FISH_TANK.get())
            .map(FishTankBlockEntity::getSignal)
            .orElse(0);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
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
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos, ModBlockEntities.FISH_TANK.get())
            .map(be -> be.getFluidHandler().getFluid().getFluidType())
            .map(FluidType::getLightLevel)
            .orElse(0);
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
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        FishTankBlockEntity.insertItemToTank(items, itemEntity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) return;
        if (entity.isOnFire()) {
            this.tryIgnite(level, pos);
        }
        if (!(entity instanceof ItemEntity itemEntity)) {
            level.getBlockEntity(pos, ModBlockEntities.FISH_TANK.get()).ifPresent(be -> be.entityInsideFluidContent(level, pos, entity));
            return;
        }
        if (itemEntity.getItem().is(ModItemTags.FIRE_STARTER)) {
            if (this.tryIgnite(level, pos)) {
                itemEntity.getItem().setCount(itemEntity.getItem().getCount() - 1);
            }
        } else if (itemEntity.getItem().is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
            this.tryIgnite(level, pos);
        }
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        FishTankBlockEntity.insertItemToTank(items, itemEntity);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && !movedByPiston
            && !SmartBlockPlacerBlockEntity.isBlockBeingMovedByPlacer()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FishTankBlockEntity tank) {
                IItemHandler handler = tank.getItemHandler();
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stack = handler.extractItem(slot, Integer.MAX_VALUE, false);
                    if (!stack.isEmpty()) Block.popResource(level, pos, stack);
                }
                tank.dropFish();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
        if (stack.is(ModItemTags.ANVIL_HAMMER) && hitResult.getDirection().getAxis() != Direction.Axis.Y) {
            return this.changeOutlet(level, pos, state, player, hitResult);
        }
        CauldronInteraction interaction = ModInteractionMap.FISH_TANK.map().get(stack.getItem());
        if (interaction != null) return interaction.interact(state, level, pos, player, hand, stack);
        return this.useItemOnTank(stack, state, level, pos, player, hand, hitResult);
    }

    public ItemInteractionResult changeOutlet(Level level, BlockPos pos, BlockState state, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            // 水平的四个方向根据被右键的方向转换
            Direction outletDir = hitResult.getDirection();
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
        return ItemInteractionResult.SUCCESS;
    }

    public boolean tryIgnite(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FishTankBlockEntity tank)) return false;
        if (!FishTankBlockEntity.canIgnite(tank.getFluidHandler().getFluid())) return false;
        if (tank.isIgnited()) return false;
        tank.setIgnited(true);
        return true;
    }

    private ItemInteractionResult useItemOnTank(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        InteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hitResult).result();
        if (result != InteractionResult.PASS) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
            if (tank.tryInteractWithTank(player, hand, hitResult)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.FISH_TANK.create(pos, state);
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
            .map(be -> be.getFluidHandler().getFluid().getFluid())
            .orElseThrow();
    }
}

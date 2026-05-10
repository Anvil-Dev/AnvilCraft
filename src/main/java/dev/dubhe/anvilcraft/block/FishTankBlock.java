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
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
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
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity.getType().equals(EntityType.ARROW) && entity.isOnFire()) {
            this.tryIgnite(level, pos);
            return;
        }
        if (!(entity instanceof ItemEntity itemEntity)) return;
        if (itemEntity.getItem().is(ModItemTags.FIRE_STARTER)) {
            this.tryIgnite(level, pos);
            itemEntity.getItem().setCount(itemEntity.getItem().getCount() - 1);
        } else if (itemEntity.getItem().is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
            this.tryIgnite(level, pos);
        }
        ResourceHandler<ItemResource> items = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        FishTankBlockEntity.insertToTank(items, itemEntity.getItem());
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
        if (entity.getType().equals(EntityType.ARROW) && entity.isOnFire()) {
            this.tryIgnite(level, pos);
            return;
        }
        if (!(entity instanceof ItemEntity itemEntity)) return;
        if (itemEntity.getItem().is(ModItemTags.FIRE_STARTER)) {
            this.tryIgnite(level, pos);
            itemEntity.getItem().setCount(itemEntity.getItem().getCount() - 1);
        } else if (itemEntity.getItem().is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
            this.tryIgnite(level, pos);
        }
        ResourceHandler<ItemResource> items = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        FishTankBlockEntity.insertToTank(items, itemEntity.getItem());
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockState newState = level.getBlockState(pos);
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {

            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
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
        if (stack.is(ModItemTags.ANVIL_HAMMER)) return this.changeOutlet(level, pos, state, player, hitResult);
        CauldronInteraction interaction = ModInteractionMap.FISH_TANK.get(stack);
        if (!interaction.equals(CauldronInteraction.DEFAULT)) return interaction.interact(state, level, pos, player, hand, stack);
        return this.useItemOnTank(stack, state, level, pos, player, hand, hitResult);
    }

    public InteractionResult changeOutlet(Level level, BlockPos pos, BlockState state, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            // 水平的四个方向根据被右键的方向转换
            Direction outletDir = Direction.from2DDataValue((hitResult.getDirection().get2DDataValue()));
            boolean hasOutlet = !state.getValue(FishTankBlock.OUTLET);
            BlockState newState = state.setValue(FishTankBlock.OUTLET, hasOutlet).setValue(FishTankBlock.FACING, outletDir);

            level.setBlock(pos, newState, 3);
        }
        level.playSound(player, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    public boolean tryIgnite(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FishTankBlockEntity tank)) return false;
        if (!FishTankBlockEntity.shouldIgnite(FluidUtil.getStack(tank.getFluidHandler(), 0))) return false;
        if (tank.isIgnited()) return false;
        tank.setIgnited(true);
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
        if (result == InteractionResult.PASS) {
            if (level.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
                if (tank.onPlayerUse(player, hand, hitResult)) {
                    return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.FISH_TANK.create(pos, state);
    }

    @Override
    public void storeData(Level level, BlockPos pos, ValueOutput output) {
        level.getBlockEntity(pos, ModBlockEntities.FISH_TANK.get())
            .ifPresent(be -> be.saveCustomOnly(output));
    }

    @Override
    public void loadData(Level level, BlockPos pos, ValueInput input) {
        level.getBlockEntity(pos, ModBlockEntities.FISH_TANK.get())
            .ifPresent(be -> be.loadAdditional(input));
    }

    @Override
    public boolean isIgnited(BlockCache cache, BlockPos pos) {
        return Util.<FishTankBlockEntity>cast(cache.getBlockEntity(pos)).isIgnited();
    }

    @Override
    public void setIgnited(BlockCache cache, BlockPos pos, boolean ignited) {
        Util.<FishTankBlockEntity>cast(cache.getBlockEntity(pos)).setIgnited(ignited);
    }

    @Override
    public Fluid getFluid(BlockCache cache, BlockPos pos) {
        return Util.<FishTankBlockEntity>cast(cache.getBlockEntity(pos)).getFluidHandler().getResource(0).getFluid();
    }

    @Override
    public boolean consumeOnce(BlockCache cache, BlockPos pos) {
        FluidStacksResourceHandler handler = Util.<FishTankBlockEntity>cast(cache.getBlockEntity(pos)).getFluidHandler();
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(0, handler.getResource(0), 250, transaction);
            if (extracted < 250) return false;
            transaction.commit();
            return true;
        }
    }
}

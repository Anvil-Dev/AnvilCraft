package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.mixin.accessor.RedStoneWireBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import javax.annotation.Nullable;

public class RuinsBlock extends BaseEntityBlock implements IMoveableEntityBlock {
    public static final BooleanProperty MOVABLE = BooleanProperty.create("movable");

    public RuinsBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MOVABLE, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(RuinsBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOVABLE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RuinsBlockEntity(ModBlockEntities.RUINS_BLOCK.get(), pos, state);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(MOVABLE) ? PushReaction.NORMAL : PushReaction.BLOCK;
    }

    @Override
    public void notifyMoved(Level level, BlockPos pos, BlockState state, BlockEntity entity) {
        if (entity instanceof RuinsBlockEntity ruins) ruins.invalidateDisplayEntity();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof RuinsBlockEntity ruins
            && RuinsStructure.isMainPart(ruins) ? ruins.getDrops(params) : List.of();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins) {
            RuinsBlockEntity main = RuinsStructure.main(level, ruins);
            if (!main.isFragile()) return InteractionResult.CONSUME;
            LootParams.Builder params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.BLOCK_ENTITY, main)
                .withLuck(player.getLuck());
            List<ItemStack> drops = main.getDrops(params);
            RuinsStructure.destroyOthers(server, ruins, false, player);
            if (server.destroyBlock(pos, false, player)) drops.forEach(stack -> popResource(server, pos, stack));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    ) {
        this.useWithoutItem(state, level, pos, player, hit);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins)) return Shapes.block();
        BlockGetter view = new RuinsBlockView(level);
        BlockState display = connectedDisplayState(view, pos, ruins.getDisplayState());
        if (RuinsStructure.isMultipart(display)) return ((AbstractMultiPartBlock<?>) display.getBlock()).getPartShape(display);
        return display.isAir() ? Shapes.block() : display.getShape(view, pos, context);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins
            ? ruins.getDisplayState().getCollisionShape(new RuinsBlockView(level), pos, context) : Shapes.empty();
    }

    public static BlockState connectedDisplayState(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof RedstoneWireBlock wire) return wire.connectionState(level, pos, state);
        if (state.getBlock() instanceof RedStoneWireBlock wire) {
            return ((RedStoneWireBlockAccessor) wire).anvilcraft$getConnectionState(level, state, pos);
        }
        return state;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins
            ? ruins.getDisplayState().getLightEmission(new RuinsBlockView(level), pos) : 0;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins
            ? ruins.getDisplayState().getLightBlock(new RuinsBlockView(level), pos) : 0;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins
            ? ruins.getDisplayState().getShadeBrightness(new RuinsBlockView(level), pos) : 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return !(level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins)
            || ruins.getDisplayState().propagatesSkylightDown(new RuinsBlockView(level), pos);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins
            && ruins.getDisplayState().getBlock().canConnectRedstone(ruins.getDisplayState(), level, pos, direction);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isCreative() && level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins) {
            RuinsStructure.destroyOthers(level, ruins, false, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !movedByPiston && level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins) {
            RuinsStructure.destroyOthers(level, ruins, true, null);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

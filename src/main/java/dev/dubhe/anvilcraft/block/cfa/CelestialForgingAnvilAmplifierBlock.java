package dev.dubhe.anvilcraft.block.cfa;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube232PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class CelestialForgingAnvilAmplifierBlock
    extends FlexibleMultiPartBlock<DirectionCube232PartHalf, DirectionProperty, Direction>
    implements IHammerChangeable, IHammerRemovable {
    public static final EnumProperty<DirectionCube232PartHalf> HALF = EnumProperty.create("half", DirectionCube232PartHalf.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CelestialForgingAnvilAmplifierBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(HALF, DirectionCube232PartHalf.BOTTOM_PART)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState mapRealModelHolderBlock(Level level, BlockPos blockPos, BlockState original) {
        Direction direction = original.getValue(FACING);
        return switch (direction) {
            case NORTH -> original.setValue(HALF, DirectionCube232PartHalf.MID_PART);
            case EAST -> original.setValue(HALF, DirectionCube232PartHalf.MID_W);
            case SOUTH -> original.setValue(HALF, DirectionCube232PartHalf.MID_WS);
            case WEST -> original.setValue(HALF, DirectionCube232PartHalf.MID_S);
            default -> original;
        };
    }

    @Override
    public BlockState placedState(DirectionCube232PartHalf part, BlockState state) {
        return state.setValue(this.getPart(), part);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().trySetValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public Property<DirectionCube232PartHalf> getPart() {
        return HALF;
    }

    @Override
    public DirectionCube232PartHalf[] getParts() {
        return DirectionCube232PartHalf.values();
    }

    @Override
    public DirectionProperty getAdditionalProperty() {
        return FACING;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HALF, state.getValue(HALF).rotate(rotation))
            .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(HALF, state.getValue(HALF).mirror(mirror))
            .setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        this.change(blockPos, level, (state) -> state.cycle(FACING));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}

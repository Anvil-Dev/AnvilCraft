package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3Part;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * 巨型石碑芯（giant_monolith_core）：以 3x3 单层多方块落地，
 * 仅中心部件显示整体模型，其余八个部件为无碰撞的透明占位。
 * 东西/南北两种朝向沿用水平轴属性，模型默认为南北朝向。
 */
public class GiantMonolithCoreBlock extends SimpleMultiPartBlock<Cube3x3Part> {
    public static final EnumProperty<Cube3x3Part> PART = EnumProperty.create("part", Cube3x3Part.class);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public GiantMonolithCoreBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(PART, Cube3x3Part.CENTER)
            .setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    public Property<Cube3x3Part> getPart() {
        return PART;
    }

    @Override
    public Cube3x3Part[] getParts() {
        return Cube3x3Part.values();
    }

    @Override
    public BlockState getPlacementState(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, AXIS);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                default -> state;
            };
            default -> state;
        };
    }
}

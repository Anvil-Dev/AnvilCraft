package dev.dubhe.anvilcraft.recipe.multiblock;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record MultiblockInput(List<List<List<BlockState>>> blocks) implements RecipeInput {
    @Override
    public ItemStack getItem(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    public BlockState getBlockState(int x, int y, int z) {
        return blocks.get(y).get(z).get(x);
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        blocks.get(y).get(z).set(x, state);
    }

    public void rotate() {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    BlockState state = getBlockState(x, y, z);
                    if (state.hasProperty(BlockStateProperties.FACING)) {
                        setBlockState(
                                x,
                                y,
                                z,
                                state.setValue(
                                        BlockStateProperties.FACING,
                                        rotateHorizontal(state.getValue(BlockStateProperties.FACING))));
                    }
                    if (state.hasProperty(BlockStateProperties.FACING_HOPPER)) {
                        setBlockState(
                                x,
                                y,
                                z,
                                state.setValue(
                                        BlockStateProperties.FACING_HOPPER,
                                        rotateHorizontal(state.getValue(BlockStateProperties.FACING_HOPPER))));
                    }
                    if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        setBlockState(
                                x,
                                y,
                                z,
                                state.setValue(
                                        BlockStateProperties.HORIZONTAL_FACING,
                                        rotateHorizontal(state.getValue(BlockStateProperties.HORIZONTAL_FACING))));
                    }
                    if (state.hasProperty(BlockStateProperties.AXIS)) {
                        setBlockState(
                                x,
                                y,
                                z,
                                state.setValue(
                                        BlockStateProperties.AXIS,
                                        rotateAxis(state.getValue(BlockStateProperties.AXIS))));
                    }
                }
            }
        }
    }

    private static Direction rotateHorizontal(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> direction;
        };
    }

    private static Direction.Axis rotateAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.Axis.Z;
            case Z -> Direction.Axis.X;
            default -> axis;
        };
    }
}

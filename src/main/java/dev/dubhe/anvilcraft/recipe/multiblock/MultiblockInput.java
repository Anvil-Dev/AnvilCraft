package dev.dubhe.anvilcraft.recipe.multiblock;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record MultiblockInput(List<List<List<BlockState>>> blocks, int size) implements RecipeInput {
    @Override
    public ItemStack getItem(int i) {
        return ItemStack.EMPTY;
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

package dev.dubhe.anvilcraft.data.generator;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumBlockstateProvider;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
import dev.dubhe.anvilcraft.util.DangerUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.DOWN_EAST;
import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.DOWN_NORTH;
import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.DOWN_SOUTH;
import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.DOWN_WEST;
import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.NORTH_EAST;
import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.NORTH_WEST;
import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.SOUTH_EAST;
import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.SOUTH_WEST;
import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.UP_EAST;
import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.UP_NORTH;
import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.UP_SOUTH;
import static dev.dubhe.anvilcraft.block.fluid.PipeBlock.CornerEnded.UP_WEST;

public class PipeBlockStateGenerator {
    public static <T extends Block> void pipeStraightBlock(DataGenContext<Block, T> ctx, RegistrumBlockstateProvider provider) {
        ModelFile straight = DangerUtil.genModModelFile("block/pipe_straight").get();
        ModelFile noEnd = DangerUtil.genModModelFile("block/pipe_no_end").get();
        ModelFile end = DangerUtil.genModModelFile("block/pipe_end").get();
        provider.getMultipartBuilder(ctx.get())
            .part()
            .modelFile(straight)
            .rotationY(90)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.X)
            .end()
            // ========
            .part()
            .modelFile(straight)
            .rotationX(90)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.Y)
            .end()
            // ========
            .part()
            .modelFile(straight)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.Z)
            .end()
            // ========================================= AXIS_X
            .part()
            .modelFile(noEnd)
            .rotationY(270)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.X)
            .condition(PipeStraightBlock.HAS_END_START, false)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationY(90)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.X)
            .condition(PipeStraightBlock.HAS_END_END, false)
            .end()
            // ========
            .part()
            .modelFile(end)
            .rotationY(270)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.X)
            .condition(PipeStraightBlock.HAS_END_START, true)
            .end()
            // ========
            .part()
            .modelFile(end)
            .rotationY(90)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.X)
            .condition(PipeStraightBlock.HAS_END_END, true)
            .end()
            // ========================================= AXIS_Y
            .part()
            .modelFile(noEnd)
            .rotationX(90)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.Y)
            .condition(PipeStraightBlock.HAS_END_START, false)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationX(270)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.Y)
            .condition(PipeStraightBlock.HAS_END_END, false)
            .end()
            // ========
            .part()
            .modelFile(end)
            .rotationX(90)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.Y)
            .condition(PipeStraightBlock.HAS_END_START, true)
            .end()
            // ========
            .part()
            .modelFile(end)
            .rotationX(270)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.Y)
            .condition(PipeStraightBlock.HAS_END_END, true)
            .end()
            // ========================================= AXIS_Z
            .part()
            .modelFile(noEnd)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.Z)
            .condition(PipeStraightBlock.HAS_END_START, false)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationY(180)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.Z)
            .condition(PipeStraightBlock.HAS_END_END, false)
            .end()
            // ========
            .part()
            .modelFile(end)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.Z)
            .condition(PipeStraightBlock.HAS_END_START, true)
            .end()
            // ========
            .part()
            .modelFile(end)
            .rotationY(180)
            .addModel()
            .condition(PipeStraightBlock.AXIS, Direction.Axis.Z)
            .condition(PipeStraightBlock.HAS_END_END, true)
            .end();
    }

    public static <T extends Block> void pipeCornerBlock(DataGenContext<Block, T> ctx, RegistrumBlockstateProvider provider) {
        ModelFile sideCorner = DangerUtil.genModModelFile("block/pipe_side_corner").get();
        ModelFile noEnd = DangerUtil.genModModelFile("block/pipe_no_end").get();
        ModelFile end = DangerUtil.genModModelFile("block/pipe_end").get();
        provider.getMultipartBuilder(ctx.get())
            .part()
            .modelFile(sideCorner)
            .rotationX(180)
            .rotationY(270)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, DOWN_NORTH)
            .end()
            // ========
            .part()
            .modelFile(sideCorner)
            .rotationX(180)
            .rotationY(90)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, DOWN_SOUTH)
            .end()
            // ========
            .part()
            .modelFile(sideCorner)
            .rotationX(180)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, DOWN_EAST)
            .end()
            // ========
            .part()
            .modelFile(sideCorner)
            .rotationX(180)
            .rotationY(180)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, DOWN_WEST)
            .end()
            // =========================================
            .part()
            .modelFile(sideCorner)
            .rotationY(270)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_NORTH)
            .end()
            // ========
            .part()
            .modelFile(sideCorner)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_EAST)
            .end()
            // ========
            .part()
            .modelFile(sideCorner)
            .rotationY(90)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_SOUTH)
            .end()
            // ========
            .part()
            .modelFile(sideCorner)
            .rotationY(180)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_WEST)
            .end()
            // =========================================
            .part()
            .modelFile(sideCorner)
            .rotationX(90)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, NORTH_EAST)
            .end()
            // ========
            .part()
            .modelFile(sideCorner)
            .rotationX(90)
            .rotationY(90)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, SOUTH_EAST)
            .end()
            // ========
            .part()
            .modelFile(sideCorner)
            .rotationX(90)
            .rotationY(180)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, SOUTH_WEST)
            .end()
            // ========
            .part()
            .modelFile(sideCorner)
            .rotationX(90)
            .rotationY(270)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, NORTH_WEST)
            .end()
            // ========================================= UP_END
            .part()
            .modelFile(end)
            .rotationX(270)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_NORTH, UP_WEST, UP_EAST, UP_SOUTH)
            .condition(PipeStraightBlock.HAS_END_START, true)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationX(270)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_NORTH, UP_WEST, UP_EAST, UP_SOUTH)
            .condition(PipeStraightBlock.HAS_END_START, false)
            .end()
            // ========================================= DOWN_END
            .part()
            .modelFile(end)
            .rotationX(90)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, DOWN_NORTH, DOWN_WEST, DOWN_EAST, DOWN_SOUTH)
            .condition(PipeStraightBlock.HAS_END_START, true)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationX(90)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, DOWN_NORTH, DOWN_WEST, DOWN_EAST, DOWN_SOUTH)
            .condition(PipeStraightBlock.HAS_END_START, false)
            .end()
            // ========================================= NORTH_START_END
            .part()
            .modelFile(end)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, NORTH_EAST, NORTH_WEST)
            .condition(PipeStraightBlock.HAS_END_START, true)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, NORTH_EAST, NORTH_WEST)
            .condition(PipeStraightBlock.HAS_END_START, false)
            .end()
            // ========================================= NORTH_END_END
            .part()
            .modelFile(end)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_NORTH, DOWN_NORTH)
            .condition(PipeStraightBlock.HAS_END_END, true)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_NORTH, DOWN_NORTH)
            .condition(PipeStraightBlock.HAS_END_END, false)
            .end()
            // ========================================= SOUTH_START_END
            .part()
            .modelFile(end)
            .rotationY(180)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, SOUTH_EAST, SOUTH_WEST)
            .condition(PipeStraightBlock.HAS_END_START, true)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationY(180)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, SOUTH_EAST, SOUTH_WEST)
            .condition(PipeStraightBlock.HAS_END_START, false)
            .end()
            // ========================================= SOUTH_END_END
            .part()
            .modelFile(end)
            .rotationY(180)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_SOUTH, DOWN_SOUTH)
            .condition(PipeStraightBlock.HAS_END_END, true)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationY(180)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_SOUTH, DOWN_SOUTH)
            .condition(PipeStraightBlock.HAS_END_END, false)
            .end()
            // ========================================= EAST_END
            .part()
            .modelFile(end)
            .rotationY(90)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_EAST, DOWN_EAST, NORTH_EAST, SOUTH_EAST)
            .condition(PipeStraightBlock.HAS_END_END, true)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationY(90)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_EAST, DOWN_EAST, NORTH_EAST, SOUTH_EAST)
            .condition(PipeStraightBlock.HAS_END_END, false)
            .end()
            // ========================================= WEST_END
            .part()
            .modelFile(end)
            .rotationY(270)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_WEST, DOWN_WEST, NORTH_WEST, SOUTH_WEST)
            .condition(PipeStraightBlock.HAS_END_END, true)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationY(270)
            .addModel()
            .condition(PipeStraightBlock.CORNER_ENDED, UP_WEST, DOWN_WEST, NORTH_WEST, SOUTH_WEST)
            .condition(PipeStraightBlock.HAS_END_END, false)
            .end();
    }

    public static <T extends Block> void pipeNodeBlock(DataGenContext<Block, T> ctx, RegistrumBlockstateProvider provider) {
        ModelFile node = DangerUtil.genModModelFile("block/pipe_node").get();
        ModelFile noEnd = DangerUtil.genModModelFile("block/pipe_no_end").get();
        ModelFile end = DangerUtil.genModModelFile("block/pipe_end").get();
        provider.getMultipartBuilder(ctx.get())
            .part()
            .modelFile(node)
            .addModel()
            .end()
            // ========================================= NORTH
            .part()
            .modelFile(end)
            .addModel()
            .condition(PipeNodeBlock.NORTH, PipeBlock.NodePipe.END)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .addModel()
            .condition(PipeNodeBlock.NORTH, PipeBlock.NodePipe.PIPE)
            .end()
            // ========================================= SOUTH
            .part()
            .modelFile(end)
            .rotationY(180)
            .addModel()
            .condition(PipeNodeBlock.SOUTH, PipeBlock.NodePipe.END)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationY(180)
            .addModel()
            .condition(PipeNodeBlock.SOUTH, PipeBlock.NodePipe.PIPE)
            .end()
            // ========================================= EAST
            .part()
            .modelFile(end)
            .rotationY(90)
            .addModel()
            .condition(PipeNodeBlock.EAST, PipeBlock.NodePipe.END)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationY(90)
            .addModel()
            .condition(PipeNodeBlock.EAST, PipeBlock.NodePipe.PIPE)
            .end()
            // ========================================= WEST
            .part()
            .modelFile(end)
            .rotationY(270)
            .addModel()
            .condition(PipeNodeBlock.WEST, PipeBlock.NodePipe.END)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationY(270)
            .addModel()
            .condition(PipeNodeBlock.WEST, PipeBlock.NodePipe.PIPE)
            .end()
            // ========================================= DOWN
            .part()
            .modelFile(end)
            .rotationX(90)
            .addModel()
            .condition(PipeNodeBlock.DOWN, PipeBlock.NodePipe.END)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationX(90)
            .addModel()
            .condition(PipeNodeBlock.DOWN, PipeBlock.NodePipe.PIPE)
            .end()
            // ========================================= UP
            .part()
            .modelFile(end)
            .rotationX(270)
            .addModel()
            .condition(PipeNodeBlock.UP, PipeBlock.NodePipe.END)
            .end()
            // ========
            .part()
            .modelFile(noEnd)
            .rotationX(270)
            .addModel()
            .condition(PipeNodeBlock.UP, PipeBlock.NodePipe.PIPE)
            .end();
    }
}

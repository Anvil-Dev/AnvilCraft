package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.building.BuildingCommit;
import dev.dubhe.anvilcraft.client.renderer.blockentity.GlassPipeBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.GlassPipeFluidBERenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PipeCheckValveRenderState;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class GlassPipeClientScene {
    private static final List<BlockPos> POSITIONS = List.of(new BlockPos(-2, 82, 6), new BlockPos(0, 82, 6), new BlockPos(2, 82, 6));
    private static boolean started;
    private static volatile boolean prepared;
    @Nullable private static volatile Throwable failure;
    private static long deadline;
    private static int ready;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 150000;
        if (failure != null || System.currentTimeMillis() > deadline) throw new IllegalStateException("Glass display fixture", failure);
        if (capturing) return;
        if (!started) {
            started = true;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    var straight = ModBlocks.GLASS_PIPE_STRAIGHT.getDefaultState();
                    var corner = ModBlocks.GLASS_PIPE_CORNER.getDefaultState()
                        .setValue(PipeBlock.CORNER_ENDED, PipeBlock.CornerEnded.fromDirections(Direction.NORTH, Direction.UP));
                    var node = ModBlocks.GLASS_PIPE_NODE.getDefaultState();
                    for (var direction : Direction.values()) {
                        node = node.setValue(PipeBlock.getPropertyForDirection(direction), PipeBlock.NodePipe.PIPE);
                    }
                    var states = List.of(straight, corner, node);
                    BuildingCommit.quietly(level, () -> {
                        for (int index = 0; index < states.size(); index++) {
                            BuildingCommit.set(level, POSITIONS.get(index), states.get(index));
                        }
                    });
                    var a = (GlassPipeBlockEntity) level.getBlockEntity(POSITIONS.get(0));
                    var b = (GlassPipeBlockEntity) level.getBlockEntity(POSITIONS.get(1));
                    var c = (GlassPipeBlockEntity) level.getBlockEntity(POSITIONS.get(2));
                    a.showFluid(new FluidStack(Fluids.WATER, 1), Set.of(Direction.WEST, Direction.EAST));
                    b.showFluid(new FluidStack(Fluids.LAVA, 1), Set.of(Direction.NORTH, Direction.UP));
                    c.setGasDisplay(new FluidStack(Fluids.WATER, 1), Set.of(Direction.values()), 0.4F);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0 84 12 180 12");
                    server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
            return;
        }
        if (!prepared) return;
        for (var pos : POSITIONS) {
            if (!(client.level.getBlockEntity(pos) instanceof GlassPipeBlockEntity pipe) || pipe.getDisplayFluid().isEmpty()) return;
        }
        client.player.setPos(0, 84, 12);
        client.player.setYRot(180);
        client.player.setXRot(12);
        client.options.hideGui = true;
        if (++ready < 100) return;
        for (int index = 0; index < POSITIONS.size(); index++) {
            var pipe = (GlassPipeBlockEntity) client.level.getBlockEntity(POSITIONS.get(index));
            var candidate = client.getBlockEntityRenderDispatcher().<GlassPipeBlockEntity, PipeCheckValveRenderState>getRenderer(pipe);
            if (!(candidate instanceof GlassPipeBlockEntityRenderer renderer)) throw new IllegalStateException("Missing glass renderer");
            var state = renderer.createRenderState();
            renderer.extractRenderState(pipe, state, 0, Vec3.ZERO, null);
            var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            GlassPipeFluidBERenderer.renderDisplayFluid(state, state.blockState, state.directions, state.alpha,
                new PoseStack().last(), builder, state.lightCoords);
            var data = builder.buildOrThrow();
            int expectedVertices = new int[]{16, 48, 120}[index];
            if (data.drawState().vertexCount() != expectedVertices) throw new IllegalStateException("Fluid face count differs: " + index);
            int expectedAlpha = index == 2 ? 102 : 255;
            for (int vertex = 0; vertex < expectedVertices; vertex++) {
                int alpha = data.vertexBuffer().get(vertex * DefaultVertexFormat.BLOCK.getVertexSize() + 15) & 255;
                if (alpha != expectedAlpha) throw new IllegalStateException("Fluid opacity differs: " + alpha);
            }
            data.close();
        }
        capturing = true;
        Screenshot.grab(client.gameDirectory, "glass-pipe-26.1-state.png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                AnvilCraft.LOGGER.info("PORT_GLASS_PIPE_CLIENT_PASSED: synchronized states, three shapes, "
                    + "hidden internal faces and 0.4 opacity");
                client.stop();
            }));
    }
}

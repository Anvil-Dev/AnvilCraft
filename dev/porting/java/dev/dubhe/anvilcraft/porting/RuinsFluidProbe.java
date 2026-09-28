package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class RuinsFluidProbe {
    public static void verify(Minecraft client) {
        List<BlockPos> positions = new ArrayList<>();
        for (int row = 0; row < 4; row++) positions.add(new BlockPos(3, 162, row * 3 - 5));
        for (int x : new int[]{-17, -16, 15, 16}) positions.add(new BlockPos(x, 162, -10 - (x & 1) * 3));
        positions.add(RuinsFluidClientScene.PAIR);
        positions.add(RuinsFluidClientScene.PAIR.east());
        int vertices = 0;
        for (BlockPos pos : positions) {
            if (!(client.level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins)) {
                throw new IllegalStateException("Missing fluid ruins at " + pos);
            }
            Object handle = client.getBlockEntityRenderDispatcher().getRenderer(ruins);
            var renderer = (RuinsBlockEntityRenderer) handle;
            var state = renderer.createRenderState();
            renderer.extractRenderState(ruins, state, 0, client.gameRenderer.getMainCamera().position(), null);
            if (state.fluid == null || state.fluid.layers().isEmpty()) throw new IllegalStateException("Missing fluid geometry " + pos);
            int count = 0;
            for (var layer : state.fluid.layers().values()) {
                if (layer.size() % 4 != 0) throw new IllegalStateException("Incomplete fluid quad");
                for (var vertex : layer) {
                    if (vertex.x() < -0.001 || vertex.x() > 1.001 || vertex.y() < -0.001 || vertex.y() > 1.001
                        || vertex.z() < -0.001 || vertex.z() > 1.001) {
                        throw new IllegalStateException("Fluid escaped local block coordinates at " + pos + ": " + vertex);
                    }
                    if ((vertex.color() >>> 24) == 0) throw new IllegalStateException("Invisible fluid vertex");
                }
                if (pos.equals(RuinsFluidClientScene.PAIR) || pos.equals(RuinsFluidClientScene.PAIR.east())) {
                    float innerX = pos.equals(RuinsFluidClientScene.PAIR) ? 1 : 0;
                    for (int start = 0; start < layer.size(); start += 4) {
                        boolean innerFace = true;
                        for (int vertex = start; vertex < start + 4; vertex++) {
                            innerFace &= Math.abs(layer.get(vertex).x() - innerX) < 0.002;
                        }
                        if (innerFace) throw new IllegalStateException("Adjacent disguised water renders an internal face");
                    }
                }
                count += layer.size();
            }
            if (count < 4) throw new IllegalStateException("No fluid face " + pos);
            vertices += count;
        }
        AnvilCraft.LOGGER.info("PORT_RUINS_FLUID_GEOMETRY_PASSED: {} vertices across {} fluid/section fixtures",
            vertices, positions.size());
    }
}

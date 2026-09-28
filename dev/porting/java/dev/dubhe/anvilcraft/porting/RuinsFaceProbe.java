package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

public final class RuinsFaceProbe {
    public static void verify(Minecraft client) {
        require(client.getItemModelResolver().shouldPlaySwapAnimation(ModBlocks.RUINS_BLOCK.asStack()),
            "Ruins preserves the source item swap animation");
        var pairs = RuinsFinishClientScene.pairs();
        for (int row = 0; row < pairs.size(); row++) {
            var pair = pairs.get(row);
            var original = new BlockPos(-3, 162, row * 3);
            var disguised = new BlockPos(2, 162, row * 3);
            boolean expected = Block.shouldRenderFace(client.level, original, pair.self(),
                client.level.getBlockState(original.east()), Direction.EAST);
            boolean actual = Block.shouldRenderFace(client.level, disguised, pair.self(),
                client.level.getBlockState(disguised.east()), Direction.EAST);
            require(actual == expected && actual == pair.visible(), "Native face visibility pair " + row);
            require(quads(client, original) == quads(client, disguised), "Native tessellation face count pair " + row);
            if (Boolean.getBoolean("anvilcraft.portSodiumScene")) RuinsSodiumFaceProbe.verify(client, original, disguised, expected);
        }
        AnvilCraft.LOGGER.info("PORT_RUINS_FACE_PROBE_PASSED: {} original/disguised neighbor pairs", pairs.size());
    }

    private static int quads(Minecraft client, BlockPos pos) {
        var state = client.level.getBlockState(pos);
        var model = client.getModelManager().getBlockStateModelSet().get(state);
        var renderer = new ModelBlockRenderer(true, true, client.getBlockColors());
        int[] count = {0};
        renderer.tesselateBlock((x, y, z, quad, lighting) -> {
            if (quad.direction() == Direction.EAST) count[0]++;
        }, 0, 0, 0, client.level, pos, state, model, state.getSeed(pos));
        return count[0];
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}

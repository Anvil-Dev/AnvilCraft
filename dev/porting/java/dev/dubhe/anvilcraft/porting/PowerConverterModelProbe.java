package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.block.power.converter.BasePowerConverterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public final class PowerConverterModelProbe {
    public static void verify(Minecraft client) {
        var parts = new ArrayList<BlockStateModelPart>();
        int states = 0;
        var report = new java.util.LinkedHashMap<String, List<Double>>();
        for (var block : List.<BasePowerConverterBlock>of(
            ModBlocks.POWER_CONVERTER_SMALL.get(), ModBlocks.POWER_CONVERTER_MIDDLE.get(),
            ModBlocks.POWER_CONVERTER_BIG.get(), ModBlocks.POWER_CONVERTER_SUPER_BIG.get(),
            ModBlocks.POWER_CONVERTER_EXTREMELY_BIG.get())) {
            for (var state : block.getStateDefinition().getPossibleStates()) {
                double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
                boolean dark = state.getValue(BasePowerConverterBlock.POWERED) || state.getValue(BasePowerConverterBlock.OVERLOAD);
                parts.clear();
                client.getModelManager().getBlockStateModelSet().get(state).collectParts(client.level, BlockPos.ZERO, state,
                    RandomSource.create(42), parts);
                int quads = 0;
                for (var part : parts) {
                    var all = new ArrayList<>(part.getQuads(null));
                    for (Direction side : Direction.values()) all.addAll(part.getQuads(side));
                    for (var quad : all) {
                        String texture = quad.materialInfo().sprite().contents().name().getPath();
                        if (texture.contains("missing") || texture.contains("overload") != dark) {
                            throw new IllegalStateException("Converter material mismatch: " + state + " " + texture);
                        }
                        for (int vertex = 0; vertex < 4; vertex++) {
                            var point = quad.position(vertex);
                            bounds[0] = Math.min(bounds[0], point.x());
                            bounds[1] = Math.min(bounds[1], point.y());
                            bounds[2] = Math.min(bounds[2], point.z());
                            bounds[3] = Math.max(bounds[3], point.x());
                            bounds[4] = Math.max(bounds[4], point.y());
                            bounds[5] = Math.max(bounds[5], point.z());
                        }
                        quads++;
                    }
                }
                if (quads == 0) throw new IllegalStateException("Missing converter model: " + state);
                report.put(state.toString(), java.util.Arrays.stream(bounds).boxed().toList());
                states++;
            }
        }
        if (states != 120) throw new IllegalStateException("Converter model state coverage: " + states);
        try {
            java.nio.file.Files.writeString(client.gameDirectory.toPath().resolve("power-converter-models-26.1.json"),
                new com.google.gson.Gson().toJson(report));
        } catch (java.io.IOException error) {
            throw new IllegalStateException(error);
        }
    }
}

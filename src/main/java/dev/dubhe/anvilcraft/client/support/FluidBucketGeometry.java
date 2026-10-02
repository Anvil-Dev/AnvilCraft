package dev.dubhe.anvilcraft.client.support;

import com.mojang.math.Quadrant;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;

public final class FluidBucketGeometry {
    private FluidBucketGeometry() {
    }

    public static QuadCollection bake(
        ModelBaker baker, Material.Baked mask, BakedQuad.MaterialInfo material, ModelState state, ExtraFaceData faceData
    ) {
        var interner = baker.interner();
        var result = new QuadCollection.Builder();
        var sides = new QuadCollection.Builder();
        var maskInfo = interner.materialInfo(BakedQuad.MaterialInfo.of(mask, mask.sprite().transparency(), -1, true, 0, true));
        ItemModelGenerator.bakeSideFaces(sides, interner, state, maskInfo, faceData);
        appendSides(result, sides.build().getAll(), mask.sprite(), material);
        var sprite = mask.sprite().contents();
        int width = sprite.width();
        int height = sprite.height();
        var pixels = new BitSet(width * height);
        sprite.getUniqueFrames().forEach(frame -> {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (!sprite.isTransparent(frame, x, y)) pixels.set(x + y * width);
                }
            }
        });
        for (int y = 0; y < height; y++) {
            int start = -1;
            for (int x = 0; x < width; x++) {
                if (pixels.get(x + y * width) != (start == -1)) continue;
                if (start == -1) {
                    start = x;
                    continue;
                }
                int end = y + 1;
                expand:
                for (; end < height; end++) {
                    for (int column = start; column <= x; column++) {
                        if (!pixels.get(column + end * width)) break expand;
                    }
                }
                for (int column = start; column < x; column++) {
                    for (int row = y; row < end; row++) pixels.clear(column + row * width);
                }
                var from = new Vector3f(16 * start / (float) width, 16 - 16 * end / (float) height, 7.5F);
                var to = new Vector3f(16 * x / (float) width, 16 - 16 * y / (float) height, 8.5F);
                for (Direction direction : new Direction[]{Direction.DOWN, Direction.UP, Direction.SOUTH,
                    Direction.NORTH, Direction.WEST, Direction.EAST}) {
                    result.addUnculledFace(shrink(FaceBakery.bakeQuad(interner, from, to, FaceBakery.defaultFaceUV(from, to, direction),
                        Quadrant.R0, material, direction, state, null, faceData)));
                }
                start = -1;
            }
        }
        return result.build();
    }

    private static long remap(long uv, TextureAtlasSprite from, TextureAtlasSprite to) {
        float u = (UVPair.unpackU(uv) - from.getU0()) / (from.getU1() - from.getU0());
        float v = (UVPair.unpackV(uv) - from.getV0()) / (from.getV1() - from.getV0());
        return UVPair.pack(to.getU(u), to.getV(v));
    }

    private static void appendSides(
        QuadCollection.Builder result, List<BakedQuad> quads, TextureAtlasSprite mask, BakedQuad.MaterialInfo material
    ) {
        var spans = new LinkedHashMap<Side, List<BakedQuad>>();
        for (var quad : quads) {
            var side = new Side(quad.direction(), coordinate(quad.position0(), quad.direction().getAxis()));
            spans.computeIfAbsent(side, _ -> new ArrayList<>()).add(quad);
        }
        for (var span : spans.values()) {
            var first = span.getFirst();
            var axis = first.direction().getAxis() == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
            float min = Float.POSITIVE_INFINITY;
            float max = Float.NEGATIVE_INFINITY;
            for (var quad : span) {
                for (int index = 0; index < 4; index++) {
                    min = Math.min(min, coordinate(quad.position(index), axis));
                    max = Math.max(max, coordinate(quad.position(index), axis));
                }
            }
            float midpoint = 0;
            for (int index = 0; index < 4; index++) midpoint += coordinate(first.position(index), axis) / 4;
            var positions = new Vector3fc[4];
            var uvs = new long[4];
            for (int index = 0; index < 4; index++) {
                float extreme = coordinate(first.position(index), axis) > midpoint ? max : min;
                for (var quad : span) {
                    for (int vertex = 0; vertex < 4; vertex++) {
                        var pos = quad.position(vertex);
                        if (coordinate(pos, axis) == extreme && pos.z() == first.position(index).z()) {
                            positions[index] = pos;
                            uvs[index] = remap(quad.packedUV(vertex), mask, material.sprite());
                        }
                    }
                }
            }
            result.addUnculledFace(finishSide(positions, uvs, first, mask, material));
        }
    }

    private static BakedQuad finishSide(
        Vector3fc[] positions, long[] uvs, BakedQuad first, TextureAtlasSprite mask, BakedQuad.MaterialInfo material
    ) {
        var sprite = material.sprite();
        float factor = shrinkFactor(mask);
        float[] us = new float[4];
        float[] vs = new float[4];
        float centerU = 0;
        float centerV = 0;
        for (int index = 0; index < 4; index++) {
            us[index] = (UVPair.unpackU(uvs[index]) - sprite.getU0()) / (sprite.getU1() - sprite.getU0());
            vs[index] = (UVPair.unpackV(uvs[index]) - sprite.getV0()) / (sprite.getV1() - sprite.getV0());
            centerU += us[index] / 4;
            centerV += vs[index] / 4;
        }
        for (int index = 0; index < 4; index++) {
            float u = us[index] + Math.copySign(0.1F / mask.contents().width(), us[index] - centerU);
            float v = vs[index] + Math.copySign(0.1F / mask.contents().height(), vs[index] - centerV);
            if (first.direction().getAxis() == Direction.Axis.Y) u = Math.clamp(u + factor * (u - centerU), 0, 1);
            else v = Math.clamp(v + factor * (v - centerV), 0, 1);
            uvs[index] = UVPair.pack(sprite.getU(u), sprite.getV(v));
            var pos = positions[index];
            positions[index] = new Vector3f(Math.clamp(pos.x() + factor * (pos.x() - 0.5F), 0, 1),
                Math.clamp(pos.y() + factor * (pos.y() - 0.5F), 0, 1), pos.z());
        }
        return shrink(new BakedQuad(positions[0], positions[1], positions[2], positions[3],
            uvs[0], uvs[1], uvs[2], uvs[3], first.direction(), material, first.bakedNormals(), first.bakedColors()));
    }

    private static BakedQuad shrink(BakedQuad quad) {
        var sprite = quad.materialInfo().sprite();
        float factor = shrinkFactor(sprite);
        float centerU = 0;
        float centerV = 0;
        for (int index = 0; index < 4; index++) {
            centerU += UVPair.unpackU(quad.packedUV(index)) / 4;
            centerV += UVPair.unpackV(quad.packedUV(index)) / 4;
        }
        var uvs = new long[4];
        for (int index = 0; index < 4; index++) {
            float u = UVPair.unpackU(quad.packedUV(index));
            float v = UVPair.unpackV(quad.packedUV(index));
            uvs[index] = UVPair.pack(u + factor * (centerU - u), v + factor * (centerV - v));
        }
        return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
            uvs[0], uvs[1], uvs[2], uvs[3], quad.direction(), quad.materialInfo(), quad.bakedNormals(), quad.bakedColors());
    }

    private static float shrinkFactor(TextureAtlasSprite sprite) {
        return 4 / Math.max(sprite.contents().width() / (sprite.getU1() - sprite.getU0()),
            sprite.contents().height() / (sprite.getV1() - sprite.getV0()));
    }

    private static float coordinate(Vector3fc position, Direction.Axis axis) {
        return switch (axis) {
            case X -> position.x();
            case Y -> position.y();
            case Z -> position.z();
        };
    }

    private record Side(Direction direction, float coordinate) {
    }
}

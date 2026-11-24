package dev.dubhe.anvilcraft.client.renderer.blockentity;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class ChutePlateModel {
    private ChutePlateModel() {
    }

    static BlockModel shortened(BlockModel model, Direction rim, int cut) {
        List<BlockElement> pieces = new ArrayList<>();
        int half = (16 - cut) / 2;
        boolean positive = rim.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        for (BlockElement element : model.getElements()) {
            appendSlice(pieces, element, rim.getAxis(), 0, half, positive ? 0 : cut);
            appendSlice(pieces, element, rim.getAxis(), 16 - half, 16, positive ? -cut : 0);
        }
        return new BlockModel(null, pieces, Map.of(), model.hasAmbientOcclusion(), model.getGuiLight(), model.getTransforms(), List.of());
    }

    static BakedModel bake(BlockModel model, BlockModel source, Function<Material, TextureAtlasSprite> sprites) {
        var builder = new SimpleBakedModel.Builder(source, ItemOverrides.EMPTY, true)
            .particle(sprites.apply(source.getMaterial("particle")));
        for (BlockElement element : model.getElements()) {
            element.faces.forEach((side, face) -> builder.addUnculledFace(
                BlockModel.bakeFace(element, face, sprites.apply(source.getMaterial(face.texture())), side, BlockModelRotation.X0_Y0)
            ));
        }
        return builder.build();
    }

    private static void appendSlice(List<BlockElement> pieces, BlockElement element, Direction.Axis axis, int low, int high, int offset) {
        int index = axis.ordinal();
        float start = element.from.get(index);
        float end = element.to.get(index);
        float a = Math.max(start, low);
        float b = Math.min(end, high);
        if (b < a || b == a && start != end) {
            return;
        }
        var from = new Vector3f(element.from).setComponent(index, a);
        var to = new Vector3f(element.to).setComponent(index, b);
        var faces = new EnumMap<Direction, BlockElementFace>(Direction.class);
        var slice = new BlockElement(from, to, faces, element.rotation, element.shade, element.getFaceData());
        element.faces.forEach((side, face) -> {
            if (side.getAxis() == axis && (side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? b != end : a != start)) {
                return;
            }
            float[] original = element.uvsByFace(side);
            if (original[0] == original[2] || original[1] == original[3]) {
                return;
            }
            BlockFaceUV uv = sliceUV(original, slice.uvsByFace(side), face.uv());
            faces.put(side, new BlockElementFace(face.cullForDirection(), face.tintIndex(), face.texture(), uv, face.faceData(),
                new MutableObject<>(slice)));
        });
        if (!faces.isEmpty()) {
            // 在原板上裁 UV 后再平移，保留接缝两侧的纹素宽度。
            from.setComponent(index, a + offset);
            to.setComponent(index, b + offset);
            pieces.add(slice);
        }
    }

    private static BlockFaceUV sliceUV(float[] original, float[] region, BlockFaceUV uv) {
        float u0 = Mth.inverseLerp(region[0], original[0], original[2]);
        float u1 = Mth.inverseLerp(region[2], original[0], original[2]);
        float v0 = Mth.inverseLerp(region[1], original[1], original[3]);
        float v1 = Mth.inverseLerp(region[3], original[1], original[3]);
        float[] fraction = switch (uv.rotation) {
            case 90 -> new float[]{v0, 1 - u1, v1, 1 - u0};
            case 180 -> new float[]{1 - u1, 1 - v1, 1 - u0, 1 - v0};
            case 270 -> new float[]{1 - v1, u0, 1 - v0, u1};
            default -> new float[]{u0, v0, u1, v1};
        };
        return new BlockFaceUV(new float[]{
            Mth.lerp(fraction[0], uv.uvs[0], uv.uvs[2]), Mth.lerp(fraction[1], uv.uvs[1], uv.uvs[3]),
            Mth.lerp(fraction[2], uv.uvs[0], uv.uvs[2]), Mth.lerp(fraction[3], uv.uvs[1], uv.uvs[3])
        }, uv.rotation);
    }
}

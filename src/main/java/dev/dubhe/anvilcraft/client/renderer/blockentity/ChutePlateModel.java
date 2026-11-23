package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

final class ChutePlateModel {
    private ChutePlateModel() {
    }

    static SimpleUnbakedStandaloneModel<BlockStateModel> shortened(Identifier id, BlockModelRotation rotation, Direction rim, int cut) {
        return new SimpleUnbakedStandaloneModel<>(id, (source, baker, name) -> {
            if (!(source.getTopGeometry() instanceof UnbakedCuboidGeometry geometry)) {
                return new SingleVariant(SimpleModelWrapper.bake(baker, source, rotation));
            }
            List<CuboidModelElement> pieces = new ArrayList<>();
            int half = (16 - cut) / 2;
            boolean positive = rim.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            for (CuboidModelElement element : geometry.elements()) {
                appendSlice(pieces, element, rim.getAxis(), 0, half, positive ? 0 : cut);
                appendSlice(pieces, element, rim.getAxis(), 16 - half, 16, positive ? -cut : 0);
            }
            var model = new CuboidModel(new UnbakedCuboidGeometry(pieces), null, null, null, TextureSlots.Data.EMPTY, id);
            return new SingleVariant(SimpleModelWrapper.bake(baker, baker.resolveInlineModel(model, name), rotation));
        });
    }

    private static void appendSlice(
        List<CuboidModelElement> pieces, CuboidModelElement element, Direction.Axis axis, int low, int high, int offset
    ) {
        int index = axis.ordinal();
        float start = element.from().get(index);
        float end = element.to().get(index);
        float a = Math.max(start, low);
        float b = Math.min(end, high);
        if (b < a || b == a && start != end) {
            return;
        }
        var from = new Vector3f(element.from()).setComponent(index, a);
        var to = new Vector3f(element.to()).setComponent(index, b);
        var faces = new EnumMap<Direction, CuboidFace>(Direction.class);
        element.faces().forEach((side, face) -> {
            if (side.getAxis() == axis && (side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? b != end : a != start)) {
                return;
            }
            CuboidFace.UVs original = FaceBakery.defaultFaceUV(element.from(), element.to(), side);
            CuboidFace.UVs region = FaceBakery.defaultFaceUV(from, to, side);
            CuboidFace.UVs uv = sliceUV(original, region, face.uvs() == null ? original : face.uvs(), face.rotation());
            faces.put(side, new CuboidFace(face.cullForDirection(), face.tintIndex(), face.texture(), uv, face.rotation(),
                face.faceData(), new MutableObject<>()));
        });
        if (!faces.isEmpty()) {
            // 先在原板上裁 UV，再平移两半；拼缝两侧的每个纹素仍然占一个模型像素。
            from.setComponent(index, a + offset);
            to.setComponent(index, b + offset);
            pieces.add(new CuboidModelElement(from, to, faces, element.rotation(), element.shade(),
                element.lightEmission(), element.faceData()));
        }
    }

    private static CuboidFace.UVs sliceUV(CuboidFace.UVs original, CuboidFace.UVs region, CuboidFace.UVs uv, Quadrant rotation) {
        float u0 = Mth.inverseLerp(region.minU(), original.minU(), original.maxU());
        float u1 = Mth.inverseLerp(region.maxU(), original.minU(), original.maxU());
        float v0 = Mth.inverseLerp(region.minV(), original.minV(), original.maxV());
        float v1 = Mth.inverseLerp(region.maxV(), original.minV(), original.maxV());
        CuboidFace.UVs fraction = switch (rotation) {
            case R0 -> new CuboidFace.UVs(u0, v0, u1, v1);
            case R90 -> new CuboidFace.UVs(v0, 1 - u1, v1, 1 - u0);
            case R180 -> new CuboidFace.UVs(1 - u1, 1 - v1, 1 - u0, 1 - v0);
            case R270 -> new CuboidFace.UVs(1 - v1, u0, 1 - v0, u1);
        };
        return new CuboidFace.UVs(
            Mth.lerp(fraction.minU(), uv.minU(), uv.maxU()), Mth.lerp(fraction.minV(), uv.minV(), uv.maxV()),
            Mth.lerp(fraction.maxU(), uv.minU(), uv.maxU()), Mth.lerp(fraction.maxV(), uv.minV(), uv.maxV())
        );
    }
}

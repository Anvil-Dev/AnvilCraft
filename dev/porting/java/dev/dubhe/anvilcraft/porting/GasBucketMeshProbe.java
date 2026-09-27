package dev.dubhe.anvilcraft.porting;

import com.google.gson.Gson;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GasBucketMeshProbe {
    public static void verify(Minecraft client) {
        try {
            var state = new ItemStackRenderState();
            client.getItemModelResolver().updateForTopItem(state, ModItems.HYDROGEN_BUCKET.asStack(),
                ItemDisplayContext.GUI, client.level, client.player, 0);
            var field = ItemStackRenderState.class.getDeclaredField("layers");
            field.setAccessible(true);
            var result = new ArrayList<>();
            for (var layer : (ItemStackRenderState.LayerRenderState[]) field.get(state)) {
                for (var quad : layer.prepareQuadList()) {
                    var sprite = quad.materialInfo().sprite();
                    var vertices = new ArrayList<>();
                    for (int index = 0; index < 4; index++) {
                        var pos = quad.position(index);
                        vertices.add(List.of(pos.x(), pos.y(), pos.z(),
                            (UVPair.unpackU(quad.packedUV(index)) - sprite.getU0()) / (sprite.getU1() - sprite.getU0()),
                            (UVPair.unpackV(quad.packedUV(index)) - sprite.getV0()) / (sprite.getV1() - sprite.getV0())));
                    }
                    result.add(Map.of("sprite", sprite.contents().name().toString(),
                        "direction", quad.direction().name(), "vertices", vertices));
                }
            }
            Files.writeString(client.gameDirectory.toPath().resolve("gas-bucket-mesh-26.1.json"), new Gson().toJson(result));
        } catch (ReflectiveOperationException | java.io.IOException error) {
            throw new IllegalStateException(error);
        }
    }
}

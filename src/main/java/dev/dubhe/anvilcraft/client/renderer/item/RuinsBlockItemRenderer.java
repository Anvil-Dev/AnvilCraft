package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public final class RuinsBlockItemRenderer implements SpecialModelRenderer<RuinsBlockItemRenderer.Argument> {
    public enum Argument { INSTANCE }

    @Override
    public Argument extractArgument(ItemStack stack) {
        return Argument.INSTANCE;
    }

    @Override
    public void submit(@Nullable Argument argument, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                       boolean foil, int outlineColor) {
        CelestialBodyRenderer.submitEndGatewayBody(pose, collector);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) output.accept(new Vector3f(x, y, z));
            }
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Argument> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public RuinsBlockItemRenderer bake(BakingContext context) {
            return new RuinsBlockItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}

package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.client.support.DiskDisplaySupport;
import dev.dubhe.anvilcraft.client.support.FittedItemRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class DiskItemRenderer implements SpecialModelRenderer<DiskItemRenderer.Argument> {
    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        if (FittedItemRenderer.isRenderingPreview()) return null;
        var icon = FittedItemRenderer.prepare(DiskDisplaySupport.recordedBlock(stack));
        return icon == null ? null : new Argument(icon, FittedItemRenderer.frontZ(stack));
    }

    @Override
    public void submit(@Nullable Argument argument, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                       boolean hasFoil, int outlineColor) {
        if (argument == null || FittedItemRenderer.isRenderingPreview()) return;
        pose.pushPose();
        pose.translate(0.5F, 6F / 16, argument.frontZ + 0.01F);
        FittedItemRenderer.submit(argument.icon, 6F / 16, pose, collector, light, overlay, false);
        pose.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(5F / 16, 3F / 16, 8.5F / 16 + 0.01F));
        output.accept(new Vector3f(11F / 16, 9F / 16, 8.5F / 16 + 0.01F));
    }

    public record Argument(FittedItemRenderer.Icon icon, float frontZ) {
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Argument> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public DiskItemRenderer bake(BakingContext context) {
            return new DiskItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}

package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.client.support.DiskDisplaySupport;
import dev.dubhe.anvilcraft.client.support.FittedItemRenderer;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class DiskItemRenderer implements SpecialModelRenderer<DiskItemRenderer.Argument> {
    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        if (FittedItemRenderer.isRenderingPreview()) return null;
        var icon = FittedItemRenderer.prepare(DiskDisplaySupport.getDisplay(stack));
        boolean blueprint = stack.is(ModItems.STRUCTURE_DISK);
        return icon == null ? null : new Argument(icon, FittedItemRenderer.frontZ(stack), blueprint, blueprint ? Util.getMillis() : 0);
    }

    @Override
    public void submit(@Nullable Argument argument, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                       boolean hasFoil, int outlineColor) {
        if (argument == null || FittedItemRenderer.isRenderingPreview()) return;
        pose.pushPose();
        pose.translate(0.5F, argument.blueprint ? 0.5F : 6F / 16, argument.frontZ + 0.01F);
        if (argument.blueprint) {
            FittedItemRenderer.submitBlueprint(argument.icon, 8F / 16, pose, collector, light, overlay);
        } else {
            FittedItemRenderer.submit(argument.icon, 6F / 16, pose, collector, light, overlay, false);
        }
        pose.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(4F / 16, 3F / 16, 8.5F / 16 + 0.01F));
        output.accept(new Vector3f(12F / 16, 12F / 16, 8.5F / 16 + 0.01F));
    }

    public record Argument(FittedItemRenderer.Icon icon, float frontZ, boolean blueprint, long animationIdentity) {
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

package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.client.support.FittedItemRenderer;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class FilterItemRenderer implements SpecialModelRenderer<FilterItemRenderer.Argument> {
    public static ItemStack selectDisplayed(@Nullable FilterContent content, long timeMillis) {
        if (content == null) return ItemStack.EMPTY;
        int count = 0;
        for (ItemStack item : content.list()) {
            if (!item.isEmpty()) count++;
        }
        if (count == 0) return ItemStack.EMPTY;
        int selected = Math.floorMod(timeMillis / 1000, count);
        for (ItemStack item : content.list()) {
            if (!item.isEmpty() && selected-- == 0) return item;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        if (FittedItemRenderer.isRenderingPreview()) return null;
        var content = stack.get(ModComponents.FILTER_CONTENT);
        ItemStack item = selectDisplayed(content, Util.getMillis());
        if (item.isEmpty()) return null;
        var icon = FittedItemRenderer.prepare(item);
        var barrier = content.blackList() ? FittedItemRenderer.prepare(Items.BARRIER.getDefaultInstance()) : null;
        return new Argument(icon, barrier, item.hasFoil(), FittedItemRenderer.frontZ(stack), item.hasFoil() ? System.nanoTime() : 0);
    }

    @Override
    public void submit(@Nullable Argument argument, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                       boolean hasFoil, int outlineColor) {
        if (argument == null || FittedItemRenderer.isRenderingPreview()) return;
        pose.pushPose();
        pose.translate(0.5F, 0.5F, argument.frontZ + 0.01F);
        if (argument.icon != null) {
            FittedItemRenderer.submit(argument.icon, 8F / 16, pose, collector, light, overlay, argument.foil);
        }
        pose.translate(0, 0, 0.02F);
        if (argument.barrier != null) FittedItemRenderer.submit(argument.barrier, 4F / 16, pose, collector, light, overlay, false);
        pose.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0.25F, 0.25F, 8.5F / 16 + 0.01F));
        output.accept(new Vector3f(0.75F, 0.75F, 8.5F / 16 + 0.03F));
    }

    public record Argument(
        FittedItemRenderer.@Nullable Icon icon, FittedItemRenderer.@Nullable Icon barrier,
        boolean foil, float frontZ, long animationIdentity
    ) {
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Argument> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public FilterItemRenderer bake(BakingContext context) {
            return new FilterItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}

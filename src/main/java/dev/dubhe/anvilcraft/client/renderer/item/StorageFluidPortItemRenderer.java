package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientStoragePortTooltip;
import dev.dubhe.anvilcraft.client.renderer.item.state.FluidTankItemRenderState;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/** 组合模型负责外壳；此层只绘制窗口里的液面或气体。 */
public class StorageFluidPortItemRenderer implements SpecialModelRenderer<StorageFluidPortItemRenderer.Contents> {
    private static final float INSET = 3 / 16F + 0.001F;

    public record Contents(FluidResource fluid, float fill, TextureAtlasSprite sprite, int color, @Nullable Object animationKey) {
    }

    @Override
    public @Nullable Contents extractArgument(ItemStack stack) {
        var data = FluidTankItemTooltip.readSingleTank(stack, ClientStoragePortTooltip.registries());
        if (data == null) return null;
        var fluid = data.fluid();
        var model = FluidRenderHelper.getModel(Minecraft.getInstance().getModelManager().getFluidStateModelSet(), fluid.getFluid());
        var sprite = model.stillMaterial().sprite();
        var tint = model.fluidTintSource();
        return new Contents(FluidResource.of(fluid), Mth.clamp((float) fluid.getAmount() / StorageFluidPortBlockEntity.CAPACITY_MB, 0, 1),
            sprite, tint == null ? -1 : tint.colorAsStack(fluid), sprite.contents().isAnimated() ? new Object() : null);
    }

    @Override
    public void submit(@Nullable Contents contents, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                       boolean hasFoil, int outlineColor) {
        if (contents == null) return;
        boolean gas = contents.fluid().getFluidType().isLighterThanAir();
        float top = gas ? 1 - INSET : INSET + (1 - 2 * INSET) * contents.fill();
        collector.submitCustomGeometry(pose, FluidTankItemRenderState.FLUID_RENDER_TYPE, (matrix, buffer) ->
            FluidRenderHelper.INSTANCE.renderFluidBox(contents.sprite(), contents.fluid(), INSET, INSET, INSET,
                1 - INSET, top, 1 - INSET, contents.color(), buffer, matrix, light, true, false, gas ? contents.fill() : 1));
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) output.accept(new Vector3f(x, y, z));
            }
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Contents> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public StorageFluidPortItemRenderer bake(BakingContext context) {
            return new StorageFluidPortItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}

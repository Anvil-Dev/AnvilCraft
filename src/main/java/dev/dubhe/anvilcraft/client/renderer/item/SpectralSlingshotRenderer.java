package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.client.renderer.item.state.SpectralRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class SpectralSlingshotRenderer implements SpecialModelRenderer<SpectralRenderState> {
    private final ItemModelResolver resolver;

    public SpectralSlingshotRenderer(ItemModelResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public @Nullable SpectralRenderState extractArgument(ItemStack stack) {
        if (!stack.is(ModItems.SPECTRAL_SLINGSHOT)) return null;
        ChargedProjectiles chargedProjectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (chargedProjectiles == null || chargedProjectiles.itemCopies().isEmpty()) return null;
        SpectralRenderState state = new SpectralRenderState();
        state.setAmmo(FeatureRendererSupport.initialize(
            chargedProjectiles.itemCopies().getFirst(),
            this.resolver
        ));
        return state;
    }

    @Override
    public void submit(
        @Nullable SpectralRenderState argument,
        PoseStack pose,
        SubmitNodeCollector collector,
        int lightCoords,
        int overlayCoords,
        boolean hasFoil,
        int outlineColor
    ) {
        if (argument == null) return;
        pose.pushPose();
        pose.translate(0F, 0.7F, 0.50F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.ZP.rotationDegrees(- 45));
        argument.getAmmo().item.submit(
            pose,
            collector,
            lightCoords,
            overlayCoords,
            outlineColor
        );
        pose.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack pose = new PoseStack();
        pose.translate(0F, 0.7F, 0.50F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.ZP.rotationDegrees(- 45));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<SpectralRenderState> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked.INSTANCE);

        @Override
        public SpectralSlingshotRenderer bake(BakingContext context) {
            return new SpectralSlingshotRenderer(Minecraft.getInstance().getItemModelResolver());
        }

        @Override
        public MapCodec<Unbaked> type() {
            return Unbaked.CODEC;
        }
    }
}

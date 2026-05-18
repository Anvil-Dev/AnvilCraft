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
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class SpectralWeaponLauncherRenderer implements SpecialModelRenderer<SpectralRenderState> {
    private final ItemModelResolver resolver;

    public SpectralWeaponLauncherRenderer(ItemModelResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public @Nullable SpectralRenderState extractArgument(ItemStack stack) {
        if (!stack.is(ModItems.SPECTRAL_WEAPON_LAUNCHER)) return null;
        if (!stack.has(DataComponents.CHARGED_PROJECTILES)) return null;
        SpectralRenderState state = new SpectralRenderState();
        state.setSelf(FeatureRendererSupport.initialize(stack, this.resolver));
        state.setAmmo(FeatureRendererSupport.initialize(
            stack.get(DataComponents.CHARGED_PROJECTILES).itemCopies().getFirst(),
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
        argument.getSelf().item.submit(
            pose,
            collector,
            lightCoords,
            overlayCoords,
            outlineColor
        );
        pose.pushPose();
        pose.translate(0F, 7F / 16F, 7F / 8F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.ZN.rotationDegrees(45));
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
        pose.translate(0F, 7F / 16F, 7F / 8F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.ZN.rotationDegrees(45));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<SpectralRenderState> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked.INSTANCE);

        @Override
        public SpectralWeaponLauncherRenderer bake(BakingContext context) {
            return new SpectralWeaponLauncherRenderer(Minecraft.getInstance().getItemModelResolver());
        }

        @Override
        public MapCodec<Unbaked> type() {
            return Unbaked.CODEC;
        }
    }
}

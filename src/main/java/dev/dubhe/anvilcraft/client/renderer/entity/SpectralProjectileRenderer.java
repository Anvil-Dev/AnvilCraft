package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.entity.state.SpectralProjectileRenderState;
import dev.dubhe.anvilcraft.entity.SpectralProjectileEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SpectralProjectileRenderer<T extends SpectralProjectileEntity> extends ArrowRenderer<T, SpectralProjectileRenderState> {
    public static final Identifier ARROW_LOCATION = Identifier.withDefaultNamespace(
        "textures/entity/projectiles/arrow.png"
    );
    private final ItemModelResolver resolver;

    public SpectralProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.resolver = ctx.getItemModelResolver();
    }

    @Override
    public Identifier getTextureLocation(SpectralProjectileRenderState state) {
        return ARROW_LOCATION;
    }

    @Override
    public SpectralProjectileRenderState createRenderState() {
        return new SpectralProjectileRenderState();
    }

    @Override
    public void extractRenderState(T entity, SpectralProjectileRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.setStack(entity.getAsItemStack());
        state.setState(new ItemClusterRenderState());
        state.getState().extractItemGroupRenderState(entity, state.getStack(), this.resolver);
    }

    @Override
    public void submit(
        SpectralProjectileRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        ItemStack arrow = state.getStack();
        if (arrow.is(ItemTags.ARROWS)) {
            // 由于不再能装载箭矢了，所以不半透明也无所谓了（）
            super.submit(state, pose, collector, camera);
            return;
        }
        final RandomSource random = RandomSource.create(Item.getId(arrow.getItem()) + arrow.getDamageValue());
        ItemClusterRenderState cluster = state.getState();
        int amount = cluster.count;
        if (amount == 0) return;
        random.setSeed(cluster.seed);

        ItemStackRenderState item = cluster.item;
        item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, cluster.outlineColor);
        for (int i = 1; i < amount; i++) {
            pose.pushPose();
            float xo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
            float yo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
            float zo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
            if (cluster.shouldSpread) {
                pose.translate(xo, yo, zo);
            }
            item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, cluster.outlineColor);
            pose.popPose();
        }
    }
}



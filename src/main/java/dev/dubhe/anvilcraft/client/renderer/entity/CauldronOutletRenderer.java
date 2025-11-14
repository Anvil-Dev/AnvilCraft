package dev.dubhe.anvilcraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CauldronOutletRenderer<T extends net.minecraft.world.entity.Entity> extends EntityRenderer<T> {
    public CauldronOutletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "missingno");
    }
}
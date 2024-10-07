package dev.dubhe.anvilcraft.util;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;

public class ModClientFluidTypeExtensionImpl implements IClientFluidTypeExtensions {
    public final ResourceLocation stillTexture;
    public final ResourceLocation flowingTexture;


    public ModClientFluidTypeExtensionImpl(ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
    }

    public ModClientFluidTypeExtensionImpl(ResourceLocation texture) {
        this(texture, texture);
    }

    public @NotNull ResourceLocation getStillTexture() {
        return stillTexture;
    }

    public @NotNull ResourceLocation getFlowingTexture() {
        return flowingTexture;
    }
}
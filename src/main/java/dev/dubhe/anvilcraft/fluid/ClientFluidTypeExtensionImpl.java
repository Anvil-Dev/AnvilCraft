package dev.dubhe.anvilcraft.fluid;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

@MethodsReturnNonnullByDefault
public class ClientFluidTypeExtensionImpl implements IClientFluidTypeExtensions {
    public final ResourceLocation stillTexture;
    public final ResourceLocation flowingTexture;


    public ClientFluidTypeExtensionImpl(ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return flowingTexture;
    }

    @Override
    public ResourceLocation getStillTexture() {
        return stillTexture;
    }
}

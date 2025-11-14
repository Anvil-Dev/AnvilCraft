package dev.dubhe.anvilcraft.client.renderer.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class CustomRenderItemClientExtension implements IClientItemExtensions {
    protected BlockEntityWithoutLevelRenderer renderer;

    protected CustomRenderItemClientExtension(BlockEntityWithoutLevelRenderer renderer) {
        this.renderer = renderer;
    }

    public static CustomRenderItemClientExtension of(BlockEntityWithoutLevelRenderer renderer) {
        return new CustomRenderItemClientExtension(renderer);
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return renderer;
    }
}
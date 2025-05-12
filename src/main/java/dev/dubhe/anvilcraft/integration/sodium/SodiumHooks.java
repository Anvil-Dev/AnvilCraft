package dev.dubhe.anvilcraft.integration.sodium;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;

@SuppressWarnings("unused")
public class SodiumHooks {
    /// Also works for embeddium xD
    public static int modifyLightForEmissiveItems(
        BakedQuad quad,
        int light
    ) {
        return !quad.isShade() ? LightTexture.FULL_BRIGHT : light;
    }
}

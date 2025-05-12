package dev.dubhe.anvilcraft.integration.sodium;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;

public class SodiumHooks {
    public static int modifyLightForEmissiveItems(
        BakedQuad quad,
        int light
    ){
        return quad.isShade() ? LightTexture.FULL_BRIGHT : light;
    }
}

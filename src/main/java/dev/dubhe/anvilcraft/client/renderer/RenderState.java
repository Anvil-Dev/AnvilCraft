package dev.dubhe.anvilcraft.client.renderer;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import lombok.Getter;
import net.minecraft.client.Minecraft;

public class RenderState {
    private static final boolean IRIS_PRESENT;
    @Getter
    private static boolean bloomRenderStage;

    static {
        IRIS_PRESENT = Util.isLoaded("iris") || Util.isLoaded("oculus");
    }

    public static boolean isIrisPresent() {
        return IRIS_PRESENT;
    }

    public static void bloomStage() {
        bloomRenderStage = true;
    }

    public static void levelStage() {
        bloomRenderStage = false;
    }

    public static boolean isEnhancedRenderingAvailable() {
        return !Minecraft.useShaderTransparency() && !IrisState.isShaderEnabled();
    }

    public static boolean isBloomEffectEnabled() {
        return AnvilCraftClient.CONFIG.renderBloomEffect;
    }
}

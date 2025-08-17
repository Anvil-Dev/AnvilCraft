package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import dev.dubhe.anvilcraft.util.Util;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public class RenderState {
    private static boolean IRIS_PRESENT;
    @Getter
    private static boolean bloomRenderStage;
    private static final Logger logger = LogUtils.getLogger();

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
        return AnvilCraft.config.renderBloomEffect;
    }
}

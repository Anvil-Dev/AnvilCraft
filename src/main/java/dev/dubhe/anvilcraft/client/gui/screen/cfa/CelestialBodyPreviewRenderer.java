package dev.dubhe.anvilcraft.client.gui.screen.cfa;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CFARenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

/**
 * 锻星砧界面中的天体与巨构预览提交工具。
 */
public final class CelestialBodyPreviewRenderer {
    private static final float RING1_SCALE_DIVISOR = 1.00f;
    private static final float RING2_SCALE_DIVISOR = 1.25f;
    private static final float RING4_SCALE_DIVISOR = 1.60f;
    private static final float RING5_SCALE_DIVISOR = 1.85f;
    private static final float RING6_SCALE_DIVISOR = 2.10f;

    private CelestialBodyPreviewRenderer() {
    }

    /**
     * 在给定的绝对屏幕区域中提交天体预览。
     */
    public static void render(
        GuiGraphicsExtractor graphics,
        CelestialBodyData body,
        int animationTick,
        long seed,
        int x,
        int y,
        int width,
        int height
    ) {
        float scale = body instanceof SpecialCelestialBodyData special && special.isPlayerHead()
            ? width * 0.45f
            : (Math.min(width, height) - 16) / 2.0f;
        graphics.submitPictureInPictureRenderState(CfaPreviewPipRenderer.State.body(
            graphics,
            body,
            animationTick,
            seed,
            x,
            y,
            width,
            height,
            scale
        ));
    }

    /**
     * 在重构按钮内提交巨构模型预览。
     */
    public static void renderMegastructure(
        GuiGraphicsExtractor graphics,
        CelestialRefactorOption option,
        int animationTick,
        int x,
        int y,
        int width,
        int height
    ) {
        StandaloneModelKey<BlockStateModel> model = CelestialBodyPreviewRenderer.resolveMegastructureModel(option);
        float divisor = switch (option.ring()) {
            case 1 -> CelestialBodyPreviewRenderer.RING1_SCALE_DIVISOR;
            case 2 -> CelestialBodyPreviewRenderer.RING2_SCALE_DIVISOR;
            case 4 -> CelestialBodyPreviewRenderer.RING4_SCALE_DIVISOR;
            case 5 -> CelestialBodyPreviewRenderer.RING5_SCALE_DIVISOR;
            case 6 -> CelestialBodyPreviewRenderer.RING6_SCALE_DIVISOR;
            default -> 1.0f;
        };
        float scale = Math.min(width, height) * 1.15f / divisor;
        graphics.submitPictureInPictureRenderState(CfaPreviewPipRenderer.State.model(
            graphics,
            model,
            option.rotation(animationTick * 2.0f % 360.0f, animationTick),
            option.id().hashCode(),
            x,
            y,
            width,
            height,
            scale
        ));
    }

    /**
     * 读取玩家头颅天体要显示的玩家名。
     */
    public static @Nullable String playerName(SpecialCelestialBodyData special) {
        if (special.playerHeadProfile() == null) return null;
        ResolvableProfile profile = ResolvableProfile.CODEC
            .parse(NbtOps.INSTANCE, special.playerHeadProfile())
            .result()
            .orElse(null);
        if (profile == null) return null;
        return profile.name().orElse(profile.partialProfile().id().toString());
    }

    static StandaloneModelKey<BlockStateModel> resolveSpecialModel(SpecialCelestialBodyData special) {
        return CFARenderer.getSpecialBodyModel(special.getModelLocation());
    }

    private static StandaloneModelKey<BlockStateModel> resolveMegastructureModel(CelestialRefactorOption option) {
        return CFARenderer.getMegastructureModel(option.modelLocation());
    }
}

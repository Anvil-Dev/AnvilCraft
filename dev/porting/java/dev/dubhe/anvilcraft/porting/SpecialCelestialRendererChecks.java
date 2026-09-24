package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.PlanetAtmosphereRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CFARenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class SpecialCelestialRendererChecks {
    public static void verify(CelestialForgingAnvilBlockEntity be) {
        var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher()
            .<CelestialForgingAnvilBlockEntity, CFARenderState>getRenderer(be);
        if (renderer == null) throw new IllegalStateException("Missing CFA renderer");
        var state = renderer.createRenderState();
        renderer.extractRenderState(be, state, 0, Vec3.ZERO, null);
        var body = (SpecialCelestialBodyData) state.getEffectiveBodyData();
        if (!state.isCanRenderBody() || (body.needsCustomModel() ? state.getComplexBodyModel() == null : state.getBodyTexture() == null)) {
            throw new IllegalStateException("Special body did not resolve its model or texture");
        }
        if (body.atmosphereColor() != null) {
            float[] rgb = CelestialBodyRenderer.getAtmosphereColor(body.atmosphereColor());
            AnvilCraft.LOGGER.info("PORT_SPECIAL_WORLD_COLOR: {}, {}, {}", rgb[0], rgb[1], rgb[2]);
            try {
                var checked = PlanetAtmosphereRenderer.class.getDeclaredField("checked");
                var failed = PlanetAtmosphereRenderer.class.getDeclaredField("failed");
                checked.setAccessible(true);
                failed.setAccessible(true);
                if (!checked.getBoolean(null) || failed.getBoolean(null)) {
                    throw new IllegalStateException("Custom atmosphere did not reach the native volume pipeline");
                }
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }
        AnvilCraft.LOGGER.info("PORT_SPECIAL_WORLD_RENDER_STATE_PASSED");
    }
}

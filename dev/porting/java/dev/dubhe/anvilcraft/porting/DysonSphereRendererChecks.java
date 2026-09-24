package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CFARenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class DysonSphereRendererChecks {
    public static void verify(CelestialForgingAnvilBlockEntity be, boolean brown, boolean special, boolean amplifier, boolean sphere) {
        var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher()
            .<CelestialForgingAnvilBlockEntity, CFARenderState>getRenderer(be);
        if (renderer == null) throw new IllegalStateException("Missing CFA renderer");
        var state = renderer.createRenderState();
        renderer.extractRenderState(be, state, 0, Vec3.ZERO, null);
        if (state.isCanRenderBody() != (brown || amplifier)) {
            throw new IllegalStateException("World body visibility must retain the source amplifier requirement");
        }
        if (brown && sphere) {
            if (!state.isBrownDwarfDysonSphere() || state.isHasMiddleRing() || state.isHasOuterRing()
                || state.getMiddleRingModel() == null || state.getOuterRingModel() == null) {
                throw new IllegalStateException("Brown sphere must replace both mechanical rings with synchronized models");
            }
        } else if (!brown && !sphere) {
            if (state.isInnerVisibleNow() != special || state.isOuterVisibleNow() == special) {
                throw new IllegalStateException("Special red dwarf must use the small stellar ring configuration");
            }
        }
        if (special && sphere && (!state.isDysonSphereR4() || state.getR4DysonModel() == null)) {
            throw new IllegalStateException("Special red dwarf must select ring-four Dyson geometry");
        }
        AnvilCraft.LOGGER.info("PORT_DYSON_RENDER_STATE_PASSED: brown={}, special={}, amplifier={}, sphere={}",
            brown, special, amplifier, sphere);
    }
}

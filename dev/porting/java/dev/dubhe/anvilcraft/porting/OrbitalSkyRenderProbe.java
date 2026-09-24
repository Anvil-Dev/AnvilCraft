package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.renderer.OverworldLikeOrbitalSkyRenderer;
import dev.dubhe.anvilcraft.client.support.OverworldLikeClientState;
import dev.dubhe.anvilcraft.saved.OverworldLikeWorldState;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeOrbitMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

/** Verifies the live extraction path, including hidden frames and generation-global geometry. */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class OrbitalSkyRenderProbe {
    private static int checked;
    private static boolean lightingChecked;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void verify(ExtractLevelRenderStateEvent event) throws ReflectiveOperationException {
        if (!Boolean.getBoolean("anvilcraft.portOrbitalSkyScene") || !OverworldLikeClientState.isInitialized()) return;
        if (!CelestialTravelManager.isOverworldLike(event.getLevel().dimension())) return;
        var field = OverworldLikeOrbitalSkyRenderer.class.getDeclaredField("FRAME");
        field.setAccessible(true);
        var frame = event.getRenderState().getRenderData((ContextKey<?>) field.get(null));
        boolean visible = AnvilCraftClient.CONFIG.renderOverworldLikeSky
            && OverworldLikeClientState.phase() != OverworldLikeWorldState.Phase.RESET_PENDING;
        if ((frame != null) != visible) throw new IllegalStateException("Orbital visibility state leaked");
        float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(Minecraft.getInstance().isPaused());
        float multiplier = OverworldLikeClientState.environmentColorMultiplier(event.getLevel());
        if (!lightingChecked && multiplier < 0.5F) {
            var client = Minecraft.getInstance();
            var extractor = new LightmapRenderStateExtractor(client.gameRenderer, client);
            var light = new LightmapRenderState();
            extractor.tick();
            extractor.extract(light, partial);
            float expected = event.getCamera().attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partial)
                * multiplier;
            if (!light.needsUpdate || Math.abs(light.skyFactor - expected) > 0.00001) {
                throw new IllegalStateException("Eclipse did not modify native sky lighting");
            }
            extractor.extract(light, partial);
            if (light.needsUpdate || Math.abs(light.skyFactor - expected) > 0.00001) {
                throw new IllegalStateException("Unchanged lightmap compounded eclipse darkness");
            }
            lightingChecked = true;
            AnvilCraft.LOGGER.info("PORT_ORBITAL_LIGHTMAP_PASSED: multiplier={}", multiplier);
        }
        int sky = event.getCamera().attributeProbe().getValue(EnvironmentAttributes.SKY_COLOR, partial);
        int cloud = event.getCamera().attributeProbe().getValue(EnvironmentAttributes.CLOUD_COLOR, partial);
        if (event.getRenderState().skyRenderState.skyColor != ARGB.scaleRGB(sky, multiplier)
            || event.getRenderState().cloudColor != ARGB.scaleRGB(cloud, multiplier)) {
            throw new IllegalStateException("Eclipse sky/cloud color not extracted");
        }
        if (frame != null) {
            var pose = OverworldLikeOrbitMath.ringPose(4, event.getLevel().getGameTime(), partial,
                OverworldLikeClientState.orbitEpochGameTime(), OverworldLikeClientState.visualSeed());
            double[] expected = {pose.outerRotation(), pose.middleRotation(), pose.innerRotation()};
            String[] names = {"outer", "middle", "inner"};
            for (int i = 0; i < names.length; i++) {
                var accessor = frame.getClass().getDeclaredMethod(names[i]);
                accessor.setAccessible(true);
                if (Math.abs((float) accessor.invoke(frame) - expected[i]) > 0.0001) {
                    throw new IllegalStateException("Orbital geometry changed: " + names[i]);
                }
            }
        }
        if (++checked == 100) AnvilCraft.LOGGER.info("PORT_ORBITAL_EXTRACTION_PASSED: 100 frames");
    }
}

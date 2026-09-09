package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.MunDimensionEffects;
import dev.dubhe.anvilcraft.client.renderer.MunSkyRenderer;
import dev.dubhe.anvilcraft.client.renderer.MunSurfaceRenderer;
import dev.dubhe.anvilcraft.client.support.MunClientSky;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.io.IOException;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunSkyEventListener {
    private MunSkyEventListener() {
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(AnvilCraft.of("mun"), new MunDimensionEffects());
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        MunSkyRenderer.registerShaders(event);
        MunSurfaceRenderer.registerShaders(event);
    }

    @SubscribeEvent
    public static void renderTerrainGlare(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) MunSurfaceRenderer.prepareShadows();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) MunSurfaceRenderer.renderGlare();
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (!MunClientSky.isMun() || event.getCamera().getFluidInCamera() != FogType.NONE) return;
        event.setRed(0);
        event.setGreen(0);
        event.setBlue(0);
    }

    @SubscribeEvent
    public static void fogDistance(ViewportEvent.RenderFog event) {
        if (!MunClientSky.isMun() || event.getType() != FogType.NONE) return;
        if (event.getCamera().getEntity() instanceof LivingEntity living
            && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) return;
        // 只在视距边缘融入黑色，避免近处地形出现大气雾。
        float distance = Minecraft.getInstance().gameRenderer.getRenderDistance();
        event.setNearPlaneDistance(distance * 0.95F);
        event.setFarPlaneDistance(distance);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!MunClientSky.isMun()) {
            MunSurfaceRenderer.clear();
            MunClientSky.clear();
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        MunSurfaceRenderer.clear();
        MunClientSky.clear();
    }
}

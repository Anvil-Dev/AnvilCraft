package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.WipBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.WipBlockRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import java.util.HashSet;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class WipLightingProbe {
    private static boolean day;
    private static boolean night;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portWipLightingScene") || night) return;
        var client = Minecraft.getInstance();
        var pos = new BlockPos(-4, 162, 0);
        if (client.level == null || !(client.level.getBlockEntity(pos) instanceof WipBlockEntity wip)) return;
        if (wip.getRecipeId() == null) return;
        var renderer = (WipBlockEntityRenderer) client.getBlockEntityRenderDispatcher()
            .<WipBlockEntity, WipBlockRenderState>getRenderer(wip);
        var state = renderer.createRenderState();
        renderer.extractRenderState(wip, state, 0, client.player.position(), null);
        var colors = new HashSet<Integer>();
        var lights = new HashSet<Integer>();
        state.getLayers().values().forEach(quads -> quads.forEach(quad -> {
            colors.add(quad.lighting().getColor(0));
            lights.add(quad.lighting().getLightCoords(0));
        }));
        if (colors.size() < 3) throw new IllegalStateException("WIP face shading was flattened: " + colors);
        if (!day) {
            var snapshot = state.getLayers().values().stream().flatMap(java.util.Collection::stream).toList();
            int color = snapshot.getFirst().lighting().getColor(0);
            state.clearQuads();
            if (snapshot.getFirst().lighting().getColor(0) != color) throw new IllegalStateException("Mutable quad snapshot");
            var fallback = new WipBlockEntity(wip.getType(), pos, wip.getBlockState());
            fallback.setLevel(client.level);
            fallback.setInitialBlock(Blocks.GRASS_BLOCK.defaultBlockState());
            renderer.extractRenderState(fallback, state, 0, client.player.position(), null);
            boolean tinted = state.getLayers().values().stream().flatMap(java.util.Collection::stream).anyMatch(quad -> {
                int tint = quad.lighting().getColor(0);
                return quad.quad().materialInfo().tintIndex() >= 0 && ARGB.green(tint) > ARGB.blue(tint) + 20;
            });
            if (!tinted) throw new IllegalStateException("Fallback initial block lost biome tint");
            if (snapshot.getFirst().lighting().getColor(0) != color) throw new IllegalStateException("Reused mutable quad lighting");
            fallback.setInitialBlock(Blocks.GLASS.defaultBlockState());
            renderer.extractRenderState(fallback, state, 0, client.player.position(), null);
            if (state.getLayers().getOrDefault(ChunkSectionLayer.TRANSLUCENT, java.util.List.of()).isEmpty()) {
                throw new IllegalStateException("Glass fallback lost translucent render layer");
            }
            fallback.setInitialBlock(Blocks.AIR.defaultBlockState());
            renderer.extractRenderState(fallback, state, 0, client.player.position(), null);
            if (state.getLayers().values().stream().anyMatch(quads -> !quads.isEmpty())) {
                throw new IllegalStateException("Empty fallback retained old geometry");
            }
            day = true;
            AnvilCraft.LOGGER.info("PORT_WIP_LIGHTING_DAY_PASSED: {} face colors, detached quad data, "
                + "biome tint, translucent layer and empty fallback", colors.size());
        }
        if (client.level.getBlockState(pos.east()).is(Blocks.TORCH) && lights.size() >= 2) {
            night = true;
            AnvilCraft.LOGGER.info("PORT_WIP_LIGHTING_NIGHT_PASSED: {} independent face light coordinates", lights.size());
        }
    }

    @SubscribeEvent
    public static void shutdown(GameShuttingDownEvent event) {
        if (Boolean.getBoolean("anvilcraft.portWipLightingScene") && (!day || !night)) {
            throw new IllegalStateException("WIP lighting probe incomplete: day=" + day + " night=" + night);
        }
    }
}

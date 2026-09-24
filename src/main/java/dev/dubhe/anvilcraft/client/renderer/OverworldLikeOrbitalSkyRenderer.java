package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CFARenderer;
import dev.dubhe.anvilcraft.client.support.OverworldLikeClientState;
import dev.dubhe.anvilcraft.saved.OverworldLikeWorldState;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeOrbitMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Generation-global orbital models drawn immediately after the native sky. */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class OverworldLikeOrbitalSkyRenderer {
    private static final ContextKey<Frame> FRAME = new ContextKey<>(AnvilCraft.of("orbital_sky"));
    private static @Nullable BlockStateModel cachedInner;
    private static @Nullable BlockStateModel cachedMiddle;
    private static List<BakedQuad> innerQuads = List.of();
    private static List<BakedQuad> middleQuads = List.of();

    private OverworldLikeOrbitalSkyRenderer() {
    }

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        var level = event.getLevel();
        boolean overworldLike = CelestialTravelManager.isOverworldLike(level.dimension());
        if (overworldLike) {
            float multiplier = OverworldLikeClientState.environmentColorMultiplier(level);
            var state = event.getRenderState();
            state.skyRenderState.skyColor = ARGB.scaleRGB(state.skyRenderState.skyColor, multiplier);
            state.cloudColor = ARGB.scaleRGB(state.cloudColor, multiplier);
        }
        if (!AnvilCraftClient.CONFIG.renderOverworldLikeSky) return;
        if (!overworldLike && !CelestialTravelManager.VOID_PLANET_LEVEL.equals(level.dimension())) return;
        if (overworldLike && (!OverworldLikeClientState.isInitialized()
            || OverworldLikeClientState.phase() == OverworldLikeWorldState.Phase.RESET_PENDING)) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var models = minecraft.getModelManager();
        var inner = models.getStandaloneModel(CFARenderer.RING4);
        var middle = models.getStandaloneModel(CFARenderer.RING5);
        if (inner == null || middle == null) return;
        if (inner != cachedInner || middle != cachedMiddle) {
            innerQuads = quads(inner);
            middleQuads = quads(middle);
            cachedInner = inner;
            cachedMiddle = middle;
        }
        long time = level.getGameTime();
        float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(minecraft.isPaused());
        long epoch = overworldLike ? OverworldLikeClientState.orbitEpochGameTime() : 0;
        long seed = overworldLike ? OverworldLikeClientState.visualSeed() : 0;
        float eclipse = overworldLike ? OverworldLikeClientState.eclipseFactor(level) : 0;
        float collapse = overworldLike ? OverworldLikeClientState.collapseProgress() : 0;
        event.getRenderState().setRenderData(FRAME, new Frame(
            (float) OverworldLikeOrbitMath.ringPose(6, time, partialTick, epoch, seed).outerRotation(),
            (float) OverworldLikeOrbitMath.ringPose(5, time, partialTick, epoch, seed).middleRotation(),
            (float) OverworldLikeOrbitMath.ringPose(4, time, partialTick, epoch, seed).innerRotation(),
            0.34F + eclipse * 0.22F + collapse * 0.44F, innerQuads, middleQuads));
    }

    private static List<BakedQuad> quads(BlockStateModel model) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(),
            RandomSource.create(42), parts);
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) quads.addAll(part.getQuads(direction));
            quads.addAll(part.getQuads(null));
        }
        return List.copyOf(quads);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent.AfterSky event) {
        Frame frame = event.getLevelRenderState().getRenderData(FRAME);
        if (frame == null) return;
        var type = ModRenderTypes.OVERWORLD_LIKE_SKY_RING;
        final BufferBuilder vertices = Tesselator.getInstance().begin(type.mode(), type.format());
        PoseStack pose = new PoseStack();
        // Native LevelRenderer already includes the camera rotation in its model-view uniform.
        pose.scale(1200, 1200, 1200);
        pose.mulPose(Axis.YP.rotationDegrees(-frame.outer));
        pose.mulPose(Axis.XP.rotationDegrees(14.5108F));
        pose.mulPose(Axis.YP.rotationDegrees(-3.8411F));
        pose.mulPose(Axis.ZP.rotationDegrees(14.5109F));
        pose.mulPose(Axis.XP.rotationDegrees(90 + frame.middle));
        renderModel(pose, vertices, frame.middleQuads, frame.brightness * 0.92F);
        pose.mulPose(Axis.ZP.rotationDegrees(frame.inner));
        renderModel(pose, vertices, frame.innerQuads, frame.brightness * 0.86F);
        var mesh = vertices.build();
        if (mesh != null) type.draw(mesh);
    }

    private static void renderModel(PoseStack pose, BufferBuilder vertices, List<BakedQuad> quads, float brightness) {
        QuadInstance instance = new QuadInstance();
        int tint = ARGB.colorFromFloat(1, brightness, brightness, brightness);
        for (BakedQuad quad : quads) {
            instance.setColor(quad.materialInfo().tintIndex() != -1 ? tint : -1);
            vertices.putBakedQuad(pose.last(), quad, instance);
        }
    }

    private record Frame(float outer, float middle, float inner, float brightness,
                         List<BakedQuad> innerQuads, List<BakedQuad> middleQuads) {
    }
}

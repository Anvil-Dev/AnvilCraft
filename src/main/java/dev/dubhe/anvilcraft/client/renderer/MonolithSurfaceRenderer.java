package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import dev.dubhe.anvilcraft.client.support.GatewayGuiProjection;
import dev.dubhe.anvilcraft.util.MonolithBlockPositions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MonolithSurfaceRenderer {
    private static final Identifier INNER = AnvilCraft.of("block/monolith_inner");
    private static final ContextKey<List<Surface>> SURFACES = new ContextKey<>(AnvilCraft.of("monolith_surfaces"));
    private static final Map<BlockStateModel, List<Face>> FACES = new IdentityHashMap<>();
    private static final float SURFACE_OFFSET = 0.001F;
    private static final RenderType SURFACE = RenderType.create("anvilcraft_monolith_surface",
        RenderSetup.builder(ModRenderPipelines.MONOLITH_SURFACE)
            .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS).createRenderSetup());

    private MonolithSurfaceRenderer() {
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        FACES.clear();
    }

    public static List<Face> faces(BlockState state) {
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        return FACES.computeIfAbsent(model, key -> {
            List<BlockStateModelPart> parts = new ArrayList<>();
            key.collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, state, RandomSource.create(42), parts);
            List<Face> faces = new ArrayList<>();
            for (var part : parts) {
                addFaces(faces, part.getQuads(null), null);
                for (Direction direction : Direction.values()) addFaces(faces, part.getQuads(direction), direction);
            }
            return List.copyOf(faces);
        });
    }

    private static void addFaces(List<Face> faces, List<BakedQuad> quads, @Nullable Direction cullFace) {
        for (BakedQuad quad : quads) {
            if (!quad.materialInfo().sprite().contents().name().equals(INNER)) continue;
            Vector3f normal = new Vector3f(quad.position1()).sub(quad.position0())
                .cross(new Vector3f(quad.position2()).sub(quad.position0())).normalize().mul(SURFACE_OFFSET);
            List<Vector3fc> vertices = new ArrayList<>(4);
            for (int index = 0; index < 4; index++) vertices.add(new Vector3f(quad.position(index)).add(normal));
            faces.add(new Face(cullFace, List.copyOf(vertices), quad.materialInfo().sprite()));
        }
    }

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        var level = event.getLevel();
        var camera = event.getRenderState().cameraRenderState.pos;
        double distance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0;
        List<Surface> surfaces = new ArrayList<>();
        for (var positions : MonolithBlockPositions.chunks()) {
            for (BlockPos pos : positions) {
                if (pos.distToCenterSqr(camera) > distance * distance
                    || !event.getFrustum().isVisible(new AABB(pos).inflate(1))) continue;
                BlockState state = level.getBlockState(pos);
                if (!MonolithBlockPositions.isMonolith(state)) continue;
                List<Face> visible = new ArrayList<>();
                for (Face face : faces(state)) {
                    Direction side = face.cullFace();
                    if (side == null || Block.shouldRenderFace(level, pos, state, level.getBlockState(pos.relative(side)), side)) {
                        visible.add(face);
                    }
                }
                if (!visible.isEmpty()) surfaces.add(new Surface(pos, List.copyOf(visible)));
            }
        }
        event.getRenderState().setRenderData(SURFACES, List.copyOf(surfaces));
    }

    @SubscribeEvent
    public static void submit(SubmitCustomGeometryEvent event) {
        var surfaces = event.getLevelRenderState().getRenderData(SURFACES);
        if (surfaces == null || surfaces.isEmpty()) return;
        var camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        for (Surface surface : surfaces) {
            BlockPos pos = surface.pos();
            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            submitFaces(pose, event.getSubmitNodeCollector(), surface.faces());
            pose.popPose();
        }
    }

    public static void submitFaces(PoseStack pose, SubmitNodeCollector collector, List<Face> faces) {
        if (faces.isEmpty()) return;
        collector.submitCustomGeometry(pose, renderType(), (renderPose, consumer) -> {
            for (Face face : faces) {
                var sprite = face.sprite();
                for (Vector3fc vertex : face.vertices()) {
                    consumer.addVertex(renderPose, vertex).setUv(sprite.getU0(), sprite.getV0())
                        .setUv2(sprite.contents().width(), sprite.contents().height());
                }
            }
        });
    }

    private static RenderType renderType() {
        var projection = GatewayGuiProjection.current();
        if (projection == null) return SURFACE;
        return RenderType.create("anvilcraft_monolith_surface_gui",
            RenderSetup.builder(ModRenderPipelines.MONOLITH_SURFACE_GUI)
                .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                .setTextureTransform(new TextureTransform("anvilcraft_monolith_gui", () -> projection))
                .createRenderSetup());
    }

    public record Face(@Nullable Direction cullFace, List<Vector3fc> vertices, TextureAtlasSprite sprite) {
    }

    private record Surface(BlockPos pos, List<Face> faces) {
    }
}

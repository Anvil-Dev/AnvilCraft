package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionBlacklist;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.TransformingVertexPipeline;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

public class RuinsBlockEntityRenderer implements BlockEntityRenderer<RuinsBlockEntity>, ModelSelectionRenderer<RuinsBlockEntity> {
    private final BlockRenderDispatcher blocks;
    private final BlockEntityRenderDispatcher entities;
    private final Cache<BakedModel, AABB> modelBounds = CacheBuilder.newBuilder().weakKeys().maximumSize(512).build();
    private final Map<BlockEntity, Long> lastVisualTick = new WeakHashMap<>();

    public RuinsBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.getBlockRenderDispatcher();
        this.entities = context.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(RuinsBlockEntity ruins, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = ruins.getLevel();
        if (level == null) return;
        try (RuinsRenderContext ignored = RuinsRenderContext.enter(level)) {
            BlockState state = RuinsBlock.connectedDisplayState(level, ruins.getBlockPos(), ruins.getDisplayState());
            BlockEntity display = this.prepareDisplay(ruins);
            if (!state.getFluidState().isEmpty()) {
                BlockPos pos = ruins.getBlockPos();
                Transformation transform = new Transformation(new Matrix4f(pose.last().pose())
                    .translate(-(pos.getX() & 15), -(pos.getY() & 15), -(pos.getZ() & 15)));
                this.blocks.renderLiquid(pos, level,
                    new TransformingVertexPipeline(buffers.getBuffer(net.minecraft.client.renderer.ItemBlockRenderTypes
                        .getRenderLayer(state.getFluidState())), transform),
                    state, state.getFluidState());
            }
            if (state.getRenderShape() == RenderShape.MODEL) {
                BakedModel model = this.blocks.getBlockModel(state);
                ModelData data = model.getModelData(level, ruins.getBlockPos(), state,
                    display == null ? ModelData.EMPTY : display.getModelData());
                long seed = state.getSeed(ruins.getBlockPos());
                RandomSource random = RandomSource.create(seed);
                // 方块顶点已经包含明暗，必须沿用方块着色器，避免再次叠加实体方向光。
                for (RenderType layer : model.getRenderTypes(state, random, data)) {
                    pose.pushPose();
                    this.blocks.getModelRenderer().tesselateBlock(level, model, state, ruins.getBlockPos(), pose,
                        buffers.getBuffer(layer),
                        true, random, seed, overlay, data, layer);
                    pose.popPose();
                }
            }
            if (display != null) {
                BlockEntityRenderer<BlockEntity> renderer = this.entities.getRenderer(display);
                if (renderer != null) renderer.render(display, partialTick, pose, buffers, light, overlay);
            }
        }
    }

    private static <T extends BlockEntity> void tickDisplay(Level level, BlockState state, BlockEntityType<T> type, BlockPos pos) {
        // 仅推进客户端显示状态（如信标光束、附魔台书本），不注册或运行服务端机器。
        if (!level.isClientSide) return;
        BlockEntityTicker<T> ticker = state.getTicker(level, type);
        T entity = type.getBlockEntity(level, pos);
        if (ticker != null && entity != null) ticker.tick(level, pos, state, entity);
    }

    @Nullable
    public BlockEntity prepareDisplay(RuinsBlockEntity ruins) {
        Level level = ruins.getLevel();
        BlockEntity display = ruins.getDisplayEntity();
        if (level == null || display == null) return display;
        Long last = this.lastVisualTick.get(display);
        if (last == null || last != level.getGameTime()) {
            this.lastVisualTick.put(display, level.getGameTime());
            if (display instanceof CelestialForgingAnvilBlockEntity anvil) anvil.tickRuinsVisuals();
            else tickDisplay(level, ruins.getDisplayState(), display.getType(), ruins.getBlockPos());
        }
        return display;
    }

    @Override
    public void collectSelectionParts(RuinsBlockEntity ruins, float partialTick, List<SelectionPart> output) {
        Level level = ruins.getLevel();
        if (level == null) return;
        try (RuinsRenderContext ignored = RuinsRenderContext.enter(level)) {
            BlockEntity display = this.prepareDisplay(ruins);
            if (display != null && !ModelSelectionBlacklist.excludesBlockEntity(display.getBlockState().getBlock())) {
                output.addAll(ModelBlockSelection.rendererParts(display, partialTick));
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(RuinsBlockEntity ruins) {
        Level level = ruins.getLevel();
        if (level == null) return new AABB(ruins.getBlockPos());
        try (RuinsRenderContext ignored = RuinsRenderContext.enter(level)) {
            BlockState state = ruins.getDisplayState();
            AABB bounds = new AABB(ruins.getBlockPos());
            if (state.getRenderShape() == RenderShape.MODEL) {
                bounds = bounds.minmax(this.staticBounds(state).move(ruins.getBlockPos()));
            }
            if (state.getBlock() instanceof AbstractMultiPartBlock<?> multipart) {
                var shape = multipart.getMultiPartShape(state);
                if (!shape.isEmpty()) bounds = bounds.minmax(shape.bounds().move(ruins.getBlockPos()));
            }
            BlockEntity display = ruins.getDisplayEntity();
            BlockEntityRenderer<BlockEntity> renderer = display == null ? null : this.entities.getRenderer(display);
            return renderer == null ? bounds : bounds.minmax(renderer.getRenderBoundingBox(display));
        }
    }

    private AABB staticBounds(BlockState state) {
        BakedModel model = this.blocks.getBlockModel(state);
        AABB cached = this.modelBounds.getIfPresent(model);
        if (cached != null) return cached;
        AABB bounds = new AABB(BlockPos.ZERO);
        for (int side = 0; side <= Direction.values().length; side++) {
            Direction face = side == Direction.values().length ? null : Direction.values()[side];
            for (BakedQuad quad : model.getQuads(state, face, RandomSource.create(0), ModelData.EMPTY, null)) {
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int offset = 0; offset < vertices.length; offset += stride) {
                    double x = Float.intBitsToFloat(vertices[offset]);
                    double y = Float.intBitsToFloat(vertices[offset + 1]);
                    double z = Float.intBitsToFloat(vertices[offset + 2]);
                    bounds = bounds.minmax(new AABB(x, y, z, x, y, z));
                }
            }
        }
        this.modelBounds.put(model, bounds);
        return bounds;
    }

    @Override
    public boolean shouldRenderOffScreen(RuinsBlockEntity ruins) {
        if (ruins.getDisplayState().getBlock() instanceof AbstractMultiPartBlock<?>) return true;
        BlockEntity display = ruins.getDisplayEntity();
        BlockEntityRenderer<BlockEntity> renderer = display == null ? null : this.entities.getRenderer(display);
        return renderer != null && renderer.shouldRenderOffScreen(display);
    }

    @Override
    public boolean shouldRender(RuinsBlockEntity ruins, Vec3 cameraPos) {
        AABB bounds = this.getRenderBoundingBox(ruins);
        double x = Math.clamp(cameraPos.x, bounds.minX, bounds.maxX) - cameraPos.x;
        double y = Math.clamp(cameraPos.y, bounds.minY, bounds.maxY) - cameraPos.y;
        double z = Math.clamp(cameraPos.z, bounds.minZ, bounds.maxZ) - cameraPos.z;
        BlockEntity display = ruins.getDisplayEntity();
        BlockEntityRenderer<BlockEntity> renderer = display == null ? null : this.entities.getRenderer(display);
        return x * x + y * y + z * z < this.getViewDistance() * this.getViewDistance()
            || renderer != null && renderer.shouldRender(display, cameraPos);
    }

    @Override
    public int getViewDistance() {
        return Math.max(64, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16);
    }
}

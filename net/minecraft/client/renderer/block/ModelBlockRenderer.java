package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelBlockRenderer {
    private static final int FACE_CUBIC = 0;
    private static final int FACE_PARTIAL = 1;
    static final Direction[] DIRECTIONS = Direction.values();
    private final BlockColors blockColors;
    private static final int CACHE_SIZE = 100;
    static final ThreadLocal<ModelBlockRenderer.Cache> CACHE = ThreadLocal.withInitial(ModelBlockRenderer.Cache::new);

    public ModelBlockRenderer(BlockColors blockColors) {
        this.blockColors = blockColors;
    }

    /**
     * @param checkSides if {@code true}, only renders each side if {@link
     *                   net.minecraft.world.level.block.Block#shouldRenderFace(
     *                   net.minecraft.world.level.block.state.BlockState,
     *                   net.minecraft.world.level.BlockGetter,
     *                   net.minecraft.core.BlockPos, net.minecraft.core.Direction,
     *                   net.minecraft.core.BlockPos)} returns {@code true}
     */
    @Deprecated //Forge: Model data and render type parameter
    public void tesselateBlock(
        BlockAndTintGetter level,
        BakedModel model,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        RandomSource random,
        long seed,
        int packedOverlay
    ) {
        tesselateBlock(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
    }
    /**
     * @param checkSides if {@code true}, only renders each side if {@link
     *                   net.minecraft.world.level.block.Block#shouldRenderFace(
     *                   net.minecraft.world.level.block.state.BlockState,
     *                   net.minecraft.world.level.BlockGetter,
     *                   net.minecraft.core.BlockPos, net.minecraft.core.Direction,
     *                   net.minecraft.core.BlockPos)} returns {@code true}
     */
    public void tesselateBlock(
        BlockAndTintGetter level,
        BakedModel model,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        RandomSource random,
        long seed,
        int packedOverlay,
        net.neoforged.neoforge.client.model.data.ModelData modelData,
        net.minecraft.client.renderer.RenderType renderType
    ) {
        boolean flag = Minecraft.useAmbientOcclusion() && switch(model.useAmbientOcclusion(state, modelData, renderType)) {
            case TRUE -> true;
            case DEFAULT -> state.getLightEmission(level, pos) == 0;
            case FALSE -> false;
        };
        Vec3 vec3 = state.getOffset(level, pos);
        poseStack.translate(vec3.x, vec3.y, vec3.z);

        try {
            if (flag) {
                this.tesselateWithAO(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, modelData, renderType);
            } else {
                this.tesselateWithoutAO(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, modelData, renderType);
            }
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block model");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block model being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, level, pos, state);
            crashreportcategory.setDetail("Using AO", flag);
            throw new ReportedException(crashreport);
        }
    }

    /**
     * @param checkSides if {@code true}, only renders each side if {@link
     *                   net.minecraft.world.level.block.Block#shouldRenderFace(
     *                   net.minecraft.world.level.block.state.BlockState,
     *                   net.minecraft.world.level.BlockGetter,
     *                   net.minecraft.core.BlockPos, net.minecraft.core.Direction,
     *                   net.minecraft.core.BlockPos)} returns {@code true}
     */
    @Deprecated //Forge: Model data and render type parameter
    public void tesselateWithAO(
        BlockAndTintGetter level,
        BakedModel model,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        RandomSource random,
        long seed,
        int packedOverlay
    ) {
         tesselateWithAO(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
    }
    /**
     * @param checkSides if {@code true}, only renders each side if {@link
     *                   net.minecraft.world.level.block.Block#shouldRenderFace(
     *                   net.minecraft.world.level.block.state.BlockState,
     *                   net.minecraft.world.level.BlockGetter,
     *                   net.minecraft.core.BlockPos, net.minecraft.core.Direction,
     *                   net.minecraft.core.BlockPos)} returns {@code true}
     */
    public void tesselateWithAO(
        BlockAndTintGetter level,
        BakedModel model,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        RandomSource random,
        long seed,
        int packedOverlay,
        net.neoforged.neoforge.client.model.data.ModelData modelData,
        net.minecraft.client.renderer.RenderType renderType
    ) {
        float[] afloat = new float[DIRECTIONS.length * 2];
        BitSet bitset = new BitSet(3);
        ModelBlockRenderer.AmbientOcclusionFace modelblockrenderer$ambientocclusionface = new ModelBlockRenderer.AmbientOcclusionFace();
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();

        for (Direction direction : DIRECTIONS) {
            random.setSeed(seed);
            List<BakedQuad> list = model.getQuads(state, direction, random, modelData, renderType);
            if (!list.isEmpty()) {
                blockpos$mutableblockpos.setWithOffset(pos, direction);
                if (!checkSides || Block.shouldRenderFace(state, level, pos, direction, blockpos$mutableblockpos)) {
                    this.renderModelFaceAO(
                        level, state, pos, poseStack, consumer, list, afloat, bitset, modelblockrenderer$ambientocclusionface, packedOverlay
                    );
                }
            }
        }

        random.setSeed(seed);
        List<BakedQuad> list1 = model.getQuads(state, (Direction)null, random, modelData, renderType);
        if (!list1.isEmpty()) {
            this.renderModelFaceAO(
                level, state, pos, poseStack, consumer, list1, afloat, bitset, modelblockrenderer$ambientocclusionface, packedOverlay
            );
        }
    }

    /**
     * @param checkSides if {@code true}, only renders each side if {@link
     *                   net.minecraft.world.level.block.Block#shouldRenderFace(
     *                   net.minecraft.world.level.block.state.BlockState,
     *                   net.minecraft.world.level.BlockGetter,
     *                   net.minecraft.core.BlockPos, net.minecraft.core.Direction,
     *                   net.minecraft.core.BlockPos)} returns {@code true}
     */
    @Deprecated //Forge: Model data and render type parameter
    public void tesselateWithoutAO(
        BlockAndTintGetter level,
        BakedModel model,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        RandomSource random,
        long seed,
        int packedOverlay
    ) {
        tesselateWithoutAO(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
    }
    /**
     * @param checkSides if {@code true}, only renders each side if {@link
     *                   net.minecraft.world.level.block.Block#shouldRenderFace(
     *                   net.minecraft.world.level.block.state.BlockState,
     *                   net.minecraft.world.level.BlockGetter,
     *                   net.minecraft.core.BlockPos, net.minecraft.core.Direction,
     *                   net.minecraft.core.BlockPos)} returns {@code true}
     */
    public void tesselateWithoutAO(
        BlockAndTintGetter level,
        BakedModel model,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        RandomSource random,
        long seed,
        int packedOverlay,
        net.neoforged.neoforge.client.model.data.ModelData modelData,
        net.minecraft.client.renderer.RenderType renderType
    ) {
        BitSet bitset = new BitSet(3);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();

        for (Direction direction : DIRECTIONS) {
            random.setSeed(seed);
            List<BakedQuad> list = model.getQuads(state, direction, random, modelData, renderType);
            if (!list.isEmpty()) {
                blockpos$mutableblockpos.setWithOffset(pos, direction);
                if (!checkSides || Block.shouldRenderFace(state, level, pos, direction, blockpos$mutableblockpos)) {
                    int i = LevelRenderer.getLightColor(level, state, blockpos$mutableblockpos);
                    this.renderModelFaceFlat(level, state, pos, i, packedOverlay, false, poseStack, consumer, list, bitset);
                }
            }
        }

        random.setSeed(seed);
        List<BakedQuad> list1 = model.getQuads(state, null, random, modelData, renderType);
        if (!list1.isEmpty()) {
            this.renderModelFaceFlat(level, state, pos, -1, packedOverlay, true, poseStack, consumer, list1, bitset);
        }
    }

    /**
     * @param shape      the array, of length 12, to store the shape bounds in
     * @param shapeFlags the bit set to store the shape flags in. The first bit will
     *                   be {@code true} if the face should be offset, and the second
     *                   if the face is less than a block in width and height.
     */
    private void renderModelFaceAO(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        List<BakedQuad> quads,
        float[] shape,
        BitSet shapeFlags,
        ModelBlockRenderer.AmbientOcclusionFace aoFace,
        int packedOverlay
    ) {
        for (BakedQuad bakedquad : quads) {
            this.calculateShape(level, state, pos, bakedquad.getVertices(), bakedquad.getDirection(), shape, shapeFlags);
            if (!net.neoforged.neoforge.client.ClientHooks.calculateFaceWithoutAO(level, state, pos, bakedquad, shapeFlags.get(0), aoFace.brightness, aoFace.lightmap))
            aoFace.calculate(level, state, pos, bakedquad.getDirection(), shape, shapeFlags, bakedquad.isShade());
            this.putQuadData(
                level,
                state,
                pos,
                consumer,
                poseStack.last(),
                bakedquad,
                aoFace.brightness[0],
                aoFace.brightness[1],
                aoFace.brightness[2],
                aoFace.brightness[3],
                aoFace.lightmap[0],
                aoFace.lightmap[1],
                aoFace.lightmap[2],
                aoFace.lightmap[3],
                packedOverlay
            );
        }
    }

    private void putQuadData(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        VertexConsumer consumer,
        PoseStack.Pose pose,
        BakedQuad quad,
        float brightness0,
        float brightness1,
        float brightness2,
        float brightness3,
        int lightmap0,
        int lightmap1,
        int lightmap2,
        int lightmap3,
        int packedOverlay
    ) {
        float f;
        float f1;
        float f2;
        if (quad.isTinted()) {
            int i = this.blockColors.getColor(state, level, pos, quad.getTintIndex());
            f = (float)(i >> 16 & 0xFF) / 255.0F;
            f1 = (float)(i >> 8 & 0xFF) / 255.0F;
            f2 = (float)(i & 0xFF) / 255.0F;
        } else {
            f = 1.0F;
            f1 = 1.0F;
            f2 = 1.0F;
        }

        consumer.putBulkData(
            pose,
            quad,
            new float[]{brightness0, brightness1, brightness2, brightness3},
            f,
            f1,
            f2,
            1.0F,
            new int[]{lightmap0, lightmap1, lightmap2, lightmap3},
            packedOverlay,
            true
        );
    }

    /**
     * Calculates the shape and corresponding flags for the specified {@code direction} and {@code vertices}, storing the resulting shape in the specified {@code shape} array and the shape flags in {@code shapeFlags}.
     *
     * @param shape      the array, of length 12, to store the shape bounds in, or {@
     *                   code null} to only calculate shape flags
     * @param shapeFlags the bit set to store the shape flags in. The first bit will
     *                   be {@code true} if the face should be offset, and the second
     *                   if the face is less than a block in width and height.
     */
    private void calculateShape(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        int[] vertices,
        Direction direction,
        @Nullable float[] shape,
        BitSet shapeFlags
    ) {
        float f = 32.0F;
        float f1 = 32.0F;
        float f2 = 32.0F;
        float f3 = -32.0F;
        float f4 = -32.0F;
        float f5 = -32.0F;

        for (int i = 0; i < 4; i++) {
            float f6 = Float.intBitsToFloat(vertices[i * 8]);
            float f7 = Float.intBitsToFloat(vertices[i * 8 + 1]);
            float f8 = Float.intBitsToFloat(vertices[i * 8 + 2]);
            f = Math.min(f, f6);
            f1 = Math.min(f1, f7);
            f2 = Math.min(f2, f8);
            f3 = Math.max(f3, f6);
            f4 = Math.max(f4, f7);
            f5 = Math.max(f5, f8);
        }

        if (shape != null) {
            shape[Direction.WEST.get3DDataValue()] = f;
            shape[Direction.EAST.get3DDataValue()] = f3;
            shape[Direction.DOWN.get3DDataValue()] = f1;
            shape[Direction.UP.get3DDataValue()] = f4;
            shape[Direction.NORTH.get3DDataValue()] = f2;
            shape[Direction.SOUTH.get3DDataValue()] = f5;
            int j = DIRECTIONS.length;
            shape[Direction.WEST.get3DDataValue() + j] = 1.0F - f;
            shape[Direction.EAST.get3DDataValue() + j] = 1.0F - f3;
            shape[Direction.DOWN.get3DDataValue() + j] = 1.0F - f1;
            shape[Direction.UP.get3DDataValue() + j] = 1.0F - f4;
            shape[Direction.NORTH.get3DDataValue() + j] = 1.0F - f2;
            shape[Direction.SOUTH.get3DDataValue() + j] = 1.0F - f5;
        }

        float f9 = 1.0E-4F;
        float f10 = 0.9999F;
        switch (direction) {
            case DOWN:
                shapeFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f1 == f4 && (f1 < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case UP:
                shapeFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f1 == f4 && (f4 > 0.9999F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case NORTH:
                shapeFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                shapeFlags.set(0, f2 == f5 && (f2 < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case SOUTH:
                shapeFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                shapeFlags.set(0, f2 == f5 && (f5 > 0.9999F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case WEST:
                shapeFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f == f3 && (f < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case EAST:
                shapeFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f == f3 && (f3 > 0.9999F || state.isCollisionShapeFullBlock(level, pos)));
        }
    }

    /**
     * @param repackLight {@code true} if packed light should be re-calculated
     * @param shapeFlags  the bit set to store the shape flags in. The first bit will
     *                    be {@code true} if the face should be offset, and the second
     *                    if the face is less than a block in width and height.
     */
    private void renderModelFaceFlat(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        int packedLight,
        int packedOverlay,
        boolean repackLight,
        PoseStack poseStack,
        VertexConsumer consumer,
        List<BakedQuad> quads,
        BitSet shapeFlags
    ) {
        for (BakedQuad bakedquad : quads) {
            if (repackLight) {
                this.calculateShape(level, state, pos, bakedquad.getVertices(), bakedquad.getDirection(), null, shapeFlags);
                BlockPos blockpos = shapeFlags.get(0) ? pos.relative(bakedquad.getDirection()) : pos;
                packedLight = LevelRenderer.getLightColor(level, state, blockpos);
            }

            float f = level.getShade(bakedquad.getDirection(), bakedquad.isShade());
            this.putQuadData(
                level, state, pos, consumer, poseStack.last(), bakedquad, f, f, f, f, packedLight, packedLight, packedLight, packedLight, packedOverlay
            );
        }
    }

    @Deprecated //Forge: Model data and render type parameter
    public void renderModel(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        @Nullable BlockState state,
        BakedModel model,
        float red,
        float green,
        float blue,
        int packedLight,
        int packedOverlay
    ) {
        renderModel(pose, consumer, state, model, red, green, blue, packedLight, packedOverlay, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
    }

    public void renderModel(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        @Nullable BlockState state,
        BakedModel model,
        float red,
        float green,
        float blue,
        int packedLight,
        int packedOverlay,
        net.neoforged.neoforge.client.model.data.ModelData modelData,
        net.minecraft.client.renderer.RenderType renderType
    ) {
        RandomSource randomsource = RandomSource.create();
        long i = 42L;

        for (Direction direction : DIRECTIONS) {
            randomsource.setSeed(42L);
            renderQuadList(pose, consumer, red, green, blue, model.getQuads(state, direction, randomsource, modelData, renderType), packedLight, packedOverlay);
        }

        randomsource.setSeed(42L);
        renderQuadList(pose, consumer, red, green, blue, model.getQuads(state, null, randomsource, modelData, renderType), packedLight, packedOverlay);
    }

    private static void renderQuadList(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float red,
        float green,
        float blue,
        List<BakedQuad> quads,
        int packedLight,
        int packedOverlay
    ) {
        for (BakedQuad bakedquad : quads) {
            float f;
            float f1;
            float f2;
            if (bakedquad.isTinted()) {
                f = Mth.clamp(red, 0.0F, 1.0F);
                f1 = Mth.clamp(green, 0.0F, 1.0F);
                f2 = Mth.clamp(blue, 0.0F, 1.0F);
            } else {
                f = 1.0F;
                f1 = 1.0F;
                f2 = 1.0F;
            }

            consumer.putBulkData(pose, bakedquad, f, f1, f2, 1.0F, packedLight, packedOverlay);
        }
    }

    public static void enableCaching() {
        CACHE.get().enable();
    }

    public static void clearCache() {
        CACHE.get().disable();
    }

    @OnlyIn(Dist.CLIENT)
    protected static enum AdjacencyInfo {
        DOWN(
            new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
            0.5F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        UP(
            new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
            1.0F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        NORTH(
            new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
            0.8F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST
            }
        ),
        SOUTH(
            new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
            0.8F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.EAST
            }
        ),
        WEST(
            new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH},
            0.6F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        EAST(
            new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH},
            0.6F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        );

        final Direction[] corners;
        final boolean doNonCubicWeight;
        final ModelBlockRenderer.SizeInfo[] vert0Weights;
        final ModelBlockRenderer.SizeInfo[] vert1Weights;
        final ModelBlockRenderer.SizeInfo[] vert2Weights;
        final ModelBlockRenderer.SizeInfo[] vert3Weights;
        private static final ModelBlockRenderer.AdjacencyInfo[] BY_FACING = Util.make(new ModelBlockRenderer.AdjacencyInfo[6], p_111134_ -> {
            p_111134_[Direction.DOWN.get3DDataValue()] = DOWN;
            p_111134_[Direction.UP.get3DDataValue()] = UP;
            p_111134_[Direction.NORTH.get3DDataValue()] = NORTH;
            p_111134_[Direction.SOUTH.get3DDataValue()] = SOUTH;
            p_111134_[Direction.WEST.get3DDataValue()] = WEST;
            p_111134_[Direction.EAST.get3DDataValue()] = EAST;
        });

        /**
         * @param shadeBrightness the shade brightness for this direction
         */
        private AdjacencyInfo(
            Direction[] corners,
            float shadeBrightness,
            boolean doNonCubicWeight,
            ModelBlockRenderer.SizeInfo[] vert0Weights,
            ModelBlockRenderer.SizeInfo[] vert1Weights,
            ModelBlockRenderer.SizeInfo[] vert2Weights,
            ModelBlockRenderer.SizeInfo[] vert3Weights
        ) {
            this.corners = corners;
            this.doNonCubicWeight = doNonCubicWeight;
            this.vert0Weights = vert0Weights;
            this.vert1Weights = vert1Weights;
            this.vert2Weights = vert2Weights;
            this.vert3Weights = vert3Weights;
        }

        public static ModelBlockRenderer.AdjacencyInfo fromFacing(Direction facing) {
            return BY_FACING[facing.get3DDataValue()];
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class AmbientOcclusionFace {
        final float[] brightness = new float[4];
        final int[] lightmap = new int[4];

        public AmbientOcclusionFace() {
        }

        /**
         * @param shape      the array, of length 12, containing the shape bounds
         * @param shapeFlags the bit set to store the shape flags in. The first bit will
         *                   be {@code true} if the face should be offset, and the second
         *                   if the face is less than a block in width and height.
         */
        public void calculate(
            BlockAndTintGetter level, BlockState state, BlockPos pos, Direction direction, float[] shape, BitSet shapeFlags, boolean shade
        ) {
            BlockPos blockpos = shapeFlags.get(0) ? pos.relative(direction) : pos;
            ModelBlockRenderer.AdjacencyInfo modelblockrenderer$adjacencyinfo = ModelBlockRenderer.AdjacencyInfo.fromFacing(direction);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            ModelBlockRenderer.Cache modelblockrenderer$cache = ModelBlockRenderer.CACHE.get();
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]);
            BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
            int i = modelblockrenderer$cache.getLightColor(blockstate, level, blockpos$mutableblockpos);
            float f = modelblockrenderer$cache.getShadeBrightness(blockstate, level, blockpos$mutableblockpos);
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]);
            BlockState blockstate1 = level.getBlockState(blockpos$mutableblockpos);
            int j = modelblockrenderer$cache.getLightColor(blockstate1, level, blockpos$mutableblockpos);
            float f1 = modelblockrenderer$cache.getShadeBrightness(blockstate1, level, blockpos$mutableblockpos);
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[2]);
            BlockState blockstate2 = level.getBlockState(blockpos$mutableblockpos);
            int k = modelblockrenderer$cache.getLightColor(blockstate2, level, blockpos$mutableblockpos);
            float f2 = modelblockrenderer$cache.getShadeBrightness(blockstate2, level, blockpos$mutableblockpos);
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[3]);
            BlockState blockstate3 = level.getBlockState(blockpos$mutableblockpos);
            int l = modelblockrenderer$cache.getLightColor(blockstate3, level, blockpos$mutableblockpos);
            float f3 = modelblockrenderer$cache.getShadeBrightness(blockstate3, level, blockpos$mutableblockpos);
            BlockState blockstate4 = level.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]) // Neo: remove move() to avoid oversampling (MC-43968)
            );
            boolean flag = !blockstate4.isViewBlocking(level, blockpos$mutableblockpos)
                || blockstate4.getLightBlock(level, blockpos$mutableblockpos) == 0;
            BlockState blockstate5 = level.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]) // Neo: remove move() to avoid oversampling (MC-43968)
            );
            boolean flag1 = !blockstate5.isViewBlocking(level, blockpos$mutableblockpos)
                || blockstate5.getLightBlock(level, blockpos$mutableblockpos) == 0;
            BlockState blockstate6 = level.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[2]) // Neo: remove move() to avoid oversampling (MC-43968)
            );
            boolean flag2 = !blockstate6.isViewBlocking(level, blockpos$mutableblockpos)
                || blockstate6.getLightBlock(level, blockpos$mutableblockpos) == 0;
            BlockState blockstate7 = level.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[3]) // Neo: remove move() to avoid oversampling (MC-43968)
            );
            boolean flag3 = !blockstate7.isViewBlocking(level, blockpos$mutableblockpos)
                || blockstate7.getLightBlock(level, blockpos$mutableblockpos) == 0;
            float f4;
            int i1;
            if (!flag2 && !flag) {
                f4 = f;
                i1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]).move(modelblockrenderer$adjacencyinfo.corners[2]);
                BlockState blockstate8 = level.getBlockState(blockpos$mutableblockpos);
                f4 = modelblockrenderer$cache.getShadeBrightness(blockstate8, level, blockpos$mutableblockpos);
                i1 = modelblockrenderer$cache.getLightColor(blockstate8, level, blockpos$mutableblockpos);
            }

            float f5;
            int j1;
            if (!flag3 && !flag) {
                f5 = f;
                j1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]).move(modelblockrenderer$adjacencyinfo.corners[3]);
                BlockState blockstate10 = level.getBlockState(blockpos$mutableblockpos);
                f5 = modelblockrenderer$cache.getShadeBrightness(blockstate10, level, blockpos$mutableblockpos);
                j1 = modelblockrenderer$cache.getLightColor(blockstate10, level, blockpos$mutableblockpos);
            }

            float f6;
            int k1;
            if (!flag2 && !flag1) {
                f6 = f;
                k1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]).move(modelblockrenderer$adjacencyinfo.corners[2]);
                BlockState blockstate11 = level.getBlockState(blockpos$mutableblockpos);
                f6 = modelblockrenderer$cache.getShadeBrightness(blockstate11, level, blockpos$mutableblockpos);
                k1 = modelblockrenderer$cache.getLightColor(blockstate11, level, blockpos$mutableblockpos);
            }

            float f7;
            int l1;
            if (!flag3 && !flag1) {
                f7 = f;
                l1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]).move(modelblockrenderer$adjacencyinfo.corners[3]);
                BlockState blockstate12 = level.getBlockState(blockpos$mutableblockpos);
                f7 = modelblockrenderer$cache.getShadeBrightness(blockstate12, level, blockpos$mutableblockpos);
                l1 = modelblockrenderer$cache.getLightColor(blockstate12, level, blockpos$mutableblockpos);
            }

            int i3 = modelblockrenderer$cache.getLightColor(state, level, pos);
            blockpos$mutableblockpos.setWithOffset(pos, direction);
            BlockState blockstate9 = level.getBlockState(blockpos$mutableblockpos);
            if (shapeFlags.get(0) || !blockstate9.isSolidRender(level, blockpos$mutableblockpos)) {
                i3 = modelblockrenderer$cache.getLightColor(blockstate9, level, blockpos$mutableblockpos);
            }

            float f8 = shapeFlags.get(0)
                ? modelblockrenderer$cache.getShadeBrightness(level.getBlockState(blockpos), level, blockpos)
                : modelblockrenderer$cache.getShadeBrightness(level.getBlockState(pos), level, pos);
            ModelBlockRenderer.AmbientVertexRemap modelblockrenderer$ambientvertexremap = ModelBlockRenderer.AmbientVertexRemap.fromFacing(direction);
            if (shapeFlags.get(1) && modelblockrenderer$adjacencyinfo.doNonCubicWeight) {
                float f29 = (f3 + f + f5 + f8) * 0.25F;
                float f31 = (f2 + f + f4 + f8) * 0.25F;
                float f32 = (f2 + f1 + f6 + f8) * 0.25F;
                float f33 = (f3 + f1 + f7 + f8) * 0.25F;
                float f13 = shape[modelblockrenderer$adjacencyinfo.vert0Weights[0].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert0Weights[1].shape];
                float f14 = shape[modelblockrenderer$adjacencyinfo.vert0Weights[2].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert0Weights[3].shape];
                float f15 = shape[modelblockrenderer$adjacencyinfo.vert0Weights[4].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert0Weights[5].shape];
                float f16 = shape[modelblockrenderer$adjacencyinfo.vert0Weights[6].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert0Weights[7].shape];
                float f17 = shape[modelblockrenderer$adjacencyinfo.vert1Weights[0].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert1Weights[1].shape];
                float f18 = shape[modelblockrenderer$adjacencyinfo.vert1Weights[2].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert1Weights[3].shape];
                float f19 = shape[modelblockrenderer$adjacencyinfo.vert1Weights[4].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert1Weights[5].shape];
                float f20 = shape[modelblockrenderer$adjacencyinfo.vert1Weights[6].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert1Weights[7].shape];
                float f21 = shape[modelblockrenderer$adjacencyinfo.vert2Weights[0].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert2Weights[1].shape];
                float f22 = shape[modelblockrenderer$adjacencyinfo.vert2Weights[2].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert2Weights[3].shape];
                float f23 = shape[modelblockrenderer$adjacencyinfo.vert2Weights[4].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert2Weights[5].shape];
                float f24 = shape[modelblockrenderer$adjacencyinfo.vert2Weights[6].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert2Weights[7].shape];
                float f25 = shape[modelblockrenderer$adjacencyinfo.vert3Weights[0].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert3Weights[1].shape];
                float f26 = shape[modelblockrenderer$adjacencyinfo.vert3Weights[2].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert3Weights[3].shape];
                float f27 = shape[modelblockrenderer$adjacencyinfo.vert3Weights[4].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert3Weights[5].shape];
                float f28 = shape[modelblockrenderer$adjacencyinfo.vert3Weights[6].shape]
                    * shape[modelblockrenderer$adjacencyinfo.vert3Weights[7].shape];
                this.brightness[modelblockrenderer$ambientvertexremap.vert0] = f29 * f13 + f31 * f14 + f32 * f15 + f33 * f16;
                this.brightness[modelblockrenderer$ambientvertexremap.vert1] = f29 * f17 + f31 * f18 + f32 * f19 + f33 * f20;
                this.brightness[modelblockrenderer$ambientvertexremap.vert2] = f29 * f21 + f31 * f22 + f32 * f23 + f33 * f24;
                this.brightness[modelblockrenderer$ambientvertexremap.vert3] = f29 * f25 + f31 * f26 + f32 * f27 + f33 * f28;
                int i2 = this.blend(l, i, j1, i3);
                int j2 = this.blend(k, i, i1, i3);
                int k2 = this.blend(k, j, k1, i3);
                int l2 = this.blend(l, j, l1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert0] = this.blend(i2, j2, k2, l2, f13, f14, f15, f16);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert1] = this.blend(i2, j2, k2, l2, f17, f18, f19, f20);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert2] = this.blend(i2, j2, k2, l2, f21, f22, f23, f24);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert3] = this.blend(i2, j2, k2, l2, f25, f26, f27, f28);
            } else {
                float f9 = (f3 + f + f5 + f8) * 0.25F;
                float f10 = (f2 + f + f4 + f8) * 0.25F;
                float f11 = (f2 + f1 + f6 + f8) * 0.25F;
                float f12 = (f3 + f1 + f7 + f8) * 0.25F;
                this.lightmap[modelblockrenderer$ambientvertexremap.vert0] = this.blend(l, i, j1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert1] = this.blend(k, i, i1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert2] = this.blend(k, j, k1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert3] = this.blend(l, j, l1, i3);
                this.brightness[modelblockrenderer$ambientvertexremap.vert0] = f9;
                this.brightness[modelblockrenderer$ambientvertexremap.vert1] = f10;
                this.brightness[modelblockrenderer$ambientvertexremap.vert2] = f11;
                this.brightness[modelblockrenderer$ambientvertexremap.vert3] = f12;
            }

            float f30 = level.getShade(direction, shade);

            for (int j3 = 0; j3 < this.brightness.length; j3++) {
                this.brightness[j3] = this.brightness[j3] * f30;
            }
        }

        /**
         * @return the ambient occlusion light color
         */
        private int blend(int lightColor0, int lightColor1, int lightColor2, int lightColor3) {
            if (lightColor0 == 0) {
                lightColor0 = lightColor3;
            }

            if (lightColor1 == 0) {
                lightColor1 = lightColor3;
            }

            if (lightColor2 == 0) {
                lightColor2 = lightColor3;
            }

            return lightColor0 + lightColor1 + lightColor2 + lightColor3 >> 2 & 16711935;
        }

        private int blend(int brightness0, int brightness1, int brightness2, int brightness3, float weight0, float weight1, float weight2, float weight3) {
            int i = (int)(
                    (float)(brightness0 >> 16 & 0xFF) * weight0
                        + (float)(brightness1 >> 16 & 0xFF) * weight1
                        + (float)(brightness2 >> 16 & 0xFF) * weight2
                        + (float)(brightness3 >> 16 & 0xFF) * weight3
                )
                & 0xFF;
            int j = (int)(
                    (float)(brightness0 & 0xFF) * weight0
                        + (float)(brightness1 & 0xFF) * weight1
                        + (float)(brightness2 & 0xFF) * weight2
                        + (float)(brightness3 & 0xFF) * weight3
                )
                & 0xFF;
            return i << 16 | j;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum AmbientVertexRemap {
        DOWN(0, 1, 2, 3),
        UP(2, 3, 0, 1),
        NORTH(3, 0, 1, 2),
        SOUTH(0, 1, 2, 3),
        WEST(3, 0, 1, 2),
        EAST(1, 2, 3, 0);

        final int vert0;
        final int vert1;
        final int vert2;
        final int vert3;
        private static final ModelBlockRenderer.AmbientVertexRemap[] BY_FACING = Util.make(new ModelBlockRenderer.AmbientVertexRemap[6], p_111204_ -> {
            p_111204_[Direction.DOWN.get3DDataValue()] = DOWN;
            p_111204_[Direction.UP.get3DDataValue()] = UP;
            p_111204_[Direction.NORTH.get3DDataValue()] = NORTH;
            p_111204_[Direction.SOUTH.get3DDataValue()] = SOUTH;
            p_111204_[Direction.WEST.get3DDataValue()] = WEST;
            p_111204_[Direction.EAST.get3DDataValue()] = EAST;
        });

        private AmbientVertexRemap(int vert0, int vert1, int vert2, int vert3) {
            this.vert0 = vert0;
            this.vert1 = vert1;
            this.vert2 = vert2;
            this.vert3 = vert3;
        }

        public static ModelBlockRenderer.AmbientVertexRemap fromFacing(Direction facing) {
            return BY_FACING[facing.get3DDataValue()];
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Cache {
        private boolean enabled;
        private final Long2IntLinkedOpenHashMap colorCache = Util.make(() -> {
            Long2IntLinkedOpenHashMap long2intlinkedopenhashmap = new Long2IntLinkedOpenHashMap(100, 0.25F) {
                @Override
                protected void rehash(int newN) {
                }
            };
            long2intlinkedopenhashmap.defaultReturnValue(Integer.MAX_VALUE);
            return long2intlinkedopenhashmap;
        });
        private final Long2FloatLinkedOpenHashMap brightnessCache = Util.make(() -> {
            Long2FloatLinkedOpenHashMap long2floatlinkedopenhashmap = new Long2FloatLinkedOpenHashMap(100, 0.25F) {
                @Override
                protected void rehash(int newN) {
                }
            };
            long2floatlinkedopenhashmap.defaultReturnValue(Float.NaN);
            return long2floatlinkedopenhashmap;
        });

        private Cache() {
        }

        public void enable() {
            this.enabled = true;
        }

        public void disable() {
            this.enabled = false;
            this.colorCache.clear();
            this.brightnessCache.clear();
        }

        public int getLightColor(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            long i = pos.asLong();
            if (this.enabled) {
                int j = this.colorCache.get(i);
                if (j != Integer.MAX_VALUE) {
                    return j;
                }
            }

            int k = LevelRenderer.getLightColor(level, state, pos);
            if (this.enabled) {
                if (this.colorCache.size() == 100) {
                    this.colorCache.removeFirstInt();
                }

                this.colorCache.put(i, k);
            }

            return k;
        }

        public float getShadeBrightness(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            long i = pos.asLong();
            if (this.enabled) {
                float f = this.brightnessCache.get(i);
                if (!Float.isNaN(f)) {
                    return f;
                }
            }

            float f1 = state.getShadeBrightness(level, pos);
            if (this.enabled) {
                if (this.brightnessCache.size() == 100) {
                    this.brightnessCache.removeFirstFloat();
                }

                this.brightnessCache.put(i, f1);
            }

            return f1;
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected static enum SizeInfo {
        DOWN(Direction.DOWN, false),
        UP(Direction.UP, false),
        NORTH(Direction.NORTH, false),
        SOUTH(Direction.SOUTH, false),
        WEST(Direction.WEST, false),
        EAST(Direction.EAST, false),
        FLIP_DOWN(Direction.DOWN, true),
        FLIP_UP(Direction.UP, true),
        FLIP_NORTH(Direction.NORTH, true),
        FLIP_SOUTH(Direction.SOUTH, true),
        FLIP_WEST(Direction.WEST, true),
        FLIP_EAST(Direction.EAST, true);

        final int shape;

        private SizeInfo(Direction direction, boolean flip) {
            this.shape = direction.get3DDataValue() + (flip ? ModelBlockRenderer.DIRECTIONS.length : 0);
        }
    }
}

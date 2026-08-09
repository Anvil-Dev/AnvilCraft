package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.INegativeShapeBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class NegativeShapeModelEventListener {
    private static final float MODEL_COORDINATE_SCALE = 16.0F;
    private static final float MIN_MODEL_EDGE = 0.025F;
    private static final float MAX_MODEL_EDGE = 15.975F;
    private static final float MODEL_EDGE_TOLERANCE = 0.0001F;
    private static final ModelProperty<FaceCoverage> FACE_COVERAGE_PROPERTY = new ModelProperty<>();
    private static final Map<BlockState, Map<Direction, List<FaceRectangle>>> EDGE_RECTANGLES = new HashMap<>();

    /**
     * 将负形方块模型的外缘四边形归入方向剔除列表。
     */
    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        EDGE_RECTANGLES.clear();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof INegativeShapeBlock<?>)) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
                models.computeIfPresent(
                    location,
                    (key, model) -> {
                        BakedModel originalModel = model instanceof NegativeShapeBakedModel wrapper
                                                   ? wrapper.originalModel()
                                                   : model;
                        EDGE_RECTANGLES.put(state, collectEdgeRectangles(originalModel, state));
                        return model instanceof NegativeShapeBakedModel ? model : new NegativeShapeBakedModel(model);
                    }
                );
            }
        }
    }

    public static boolean shouldSkipFace(BlockState state, BlockState adjacentState, Direction face) {
        if (!(state.getBlock() instanceof INegativeShapeBlock<?> block)
            || !block.getBlockType().isInstance(adjacentState.getBlock())) {
            return false;
        }
        List<FaceRectangle> selfRectangles = getEdgeRectangles(state, face);
        if (selfRectangles.isEmpty()) return false;
        List<FaceRectangle> adjacentRectangles = getEdgeRectangles(adjacentState, face.getOpposite());
        if (adjacentRectangles.isEmpty()) return false;
        for (FaceRectangle rectangle : selfRectangles) {
            if (!subtract(rectangle, adjacentRectangles).isEmpty()) return false;
        }
        return true;
    }

    private static Map<Direction, List<FaceRectangle>> collectEdgeRectangles(BakedModel model, BlockState state) {
        EnumMap<Direction, List<FaceRectangle>> result = new EnumMap<>(Direction.class);
        List<BakedQuad> quads = model.getQuads(state, null, RandomSource.create(0L), ModelData.EMPTY, null);
        for (BakedQuad quad : quads) {
            Direction direction = getEdgeDirection(quad);
            if (direction == null) continue;
            FaceRectangle rectangle = FaceRectangle.fromQuad(quad, direction);
            if (rectangle == null) continue;
            addUnique(result.computeIfAbsent(direction, ignored -> new ArrayList<>()), rectangle);
        }
        result.replaceAll((direction, rectangles) -> List.copyOf(rectangles));
        return Map.copyOf(result);
    }

    private static void addUnique(List<FaceRectangle> rectangles, FaceRectangle rectangle) {
        for (FaceRectangle existing : rectangles) {
            if (existing.sameAs(rectangle)) return;
        }
        rectangles.add(rectangle);
    }

    private static List<FaceRectangle> getEdgeRectangles(BlockState state, Direction face) {
        Map<Direction, List<FaceRectangle>> rectangles = EDGE_RECTANGLES.get(state);
        if (rectangles == null) return List.of();
        return rectangles.getOrDefault(face, List.of());
    }

    private static class NegativeShapeBakedModel extends BakedModelWrapper<BakedModel> {
        NegativeShapeBakedModel(BakedModel originalModel) {
            super(originalModel);
        }

        private BakedModel originalModel() {
            return originalModel;
        }

        @Override
        public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource random
        ) {
            return getQuads(state, side, random, ModelData.EMPTY, null);
        }

        @Override
        public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource random,
            ModelData modelData,
            @Nullable RenderType renderType
        ) {
            List<BakedQuad> quads = originalModel.getQuads(state, null, random, modelData, renderType);
            if (side == null) return withoutEdgeQuads(quads);
            FaceCoverage coverage = modelData.get(FACE_COVERAGE_PROPERTY);
            return edgeQuads(quads, side, coverage == null ? List.of() : coverage.get(side));
        }

        @Override
        public ModelData getModelData(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            ModelData modelData
        ) {
            ModelData result = originalModel.getModelData(level, pos, state, modelData);
            if (!(state.getBlock() instanceof INegativeShapeBlock<?> block)) return result;
            EnumMap<Direction, List<FaceRectangle>> coverage = null;
            for (Direction direction : Direction.values()) {
                BlockState adjacentState = level.getBlockState(pos.relative(direction));
                if (!block.getBlockType().isInstance(adjacentState.getBlock())) continue;
                List<FaceRectangle> rectangles = getEdgeRectangles(adjacentState, direction.getOpposite());
                if (rectangles.isEmpty()) continue;
                if (coverage == null) coverage = new EnumMap<>(Direction.class);
                coverage.put(direction, rectangles);
            }
            if (coverage == null) return result;
            return result.derive().with(FACE_COVERAGE_PROPERTY, new FaceCoverage(coverage)).build();
        }

        private static List<BakedQuad> withoutEdgeQuads(List<BakedQuad> quads) {
            List<BakedQuad> result = null;
            for (int i = 0; i < quads.size(); i++) {
                BakedQuad quad = quads.get(i);
                if (getEdgeDirection(quad) != null) {
                    if (result == null) {
                        result = new ArrayList<>(quads.size());
                        result.addAll(quads.subList(0, i));
                    }
                } else if (result != null) {
                    result.add(quad);
                }
            }
            return result == null ? quads : result;
        }

        private static List<BakedQuad> edgeQuads(
            List<BakedQuad> quads,
            Direction side,
            List<FaceRectangle> coverage
        ) {
            List<BakedQuad> result = null;
            for (BakedQuad quad : quads) {
                if (getEdgeDirection(quad) != side) continue;
                if (result == null) result = new ArrayList<>();
                if (coverage.isEmpty()) {
                    result.add(quad);
                } else {
                    result.addAll(sliceQuad(quad, side, coverage));
                }
            }
            return result == null ? List.of() : result;
        }

        private static List<BakedQuad> sliceQuad(
            BakedQuad quad,
            Direction side,
            List<FaceRectangle> coverage
        ) {
            FaceRectangle rectangle = FaceRectangle.fromQuad(quad, side);
            if (rectangle == null) return List.of(quad);
            List<FaceRectangle> visibleRectangles = subtract(rectangle, coverage);
            if (visibleRectangles.isEmpty()) return List.of();
            if (visibleRectangles.size() == 1 && visibleRectangles.getFirst().sameAs(rectangle)) return List.of(quad);
            List<BakedQuad> result = new ArrayList<>(visibleRectangles.size());
            for (FaceRectangle visibleRectangle : visibleRectangles) {
                BakedQuad slicedQuad = rebuildQuad(quad, side, rectangle, visibleRectangle);
                result.add(slicedQuad == null ? quad : slicedQuad);
            }
            return result;
        }
    }

    @Nullable
    private static Direction getEdgeDirection(BakedQuad quad) {
        for (Direction direction : Direction.values()) {
            float edge = direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE
                         ? MIN_MODEL_EDGE
                         : MAX_MODEL_EDGE;
            if (isOnEdge(quad, direction.getAxis(), edge)) return direction;
        }
        return null;
    }

    private static boolean isOnEdge(BakedQuad quad, Direction.Axis axis, float edge) {
        int[] vertices = quad.getVertices();
        if (vertices.length % 4 != 0) return false;
        int vertexStride = vertices.length / 4;
        if (vertexStride < 3) return false;
        int coordinateOffset = coordinateOffset(axis);
        for (int vertex = 0; vertex < 4; vertex++) {
            float coordinate = modelCoordinate(vertices, vertexStride, vertex, coordinateOffset);
            if (!Float.isFinite(coordinate) || Math.abs(coordinate - edge) > MODEL_EDGE_TOLERANCE) return false;
        }
        return true;
    }

    private static int coordinateOffset(Direction.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }

    private static int firstCoordinateOffset(Direction side) {
        return switch (side.getAxis()) {
            case X -> 1;
            case Y, Z -> 0;
        };
    }

    private static int secondCoordinateOffset(Direction side) {
        return switch (side.getAxis()) {
            case X, Y -> 2;
            case Z -> 1;
        };
    }

    private static float modelCoordinate(int[] vertices, int vertexStride, int vertex, int coordinateOffset) {
        return Float.intBitsToFloat(vertices[vertex * vertexStride + coordinateOffset]) * MODEL_COORDINATE_SCALE;
    }

    private static List<FaceRectangle> subtract(FaceRectangle rectangle, List<FaceRectangle> coverage) {
        List<FaceRectangle> visible = List.of(rectangle);
        for (FaceRectangle coveredRectangle : coverage) {
            if (visible.isEmpty()) return visible;
            List<FaceRectangle> next = new ArrayList<>();
            for (FaceRectangle visibleRectangle : visible) {
                visibleRectangle.subtract(coveredRectangle, next);
            }
            visible = next;
        }
        return visible;
    }

    @Nullable
    private static BakedQuad rebuildQuad(
        BakedQuad quad,
        Direction side,
        FaceRectangle sourceRectangle,
        FaceRectangle targetRectangle
    ) {
        int[] vertices = quad.getVertices();
        if (vertices.length % 4 != 0) return null;
        int vertexStride = vertices.length / 4;
        if (vertexStride < 6) return null;
        int firstOffset = firstCoordinateOffset(side);
        int secondOffset = secondCoordinateOffset(side);
        int[] cornerVertices = getCornerVertices(vertices, vertexStride, sourceRectangle, firstOffset, secondOffset);
        if (cornerVertices == null) return null;
        int[] result = vertices.clone();
        for (int vertex = 0; vertex < 4; vertex++) {
            int vertexOffset = vertex * vertexStride;
            float sourceFirst = modelCoordinate(vertices, vertexStride, vertex, firstOffset);
            float sourceSecond = modelCoordinate(vertices, vertexStride, vertex, secondOffset);
            float targetFirst = sourceRectangle.isMinFirst(sourceFirst)
                                ? targetRectangle.minFirst()
                                : targetRectangle.maxFirst();
            float targetSecond = sourceRectangle.isMinSecond(sourceSecond)
                                 ? targetRectangle.minSecond()
                                 : targetRectangle.maxSecond();
            writeInterpolatedVertex(
                vertices,
                result,
                vertexStride,
                vertex,
                cornerVertices,
                sourceRectangle,
                targetFirst,
                targetSecond
            );
            result[vertexOffset + side.getAxis().ordinal()] =
                Float.floatToRawIntBits(edgeCoordinate(side) / MODEL_COORDINATE_SCALE);
            result[vertexOffset + firstOffset] =
                Float.floatToRawIntBits(targetFirst / MODEL_COORDINATE_SCALE);
            result[vertexOffset + secondOffset] =
                Float.floatToRawIntBits(targetSecond / MODEL_COORDINATE_SCALE);
        }
        return new BakedQuad(
            result,
            quad.getTintIndex(),
            quad.getDirection(),
            quad.getSprite(),
            quad.isShade(),
            quad.hasAmbientOcclusion()
        );
    }

    @Nullable
    private static int[] getCornerVertices(
        int[] vertices,
        int vertexStride,
        FaceRectangle rectangle,
        int firstOffset,
        int secondOffset
    ) {
        int[] cornerVertices = {-1, -1, -1, -1};
        for (int vertex = 0; vertex < 4; vertex++) {
            float first = modelCoordinate(vertices, vertexStride, vertex, firstOffset);
            float second = modelCoordinate(vertices, vertexStride, vertex, secondOffset);
            int firstIndex = rectangle.isMinFirst(first) ? 0 : rectangle.isMaxFirst(first) ? 1 : -1;
            int secondIndex = rectangle.isMinSecond(second) ? 0 : rectangle.isMaxSecond(second) ? 1 : -1;
            if (firstIndex < 0 || secondIndex < 0) return null;
            int cornerIndex = firstIndex + secondIndex * 2;
            if (cornerVertices[cornerIndex] >= 0) return null;
            cornerVertices[cornerIndex] = vertex;
        }
        for (int cornerVertex : cornerVertices) {
            if (cornerVertex < 0) return null;
        }
        return cornerVertices;
    }

    private static void writeInterpolatedVertex(
        int[] source,
        int[] target,
        int vertexStride,
        int targetVertex,
        int[] cornerVertices,
        FaceRectangle sourceRectangle,
        float targetFirst,
        float targetSecond
    ) {
        float firstFactor = sourceRectangle.firstFactor(targetFirst);
        float secondFactor = sourceRectangle.secondFactor(targetSecond);
        int targetOffset = targetVertex * vertexStride;
        for (int element = 0; element < vertexStride; element++) {
            target[targetOffset + element] = switch (element) {
                case 0, 1, 2, 4, 5 -> Float.floatToRawIntBits(
                    interpolateFloat(source, vertexStride, cornerVertices, element, firstFactor, secondFactor)
                );
                case 3 -> interpolateColor(source, vertexStride, cornerVertices, element, firstFactor, secondFactor);
                default -> pickDiscreteValue(source, vertexStride, cornerVertices, element, firstFactor, secondFactor);
            };
        }
    }

    private static float interpolateFloat(
        int[] source,
        int vertexStride,
        int[] cornerVertices,
        int element,
        float firstFactor,
        float secondFactor
    ) {
        float value00 = Float.intBitsToFloat(source[cornerVertices[0] * vertexStride + element]);
        float value10 = Float.intBitsToFloat(source[cornerVertices[1] * vertexStride + element]);
        float value01 = Float.intBitsToFloat(source[cornerVertices[2] * vertexStride + element]);
        float value11 = Float.intBitsToFloat(source[cornerVertices[3] * vertexStride + element]);
        return interpolate(interpolate(value00, value10, firstFactor), interpolate(value01, value11, firstFactor),
            secondFactor);
    }

    private static int interpolateColor(
        int[] source,
        int vertexStride,
        int[] cornerVertices,
        int element,
        float firstFactor,
        float secondFactor
    ) {
        int color00 = source[cornerVertices[0] * vertexStride + element];
        int color10 = source[cornerVertices[1] * vertexStride + element];
        int color01 = source[cornerVertices[2] * vertexStride + element];
        int color11 = source[cornerVertices[3] * vertexStride + element];
        if (color00 == color10 && color00 == color01 && color00 == color11) return color00;
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            float channel = interpolate(
                interpolate((color00 >>> shift) & 0xFF, (color10 >>> shift) & 0xFF, firstFactor),
                interpolate((color01 >>> shift) & 0xFF, (color11 >>> shift) & 0xFF, firstFactor),
                secondFactor
            );
            result |= Math.round(channel) << shift;
        }
        return result;
    }

    private static int pickDiscreteValue(
        int[] source,
        int vertexStride,
        int[] cornerVertices,
        int element,
        float firstFactor,
        float secondFactor
    ) {
        int value = source[cornerVertices[0] * vertexStride + element];
        boolean same = true;
        for (int corner = 1; corner < 4; corner++) {
            if (source[cornerVertices[corner] * vertexStride + element] != value) {
                same = false;
                break;
            }
        }
        if (same) return value;
        int firstIndex = firstFactor < 0.5F ? 0 : 1;
        int secondIndex = secondFactor < 0.5F ? 0 : 1;
        return source[cornerVertices[firstIndex + secondIndex * 2] * vertexStride + element];
    }

    private static float edgeCoordinate(Direction side) {
        return side.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? MIN_MODEL_EDGE : MAX_MODEL_EDGE;
    }

    private static float interpolate(float from, float to, float factor) {
        return from + (to - from) * factor;
    }

    private record FaceCoverage(Map<Direction, List<FaceRectangle>> coverage) {
        private FaceCoverage(Map<Direction, List<FaceRectangle>> coverage) {
            this.coverage = Map.copyOf(coverage);
        }

        private List<FaceRectangle> get(Direction direction) {
            return coverage.getOrDefault(direction, List.of());
        }
    }

    private record FaceRectangle(float minFirst, float minSecond, float maxFirst, float maxSecond) {
        @Nullable
        private static FaceRectangle fromQuad(BakedQuad quad, Direction side) {
            int[] vertices = quad.getVertices();
            if (vertices.length % 4 != 0) return null;
            int vertexStride = vertices.length / 4;
            if (vertexStride < 3) return null;
            int firstOffset = firstCoordinateOffset(side);
            int secondOffset = secondCoordinateOffset(side);
            float minFirst = Float.POSITIVE_INFINITY;
            float minSecond = Float.POSITIVE_INFINITY;
            float maxFirst = Float.NEGATIVE_INFINITY;
            float maxSecond = Float.NEGATIVE_INFINITY;
            for (int vertex = 0; vertex < 4; vertex++) {
                float first = modelCoordinate(vertices, vertexStride, vertex, firstOffset);
                float second = modelCoordinate(vertices, vertexStride, vertex, secondOffset);
                if (!Float.isFinite(first) || !Float.isFinite(second)) return null;
                minFirst = Math.min(minFirst, first);
                minSecond = Math.min(minSecond, second);
                maxFirst = Math.max(maxFirst, first);
                maxSecond = Math.max(maxSecond, second);
            }
            FaceRectangle rectangle = new FaceRectangle(minFirst, minSecond, maxFirst, maxSecond);
            return rectangle.isEmpty() ? null : rectangle;
        }

        private boolean isEmpty() {
            return maxFirst - minFirst <= MODEL_EDGE_TOLERANCE
                   || maxSecond - minSecond <= MODEL_EDGE_TOLERANCE;
        }

        private boolean sameAs(FaceRectangle rectangle) {
            return Math.abs(minFirst - rectangle.minFirst) <= MODEL_EDGE_TOLERANCE
                   && Math.abs(minSecond - rectangle.minSecond) <= MODEL_EDGE_TOLERANCE
                   && Math.abs(maxFirst - rectangle.maxFirst) <= MODEL_EDGE_TOLERANCE
                   && Math.abs(maxSecond - rectangle.maxSecond) <= MODEL_EDGE_TOLERANCE;
        }

        private boolean isMinFirst(float value) {
            return Math.abs(value - minFirst) <= MODEL_EDGE_TOLERANCE;
        }

        private boolean isMaxFirst(float value) {
            return Math.abs(value - maxFirst) <= MODEL_EDGE_TOLERANCE;
        }

        private boolean isMinSecond(float value) {
            return Math.abs(value - minSecond) <= MODEL_EDGE_TOLERANCE;
        }

        private boolean isMaxSecond(float value) {
            return Math.abs(value - maxSecond) <= MODEL_EDGE_TOLERANCE;
        }

        private float firstFactor(float value) {
            return clamp((value - minFirst) / (maxFirst - minFirst));
        }

        private float secondFactor(float value) {
            return clamp((value - minSecond) / (maxSecond - minSecond));
        }

        private void subtract(FaceRectangle coveredRectangle, List<FaceRectangle> result) {
            FaceRectangle intersection = intersection(coveredRectangle);
            if (intersection == null) {
                result.add(this);
                return;
            }
            addIfVisible(result, new FaceRectangle(minFirst, minSecond, intersection.minFirst, maxSecond));
            addIfVisible(result, new FaceRectangle(intersection.maxFirst, minSecond, maxFirst, maxSecond));
            addIfVisible(result, new FaceRectangle(
                intersection.minFirst,
                minSecond,
                intersection.maxFirst,
                intersection.minSecond
            ));
            addIfVisible(result, new FaceRectangle(
                intersection.minFirst,
                intersection.maxSecond,
                intersection.maxFirst,
                maxSecond
            ));
        }

        @Nullable
        private FaceRectangle intersection(FaceRectangle rectangle) {
            FaceRectangle intersection = new FaceRectangle(
                Math.max(minFirst, rectangle.minFirst),
                Math.max(minSecond, rectangle.minSecond),
                Math.min(maxFirst, rectangle.maxFirst),
                Math.min(maxSecond, rectangle.maxSecond)
            );
            return intersection.isEmpty() ? null : intersection;
        }

        private static void addIfVisible(List<FaceRectangle> rectangles, FaceRectangle rectangle) {
            if (!rectangle.isEmpty()) rectangles.add(rectangle);
        }
    }

    private static float clamp(float value) {
        return Math.clamp(value, 0.0F, 1.0F);
    }
}

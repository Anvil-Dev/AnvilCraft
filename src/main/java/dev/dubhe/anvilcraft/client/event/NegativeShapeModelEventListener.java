package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.INegativeShapeBlock;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class NegativeShapeModelEventListener {
    private static final float MODEL_COORDINATE_SCALE = 16.0F;
    private static final float MIN_MODEL_EDGE = 0.025F;
    private static final float MAX_MODEL_EDGE = 15.975F;
    private static final float MODEL_EDGE_TOLERANCE = 0.0001F;
    private static final FaceCoverage EMPTY_COVERAGE = new FaceCoverage(Map.of());
    private static final Map<BlockState, Map<Direction, List<FaceRectangle>>> EDGE_RECTANGLES = new HashMap<>();

    /**
     * 将负形方块模型的外缘四边形归入方向剔除列表。
     */
    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        EDGE_RECTANGLES.clear();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof INegativeShapeBlock<?>)) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                models.computeIfPresent(
                    state,
                    (key, model) -> {
                        BlockStateModel originalModel = model instanceof NegativeShapeBlockStateModel wrapper
                                                        ? wrapper.originalModel()
                                                        : model;
                        EDGE_RECTANGLES.put(state, collectEdgeRectangles(originalModel));
                        return model instanceof NegativeShapeBlockStateModel
                               ? model
                               : new NegativeShapeBlockStateModel(model);
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

    private static Map<Direction, List<FaceRectangle>> collectEdgeRectangles(BlockStateModel model) {
        EnumMap<Direction, List<FaceRectangle>> result = new EnumMap<>(Direction.class);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(null)) {
                Direction direction = getEdgeDirection(quad);
                if (direction == null) continue;
                FaceRectangle rectangle = FaceRectangle.fromQuad(quad, direction);
                if (rectangle == null) continue;
                addUnique(result.computeIfAbsent(direction, ignored -> new ArrayList<>()), rectangle);
            }
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

    private static class NegativeShapeBlockStateModel extends DelegateBlockStateModel {
        NegativeShapeBlockStateModel(BlockStateModel originalModel) {
            super(originalModel);
        }

        private BlockStateModel originalModel() {
            return this.delegate;
        }

        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
            int firstAddedPart = parts.size();
            super.collectParts(random, parts);
            wrapAddedParts(parts, firstAddedPart, EMPTY_COVERAGE);
        }

        @Override
        public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> parts
        ) {
            int firstAddedPart = parts.size();
            super.collectParts(level, pos, state, random, parts);
            wrapAddedParts(parts, firstAddedPart, collectCoverage(level, pos, state));
        }

        private static void wrapAddedParts(
            List<BlockStateModelPart> parts,
            int firstAddedPart,
            FaceCoverage coverage
        ) {
            for (int index = firstAddedPart; index < parts.size(); index++) {
                BlockStateModelPart part = parts.get(index);
                if (!(part instanceof NegativeShapePart)) {
                    parts.set(index, new NegativeShapePart(part, coverage));
                }
            }
        }

        private static FaceCoverage collectCoverage(BlockAndTintGetter level, BlockPos pos, BlockState state) {
            if (!(state.getBlock() instanceof INegativeShapeBlock<?> block)) return EMPTY_COVERAGE;
            EnumMap<Direction, List<FaceRectangle>> coverage = null;
            for (Direction direction : Direction.values()) {
                BlockState adjacentState = level.getBlockState(pos.relative(direction));
                if (!block.getBlockType().isInstance(adjacentState.getBlock())) continue;
                List<FaceRectangle> rectangles = getEdgeRectangles(adjacentState, direction.getOpposite());
                if (rectangles.isEmpty()) continue;
                if (coverage == null) coverage = new EnumMap<>(Direction.class);
                coverage.put(direction, rectangles);
            }
            return coverage == null ? EMPTY_COVERAGE : new FaceCoverage(coverage);
        }
    }

    private static class NegativeShapePart implements BlockStateModelPart {
        private final BlockStateModelPart originalPart;
        private final FaceCoverage coverage;

        NegativeShapePart(BlockStateModelPart originalPart, FaceCoverage coverage) {
            this.originalPart = originalPart;
            this.coverage = coverage;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            List<BakedQuad> quads = this.originalPart.getQuads(side);
            if (side == null) return withoutEdgeQuads(quads);
            List<BakedQuad> edgeQuads = edgeQuads(
                this.originalPart.getQuads(null),
                side,
                this.coverage.get(side)
            );
            if (quads.isEmpty()) return edgeQuads;
            if (edgeQuads.isEmpty()) return quads;
            List<BakedQuad> result = new ArrayList<>(quads.size() + edgeQuads.size());
            result.addAll(quads);
            result.addAll(edgeQuads);
            return result;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.originalPart.useAmbientOcclusion();
        }

        @Override
        public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
            return this.originalPart.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return this.originalPart.materialFlags();
        }

        private static List<BakedQuad> withoutEdgeQuads(List<BakedQuad> quads) {
            List<BakedQuad> result = null;
            for (int index = 0; index < quads.size(); index++) {
                BakedQuad quad = quads.get(index);
                if (getEdgeDirection(quad) != null) {
                    if (result == null) {
                        result = new ArrayList<>(quads.size());
                        result.addAll(quads.subList(0, index));
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
        int coordinateOffset = coordinateOffset(axis);
        for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
            float coordinate = modelCoordinate(quad.position(vertex), coordinateOffset);
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

    private static float modelCoordinate(Vector3fc position, int coordinateOffset) {
        return switch (coordinateOffset) {
            case 0 -> position.x();
            case 1 -> position.y();
            case 2 -> position.z();
            default -> throw new IllegalArgumentException("Invalid coordinate offset: " + coordinateOffset);
        } * MODEL_COORDINATE_SCALE;
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
        int firstOffset = firstCoordinateOffset(side);
        int secondOffset = secondCoordinateOffset(side);
        int[] cornerVertices = getCornerVertices(quad, sourceRectangle, firstOffset, secondOffset);
        if (cornerVertices == null) return null;
        Vector3f[] positions = new Vector3f[BakedQuad.VERTEX_COUNT];
        long[] packedUvs = new long[BakedQuad.VERTEX_COUNT];
        for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
            Vector3fc sourcePosition = quad.position(vertex);
            float sourceFirst = modelCoordinate(sourcePosition, firstOffset);
            float sourceSecond = modelCoordinate(sourcePosition, secondOffset);
            float targetFirst = sourceRectangle.isMinFirst(sourceFirst)
                                ? targetRectangle.minFirst()
                                : targetRectangle.maxFirst();
            float targetSecond = sourceRectangle.isMinSecond(sourceSecond)
                                 ? targetRectangle.minSecond()
                                 : targetRectangle.maxSecond();
            positions[vertex] = buildPosition(side, firstOffset, secondOffset, targetFirst, targetSecond);
            packedUvs[vertex] = interpolatePackedUv(
                quad,
                cornerVertices,
                sourceRectangle.firstFactor(targetFirst),
                sourceRectangle.secondFactor(targetSecond)
            );
        }
        return new BakedQuad(
            positions[0],
            positions[1],
            positions[2],
            positions[3],
            packedUvs[0],
            packedUvs[1],
            packedUvs[2],
            packedUvs[3],
            quad.direction(),
            quad.materialInfo(),
            quad.bakedNormals(),
            quad.bakedColors()
        );
    }

    private static Vector3f buildPosition(
        Direction side,
        int firstOffset,
        int secondOffset,
        float targetFirst,
        float targetSecond
    ) {
        float[] coordinates = new float[3];
        coordinates[side.getAxis().ordinal()] = edgeCoordinate(side) / MODEL_COORDINATE_SCALE;
        coordinates[firstOffset] = targetFirst / MODEL_COORDINATE_SCALE;
        coordinates[secondOffset] = targetSecond / MODEL_COORDINATE_SCALE;
        return new Vector3f(coordinates[0], coordinates[1], coordinates[2]);
    }

    @Nullable
    private static int[] getCornerVertices(
        BakedQuad quad,
        FaceRectangle rectangle,
        int firstOffset,
        int secondOffset
    ) {
        int[] cornerVertices = {-1, -1, -1, -1};
        for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
            Vector3fc position = quad.position(vertex);
            float first = modelCoordinate(position, firstOffset);
            float second = modelCoordinate(position, secondOffset);
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

    private static long interpolatePackedUv(
        BakedQuad quad,
        int[] cornerVertices,
        float firstFactor,
        float secondFactor
    ) {
        long uv00 = quad.packedUV(cornerVertices[0]);
        long uv10 = quad.packedUV(cornerVertices[1]);
        long uv01 = quad.packedUV(cornerVertices[2]);
        long uv11 = quad.packedUV(cornerVertices[3]);
        float u = interpolate(
            interpolate(UVPair.unpackU(uv00), UVPair.unpackU(uv10), firstFactor),
            interpolate(UVPair.unpackU(uv01), UVPair.unpackU(uv11), firstFactor),
            secondFactor
        );
        float v = interpolate(
            interpolate(UVPair.unpackV(uv00), UVPair.unpackV(uv10), firstFactor),
            interpolate(UVPair.unpackV(uv01), UVPair.unpackV(uv11), firstFactor),
            secondFactor
        );
        return UVPair.pack(u, v);
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
            return this.coverage.getOrDefault(direction, List.of());
        }
    }

    private record FaceRectangle(float minFirst, float minSecond, float maxFirst, float maxSecond) {
        @Nullable
        private static FaceRectangle fromQuad(BakedQuad quad, Direction side) {
            int firstOffset = firstCoordinateOffset(side);
            int secondOffset = secondCoordinateOffset(side);
            float minFirst = Float.POSITIVE_INFINITY;
            float minSecond = Float.POSITIVE_INFINITY;
            float maxFirst = Float.NEGATIVE_INFINITY;
            float maxSecond = Float.NEGATIVE_INFINITY;
            for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
                Vector3fc position = quad.position(vertex);
                float first = modelCoordinate(position, firstOffset);
                float second = modelCoordinate(position, secondOffset);
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
            return this.maxFirst - this.minFirst <= MODEL_EDGE_TOLERANCE
                   || this.maxSecond - this.minSecond <= MODEL_EDGE_TOLERANCE;
        }

        private boolean sameAs(FaceRectangle rectangle) {
            return Math.abs(this.minFirst - rectangle.minFirst) <= MODEL_EDGE_TOLERANCE
                   && Math.abs(this.minSecond - rectangle.minSecond) <= MODEL_EDGE_TOLERANCE
                   && Math.abs(this.maxFirst - rectangle.maxFirst) <= MODEL_EDGE_TOLERANCE
                   && Math.abs(this.maxSecond - rectangle.maxSecond) <= MODEL_EDGE_TOLERANCE;
        }

        private boolean isMinFirst(float value) {
            return Math.abs(value - this.minFirst) <= MODEL_EDGE_TOLERANCE;
        }

        private boolean isMaxFirst(float value) {
            return Math.abs(value - this.maxFirst) <= MODEL_EDGE_TOLERANCE;
        }

        private boolean isMinSecond(float value) {
            return Math.abs(value - this.minSecond) <= MODEL_EDGE_TOLERANCE;
        }

        private boolean isMaxSecond(float value) {
            return Math.abs(value - this.maxSecond) <= MODEL_EDGE_TOLERANCE;
        }

        private float firstFactor(float value) {
            return clamp((value - this.minFirst) / (this.maxFirst - this.minFirst));
        }

        private float secondFactor(float value) {
            return clamp((value - this.minSecond) / (this.maxSecond - this.minSecond));
        }

        private void subtract(FaceRectangle coveredRectangle, List<FaceRectangle> result) {
            FaceRectangle intersection = this.intersection(coveredRectangle);
            if (intersection == null) {
                result.add(this);
                return;
            }
            addIfVisible(result, new FaceRectangle(this.minFirst, this.minSecond, intersection.minFirst, this.maxSecond));
            addIfVisible(result, new FaceRectangle(intersection.maxFirst, this.minSecond, this.maxFirst, this.maxSecond));
            addIfVisible(result, new FaceRectangle(
                intersection.minFirst,
                this.minSecond,
                intersection.maxFirst,
                intersection.minSecond
            ));
            addIfVisible(result, new FaceRectangle(
                intersection.minFirst,
                intersection.maxSecond,
                intersection.maxFirst,
                this.maxSecond
            ));
        }

        @Nullable
        private FaceRectangle intersection(FaceRectangle rectangle) {
            FaceRectangle intersection = new FaceRectangle(
                Math.max(this.minFirst, rectangle.minFirst),
                Math.max(this.minSecond, rectangle.minSecond),
                Math.min(this.maxFirst, rectangle.maxFirst),
                Math.min(this.maxSecond, rectangle.maxSecond)
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

package dev.dubhe.anvilcraft.client.selection;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.lib.v2.cube.client.model.ModelSelection;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.joml.Matrix4d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

final class ModelSelectionBakery {
    private static final long MAX_BYTES = 64L * 1024 * 1024;
    private static final ModelSelection EMPTY = new ModelSelection.Multipart(List.of(), new AABB(0, 0, 0, 0, 0, 0));
    private final Map<ResourceLocation, UnbakedModel> models;
    private final Map<Variant, ModelSelection> variants = new HashMap<>();
    private final Map<List<ConvexShape>, SelectionGeometry> geometries = new HashMap<>();
    private final Set<ConvexShape> retainedShapes = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<BlockModel, List<ConvexShape>> elements = new IdentityHashMap<>();
    private long bytes;

    ModelSelectionBakery(Map<ResourceLocation, UnbakedModel> models) {
        this.models = models;
    }

    ModelBlockSelection.Snapshot bake(Map<ModelResourceLocation, UnbakedModel> topLevel) {
        Map<BlockState, ModelSelection> states = new IdentityHashMap<>();
        Map<BlockState, List<SelectionPart>> outlines = new IdentityHashMap<>();
        Map<ModelResourceLocation, SelectionPart> standalone = new HashMap<>();
        int failures = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!AnvilCraft.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) continue;
            long previousBytes = this.bytes;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                UnbakedModel model = topLevel.get(BlockModelShaper.stateToModelLocation(state));
                if (model == null) continue;
                try {
                    ModelSelection selection = this.resolve(model, state);
                    if (selection instanceof ModelSelection.Fixed(SelectionPart part)) {
                        standalone.put(BlockModelShaper.stateToModelLocation(state), part);
                    }
                    states.put(state, state.getRenderShape() == RenderShape.MODEL ? selection : EMPTY);
                } catch (IllegalArgumentException exception) {
                    failures++;
                }
            }
            if (block instanceof AbstractMultiPartBlock<?> multipart) this.multipart(multipart, states, outlines);
            if (this.bytes - previousBytes > 1024 * 1024) {
                AnvilCraft.LOGGER.debug("Cube selection geometry for {}: {} bytes", BuiltInRegistries.BLOCK.getKey(block),
                    this.bytes - previousBytes);
            }
        }
        for (Map.Entry<ModelResourceLocation, UnbakedModel> entry : topLevel.entrySet()) {
            if (!AnvilCraft.MOD_ID.equals(entry.getKey().id().getNamespace())
                || !ModelResourceLocation.STANDALONE_VARIANT.equals(entry.getKey().getVariant())) continue;
            if (!(entry.getValue() instanceof BlockModel model)) continue;
            try {
                ModelSelection selection = this.blockModel(model, BlockModelRotation.X0_Y0);
                if (selection instanceof ModelSelection.Fixed(SelectionPart part)) standalone.put(entry.getKey(), part);
            } catch (IllegalArgumentException exception) {
                AnvilCraft.LOGGER.warn("Unable to prepare selection model {}: {}", entry.getKey(), exception.getMessage());
            }
        }
        AnvilCraft.LOGGER.info("Prepared cube selection: {} states, {} multipart parts, {} render models, {} bytes, {} fallback states",
            states.size(), outlines.size(), standalone.size(), this.bytes, failures);
        return new ModelBlockSelection.Snapshot(Map.copyOf(states), Map.copyOf(outlines), Map.copyOf(standalone));
    }

    private ModelSelection resolve(UnbakedModel model, BlockState state) {
        switch (model) {
            case MultiVariant multi -> {
                List<ModelSelection> choices = new ArrayList<>();
                List<Integer> weights = new ArrayList<>();
                for (Variant variant : multi.getVariants()) {
                    choices.add(this.variants.computeIfAbsent(variant, this::variant));
                    weights.add(variant.getWeight());
                }
                if (choices.size() == 1) return choices.getFirst();
                return new ModelSelection.Weighted(choices, weights, weights.stream().mapToInt(Integer::intValue).sum(), bounds(choices));
            }
            case MultiPart multipart -> {
                List<ModelSelection> parts = new ArrayList<>();
                for (Selector selector : multipart.getSelectors()) {
                    if (selector.getPredicate(state.getBlock().getStateDefinition()).test(state)) {
                        parts.add(this.resolve(selector.getVariant(), state));
                    }
                }
                return this.combine(parts);
            }
            case BlockModel blockModel -> {
                return this.blockModel(blockModel, BlockModelRotation.X0_Y0);
            }
            default -> {
            }
        }
        throw new IllegalArgumentException("Unsupported block model " + model.getClass().getSimpleName());
    }

    private ModelSelection variant(Variant variant) {
        if (!(this.models.get(variant.getModelLocation()) instanceof BlockModel model)) {
            throw new IllegalArgumentException("Missing cube model " + variant.getModelLocation());
        }
        return this.blockModel(model, variant);
    }

    private ModelSelection blockModel(BlockModel model, ModelState state) {
        if (model.customData.hasCustomGeometry()) throw new IllegalArgumentException("Custom geometry loader");
        List<ConvexShape> shapes = this.elements.computeIfAbsent(model, key -> ModelCubeGeometry.decode(key.getElements()));
        if (shapes.isEmpty()) return EMPTY;
        ModelState transformed = model.customData.getRootTransform().isIdentity() ? state
            : UnbakedGeometryHelper.composeRootTransformIntoModelState(state, model.customData.getRootTransform());
        Matrix4f matrix = new Matrix4f().translation(0.5F, 0.5F, 0.5F)
            .mul(transformed.getRotation().getMatrix()).translate(-0.5F, -0.5F, -0.5F).scale(1 / ModelCubeGeometry.SCALE);
        return new ModelSelection.Fixed(new SelectionPart(this.geometry(shapes), matrix));
    }

    private ModelSelection combine(List<ModelSelection> parts) {
        if (parts.isEmpty()) return EMPTY;
        if (parts.size() == 1 && parts.getFirst() instanceof ModelSelection.Fixed) return parts.getFirst();
        // 管道等方块有大量状态组合，保留共享子模型，避免为每种连接状态复制整套凸体。
        return new ModelSelection.Multipart(parts, bounds(parts));
    }

    private static void appendShapes(ModelSelection selection, Matrix4d transform, List<ConvexShape> output) {
        if (selection instanceof ModelSelection.Fixed(SelectionPart part1)) {
            PoseStack pose = new PoseStack();
            part1.apply(pose);
            Matrix4d matrix = new Matrix4d(transform).scale(ModelCubeGeometry.SCALE).mul(new Matrix4d(pose.last().pose()));
            part1.geometry().shapes().forEach(shape -> output.add(shape.transform(matrix)));
        } else if (selection instanceof ModelSelection.Multipart multipart) {
            for (ModelSelection part : multipart.parts()) appendShapes(part, transform, output);
        } else {
            throw new IllegalArgumentException("Randomized multipart structure");
        }
    }

    private <P extends Enum<P>> void multipart(
        AbstractMultiPartBlock<P> block, Map<BlockState, ModelSelection> states, Map<BlockState, List<SelectionPart>> outlines
    ) {
        P first = block.getParts()[0];
        Map<BlockState, ModelSelection> original = new IdentityHashMap<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelSelection selection = states.get(state);
            if (selection != null) original.put(state, selection);
        }
        for (BlockState base : block.getStateDefinition().getPossibleStates()) {
            if (base.getValue(block.getPart()) != first) continue;
            try {
                List<ConvexShape> joined = new ArrayList<>();
                for (P part : block.getParts()) {
                    BlockState source = base.setValue(block.getPart(), part);
                    if (block instanceof GiantAnvilBlock) source = block.placedState(part, source);
                    ModelSelection selection = original.get(source);
                    if (selection == EMPTY) continue;
                    if (selection == null) throw new IllegalArgumentException("Missing multipart model");
                    Vec3i offset = block.offsetFrom(base, part);
                    Matrix4d translation = new Matrix4d().translation(
                        offset.getX() * ModelCubeGeometry.SCALE, offset.getY() * ModelCubeGeometry.SCALE,
                        offset.getZ() * ModelCubeGeometry.SCALE
                    );
                    appendShapes(selection, translation, joined);
                }
                if (joined.isEmpty()) {
                    if (base.getRenderShape() != RenderShape.MODEL) {
                        for (P part : block.getParts()) outlines.put(base.setValue(block.getPart(), part), List.of());
                    }
                    continue;
                }
                SelectionGeometry whole = this.geometry(joined);
                AABB occupied = new AABB(0, 0, 0, ModelCubeGeometry.SCALE, ModelCubeGeometry.SCALE, ModelCubeGeometry.SCALE);
                for (P part : block.getParts()) {
                    Vec3i offset = block.offsetFrom(base, part);
                    occupied = occupied.minmax(new AABB(0, 0, 0, ModelCubeGeometry.SCALE, ModelCubeGeometry.SCALE, ModelCubeGeometry.SCALE)
                        .move(offset.getX() * ModelCubeGeometry.SCALE, offset.getY() * ModelCubeGeometry.SCALE,
                            offset.getZ() * ModelCubeGeometry.SCALE));
                }
                Map<BlockState, ModelSelection> clipped = new IdentityHashMap<>();
                Map<BlockState, List<SelectionPart>> complete = new IdentityHashMap<>();
                for (P part : block.getParts()) {
                    BlockState state = base.setValue(block.getPart(), part);
                    Vec3i offset = block.offsetFrom(base, part);
                    double x = offset.getX() * ModelCubeGeometry.SCALE;
                    double y = offset.getY() * ModelCubeGeometry.SCALE;
                    double z = offset.getZ() * ModelCubeGeometry.SCALE;
                    AABB cell = new AABB(x, y, z, x + ModelCubeGeometry.SCALE, y + ModelCubeGeometry.SCALE, z + ModelCubeGeometry.SCALE);
                    AABB bounds = whole.bounds();
                    cell = new AABB(
                        x == occupied.minX ? Math.min(x, bounds.minX) : cell.minX,
                        y == occupied.minY ? Math.min(y, bounds.minY) : cell.minY,
                        z == occupied.minZ ? Math.min(z, bounds.minZ) : cell.minZ,
                        cell.maxX == occupied.maxX ? Math.max(cell.maxX, bounds.maxX) : cell.maxX,
                        cell.maxY == occupied.maxY ? Math.max(cell.maxY, bounds.maxY) : cell.maxY,
                        cell.maxZ == occupied.maxZ ? Math.max(cell.maxZ, bounds.maxZ) : cell.maxZ
                    );
                    List<ConvexShape> pieces = new ArrayList<>();
                    Matrix4d translation = new Matrix4d().translation(-x, -y, -z);
                    for (ConvexShape shape : joined) {
                        ConvexShape piece = ModelCubeGeometry.clip(shape, cell);
                        if (piece != null) pieces.add(piece.transform(translation));
                    }
                    clipped.put(state, this.fixed(pieces));
                    complete.put(state, List.of(new SelectionPart(whole, new Matrix4f()
                        .translation(-offset.getX(), -offset.getY(), -offset.getZ()).scale(1 / ModelCubeGeometry.SCALE))));
                }
                states.putAll(clipped);
                outlines.putAll(complete);
            } catch (IllegalArgumentException exception) {
                AnvilCraft.LOGGER.warn("Unable to prepare multipart selection {}: {}", base, exception.getMessage());
            }
        }
    }

    private ModelSelection fixed(List<ConvexShape> shapes) {
        return shapes.isEmpty() ? EMPTY : new ModelSelection.Fixed(
            new SelectionPart(this.geometry(shapes), new Matrix4f().scaling(1 / ModelCubeGeometry.SCALE))
        );
    }

    private SelectionGeometry geometry(List<ConvexShape> shapes) {
        SelectionGeometry existing = this.geometries.get(shapes);
        if (existing != null) return existing;
        SelectionGeometry result = new SelectionGeometry(shapes);
        if (!this.retain(result)) throw new IllegalArgumentException("Selection memory budget exceeded");
        this.geometries.put(result.shapes(), result);
        return result;
    }

    private boolean retain(SelectionGeometry geometry) {
        long additionalBytes = geometry.estimatedBytes();
        for (ConvexShape shape : geometry.shapes()) {
            if (this.retainedShapes.contains(shape)) additionalBytes -= shape.estimatedBytes();
        }
        if (this.bytes + additionalBytes > MAX_BYTES) return false;
        this.bytes += additionalBytes;
        this.retainedShapes.addAll(geometry.shapes());
        return true;
    }

    static List<SelectionPart> collect(@Nullable ModelSelection selection, long seed) {
        if (selection == null) return List.of();
        if (selection instanceof ModelSelection.Fixed(SelectionPart part)) return List.of(part);
        List<SelectionPart> result = new ArrayList<>();
        selection.collect(RandomSource.create(seed), result);
        return result;
    }

    private static AABB bounds(List<ModelSelection> selections) {
        AABB bounds = selections.isEmpty() ? new AABB(0, 0, 0, 0, 0, 0) : selections.getFirst().bounds();
        for (ModelSelection selection : selections) bounds = bounds.minmax(selection.bounds());
        return bounds;
    }
}

package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.OctahedralGroup;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ChutePlates;
import dev.dubhe.anvilcraft.block.logistics.chute.ChutePlateFit;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class ChutePlateRenderer<T extends BlockEntity & ChutePlates.Holder>
    implements BlockEntityRenderer<T, ChutePlateRenderer.State>, ModelSelectionRenderer<T> {
    private record Part(
        String model, OctahedralGroup rotation, Predicate<Set<Direction>> visible, StandaloneModelKey<BlockStateModel> key,
        Map<ChutePlateFit, StandaloneModelKey<BlockStateModel>> shortened
    ) {
        Part(String model, OctahedralGroup rotation, Predicate<Set<Direction>> visible) {
            this(model, rotation, visible, new StandaloneModelKey<>(() -> model + "/" + rotation.getSerializedName()),
                shortenedKeys(model, rotation));
        }

        private static Map<ChutePlateFit, StandaloneModelKey<BlockStateModel>> shortenedKeys(String model, OctahedralGroup rotation) {
            Map<ChutePlateFit, StandaloneModelKey<BlockStateModel>> keys = new HashMap<>();
            for (ChutePlateFit fit : ChutePlateFit.shortenedFits()) {
                Direction inlet = fit.inlet();
                if (inlet == null) {
                    continue;
                }
                Direction local = rotation.inverse().rotate(inlet);
                if (local.getAxis().isHorizontal() && (model.equals(PLATE) || local != Direction.SOUTH)) {
                    keys.put(fit, new StandaloneModelKey<>(() -> model + "/" + rotation + "/" + local + "/" + fit.cut()));
                }
            }
            return Map.copyOf(keys);
        }

        StandaloneModelKey<BlockStateModel> keyFor(ChutePlateFit fit) {
            return this.shortened.getOrDefault(fit, this.key);
        }
    }

    private static final String PLATE = "chute_plate";
    private static final List<Part> PARTS = createParts();
    private static final Function<Set<Direction>, List<Part>> VISIBLE = Util.memoize(
        faces -> PARTS.stream().filter(part -> part.visible.test(faces)).toList()
    );

    public static class State extends BlockEntityRenderState {
        private final BlockModelRenderState plates = new BlockModelRenderState();
    }

    public ChutePlateRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static List<Part> createParts() {
        List<Part> parts = new ArrayList<>();
        for (OctahedralGroup rotation : OctahedralGroup.values()) {
            if (rotation.transformation().determinant() < 0) {
                continue;
            }
            Direction plate = rotation.rotate(Direction.UP);
            Direction edge = rotation.rotate(Direction.NORTH);
            if (edge == Direction.rotate(new Matrix4f().rotation(plate.getRotation()), Direction.NORTH)) {
                parts.add(new Part(PLATE, rotation, faces -> faces.contains(plate)));
            }
            Direction west = rotation.rotate(Direction.WEST);
            Direction east = rotation.rotate(Direction.EAST);
            // 从同一个开放面观察，每条侧边只占一端的对角像素，另一端的对角像素归相邻板。
            for (boolean cutWest : List.of(false, true)) {
                for (boolean cutEast : List.of(false, true)) {
                    String model = "chute_plate_edge" + (cutWest ? "_west" : "") + (cutEast ? "_east" : "");
                    parts.add(new Part(model, rotation, faces -> faces.contains(plate) && !faces.contains(edge)
                        && faces.contains(west) == cutWest && faces.contains(east) == cutEast));
                }
            }
        }
        return List.copyOf(parts);
    }

    public static void registerModels(ModelEvent.RegisterStandalone event) {
        for (Part part : PARTS) {
            var id = AnvilCraft.of("block/" + part.model);
            var rotation = BlockModelRotation.get(part.rotation);
            event.register(part.key, SimpleUnbakedStandaloneModel.blockStateModel(id, rotation));
            part.shortened.forEach((fit, key) -> {
                Direction inlet = fit.inlet();
                if (inlet != null) {
                    event.register(key, ChutePlateModel.shortened(id, rotation, part.rotation.inverse().rotate(inlet), fit.cut()));
                }
            });
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        T chute, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(chute, state, partialTick, camera, breakProgress);
        state.plates.clear();
        var client = Minecraft.getInstance();
        var level = client.level;
        if (level == null || chute.getChutePlates().getPlates().isEmpty()) {
            return;
        }
        var output = state.plates.setupModel(new Matrix4f(), false);
        var pos = chute.getBlockPos();
        var random = state.plates.scratchRandomSource(pos.asLong());
        ChutePlateFit fit = ChutePlateFit.of(chute.getBlockState());
        for (Part part : VISIBLE.apply(chute.getChutePlates().getPlates())) {
            BlockStateModel model = client.getModelManager().getStandaloneModel(part.keyFor(fit));
            if (model != null) {
                model.collectParts(level, pos, chute.getBlockState(), random, output);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.plates.isEmpty()) {
            return;
        }
        // 整组铁板沿视线靠前绘制，避开溜槽端面的共面深度冲突，同时保持各板接缝闭合。
        pose.pushPose();
        pose.translate(camera.pos.subtract(state.blockPos.getCenter()).normalize().scale(1d / 1024));
        state.plates.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public void collectSelectionModels(T chute, float partialTick, PoseStack pose, ModelConsumer consumer) {
        ChutePlateFit fit = ChutePlateFit.of(chute.getBlockState());
        for (Part part : VISIBLE.apply(chute.getChutePlates().getPlates())) {
            if (part.model.equals(PLATE)) {
                consumer.accept(part.keyFor(fit), pose);
            }
        }
    }

    @Override
    public void collectPreviewModels(T chute, float partialTick, PoseStack pose, ModelConsumer consumer) {
        ChutePlateFit fit = ChutePlateFit.of(chute.getBlockState());
        for (Part part : VISIBLE.apply(chute.getChutePlates().getPlates())) {
            consumer.accept(part.keyFor(fit), pose);
        }
    }
}

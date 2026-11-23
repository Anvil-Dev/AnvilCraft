package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.OctahedralGroup;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.ChutePlateFit;
import dev.dubhe.anvilcraft.block.entity.ChutePlates;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.mixin.accessor.ModelBakeryAccessor;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class ChutePlateRenderer<T extends BlockEntity & ChutePlates.Holder> implements BlockEntityRenderer<T>, ModelSelectionRenderer<T> {
    private record Part(String model, OctahedralGroup rotation, Predicate<Set<Direction>> visible,
                        Quaternionf orientation, ModelResourceLocation key, Map<ChutePlateFit, ModelResourceLocation> shortened) {
        Part(String model, OctahedralGroup rotation, Quaternionf orientation, Predicate<Set<Direction>> visible) {
            this(model, rotation, visible, orientation, key(model),
                shortenedKeys(model, rotation));
        }

        private static ModelResourceLocation key(String model) {
            return ModelResourceLocation.standalone(AnvilCraft.of("block/" + model));
        }

        private static Map<ChutePlateFit, ModelResourceLocation> shortenedKeys(String model, OctahedralGroup rotation) {
            Map<ChutePlateFit, ModelResourceLocation> keys = new HashMap<>();
            for (ChutePlateFit fit : ChutePlateFit.shortenedFits()) {
                Direction inlet = fit.inlet();
                if (inlet == null) {
                    continue;
                }
                Direction local = rotation.inverse().rotate(inlet);
                if (local.getAxis().isHorizontal() && (model.equals(PLATE) || local != Direction.SOUTH)) {
                    keys.put(fit, key(model + "/" + local.getSerializedName() + "_" + fit.cut()));
                }
            }
            return Map.copyOf(keys);
        }

        void collect(ChutePlateFit fit, PoseStack pose, ModelConsumer consumer) {
            pose.pushPose();
            pose.rotateAround(this.orientation, 0.5F, 0.5F, 0.5F);
            consumer.accept(this.shortened.getOrDefault(fit, this.key), pose);
            pose.popPose();
        }
    }

    private static final String PLATE = "chute_plate";
    private static final List<Part> PARTS = createParts();
    private static final Function<Set<Direction>, List<Part>> VISIBLE = Util.memoize(
        faces -> PARTS.stream().filter(part -> part.visible.test(faces)).toList()
    );
    private static Map<ModelResourceLocation, UnbakedModel> shortenedModels = Map.of();

    public ChutePlateRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static List<Part> createParts() {
        List<Part> parts = new ArrayList<>();
        for (OctahedralGroup rotation : OctahedralGroup.values()) {
            var basis = new Matrix3f(rotation.rotate(Direction.EAST).step(), rotation.rotate(Direction.UP).step(),
                rotation.rotate(Direction.SOUTH).step());
            if (basis.determinant() < 0) {
                continue;
            }
            Quaternionf orientation = basis.getNormalizedRotation(new Quaternionf());
            Direction plate = rotation.rotate(Direction.UP);
            Direction edge = rotation.rotate(Direction.NORTH);
            if (edge == Direction.rotate(new Matrix4f().rotation(plate.getRotation()), Direction.NORTH)) {
                parts.add(new Part(PLATE, rotation, orientation, faces -> faces.contains(plate)));
            }
            Direction west = rotation.rotate(Direction.WEST);
            Direction east = rotation.rotate(Direction.EAST);
            // 从同一个开放面观察，每条侧边只占一端的对角像素，另一端的对角像素归相邻板。
            for (boolean cutWest : List.of(false, true)) {
                for (boolean cutEast : List.of(false, true)) {
                    String model = "chute_plate_edge" + (cutWest ? "_west" : "") + (cutEast ? "_east" : "");
                    parts.add(new Part(model, rotation, orientation, faces -> faces.contains(plate) && !faces.contains(edge)
                        && faces.contains(west) == cutWest && faces.contains(east) == cutEast));
                }
            }
        }
        return List.copyOf(parts);
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        PARTS.stream().map(Part::key).distinct().forEach(event::register);
    }

    public static void bakeModels(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, UnbakedModel> sources = ((ModelBakeryAccessor) event.getModelBakery()).getTopLevelModels();
        Map<ModelResourceLocation, UnbakedModel> shortened = new HashMap<>();
        for (Part part : PARTS) {
            UnbakedModel source = sources.get(part.key);
            if (source == null) {
                continue;
            }
            part.shortened.forEach((fit, key) -> {
                Direction inlet = fit.inlet();
                if (inlet == null || shortened.containsKey(key)) {
                    return;
                }
                if (source instanceof BlockModel model && !model.customData.hasCustomGeometry()) {
                    var cut = ChutePlateModel.shortened(model, part.rotation.inverse().rotate(inlet), fit.cut());
                    shortened.put(key, cut);
                    event.getModels().put(key, ChutePlateModel.bake(cut, model, event.getTextureGetter()));
                } else {
                    shortened.put(key, source);
                    event.getModels().put(key, event.getModels().get(part.key));
                }
            });
        }
        shortenedModels = Map.copyOf(shortened);
    }

    public static Map<ModelResourceLocation, UnbakedModel> shortenedModels() {
        return shortenedModels;
    }

    @Override
    public void render(T chute, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (chute.getChutePlates().getPlates().isEmpty()) {
            return;
        }
        var client = Minecraft.getInstance();
        Vec3 bias = client.gameRenderer.getMainCamera().getPosition().subtract(chute.getBlockPos().getCenter())
            .normalize().scale(1d / 1024);
        pose.pushPose();
        pose.translate(bias.x, bias.y, bias.z);
        var consumer = buffers.getBuffer(Sheets.solidBlockSheet());
        this.collectPreviewModels(chute, partialTick, pose, (key, modelPose) -> client.getBlockRenderer().getModelRenderer().renderModel(
            modelPose.last(), consumer, chute.getBlockState(), client.getModelManager().getModel(key), 1, 1, 1, light, overlay));
        pose.popPose();
    }

    @Override
    public void collectSelectionModels(T chute, float partialTick, PoseStack pose, ModelConsumer consumer) {
        ChutePlateFit fit = ChutePlateFit.of(chute.getBlockState());
        for (Part part : VISIBLE.apply(chute.getChutePlates().getPlates())) {
            if (part.model.equals(PLATE)) {
                part.collect(fit, pose, consumer);
            }
        }
    }

    @Override
    public void collectPreviewModels(T chute, float partialTick, PoseStack pose, ModelConsumer consumer) {
        ChutePlateFit fit = ChutePlateFit.of(chute.getBlockState());
        for (Part part : VISIBLE.apply(chute.getChutePlates().getPlates())) {
            part.collect(fit, pose, consumer);
        }
    }
}

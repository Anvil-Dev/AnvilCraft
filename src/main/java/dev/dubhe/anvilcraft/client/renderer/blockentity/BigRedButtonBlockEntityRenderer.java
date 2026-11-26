package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import dev.dubhe.anvilcraft.block.state.ButtonCap;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import dev.dubhe.anvilcraft.block.utility.redstone.ButtonMount;
import dev.dubhe.anvilcraft.client.event.WheelLifecycleEventListener;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.BigRedButtonRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.property.component.ChromaticButtonMode;
import dev.dubhe.anvilcraft.item.property.component.MountedButtonBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BigRedButtonBlockEntityRenderer
    implements BlockEntityRenderer<BigRedButtonBlockEntity, BigRedButtonRenderState>, ModelSelectionRenderer<BigRedButtonBlockEntity> {
    private static final Map<StringRepresentable, StandaloneModelKey<BlockStateModel>> CAPS = Stream.<StringRepresentable>concat(
        Arrays.stream(DyeColor.values()), Arrays.stream(ButtonCap.values()).filter(cap -> cap == ButtonCap.RGB || cap.isHammerCap())
    ).collect(Collectors.toUnmodifiableMap(Function.identity(), cap -> new StandaloneModelKey<>(cap::getSerializedName)));

    private static final StandaloneModelKey<BlockStateModel> GRADIENT = new StandaloneModelKey<>(() -> "gradient chromatic button");

    static StandaloneModelKey<BlockStateModel> chromaticModel(MountedButtonBlock block) {
        return block.mode() == ChromaticButtonMode.GRADIENT ? GRADIENT : CAPS.get(ButtonCap.RGB);
    }

    public BigRedButtonBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerModels(ModelEvent.RegisterStandalone event) {
        var cap = AnvilCraft.of("block/big_red_button_cap");
        event.register(GRADIENT, SimpleUnbakedStandaloneModel.blockStateModel(cap.withSuffix("_rgb_gradient")));
        CAPS.forEach((variant, key) -> event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(
            variant == DyeColor.RED ? cap : cap.withSuffix("_" + variant.getSerializedName())
        )));
    }

    @Override
    public BigRedButtonRenderState createRenderState() {
        return new BigRedButtonRenderState();
    }

    @Override
    public void extractRenderState(
        BigRedButtonBlockEntity blockEntity, BigRedButtonRenderState state, float partialTick,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        extractButtonState(blockEntity, state, partialTick);
        state.model = state.capModel == null ? null : FeatureRendererSupport.initialize(state.capModel, blockEntity);
        if (state.mounted == null) {
            state.projection = null;
        } else {
            var capPose = new PoseStack();
            applyCapPose(state, capPose);
            Vec3 relative = cameraPosition.subtract(Vec3.atLowerCornerOf(blockEntity.getBlockPos()));
            state.projection = MountedBlockProjection.extract(state.mounted, partialTick, relative,
                state.mounted.state().is(ModBlocks.SMART_BLOCK_PLACER) ? blockEntity.getPlacerDanceTime(partialTick) : null,
                blockEntity, state.facing, state.lightCoords, capPose.last().pose());
        }
    }

    private static void extractButtonState(BigRedButtonBlockEntity blockEntity, BigRedButtonRenderState state, float partialTick) {
        state.facing = blockEntity.getBlockState().getValue(BigRedButtonBlock.FACING);
        var mounted = WheelLifecycleEventListener.previewMountedBlock(blockEntity);
        state.mounted = mounted == null ? null : MountedBlockProjection.displayBlock(mounted, blockEntity.isDisplayPowered());
        state.progress = state.squashes() ? blockEntity.getSquashProgress(partialTick) : blockEntity.getPressProgress(partialTick);
        ButtonCap cap = blockEntity.getCap();
        state.capModel = state.mounted != null || blockEntity.isBareBase() ? null : CAPS.get(cap == null ? blockEntity.getColor() : cap);
    }

    @Override
    public void collectSelectionModels(BigRedButtonBlockEntity blockEntity, float partialTick, PoseStack pose, ModelConsumer consumer) {
        BigRedButtonRenderState state = new BigRedButtonRenderState();
        extractButtonState(blockEntity, state, partialTick);
        withCapPose(state, pose, () -> {
            if (state.mounted == null) {
                if (state.capModel != null) {
                    consumer.accept(state.capModel, pose);
                }
            } else if (state.mounted.state().is(ModBlocks.CHROMATIC_STONE)) {
                consumer.accept(chromaticModel(state.mounted), pose);
            } else {
                var mount = ButtonMount.forBlock(state.mounted.state());
                MountedBlockProjection.fit(state.mounted, blockEntity).apply(pose);
                mount.parts().forEach((pos, block) -> {
                    pose.pushPose();
                    try {
                        Vec3 offset = block.getOffset(blockEntity.getBlockPos().offset(pos));
                        pose.translate(pos.getX() + offset.x, pos.getY() + offset.y, pos.getZ() + offset.z);
                        consumer.accept(block, pose);
                    } finally {
                        pose.popPose();
                    }
                });
            }
        });
    }

    @Override
    public void collectSelectionParts(BigRedButtonBlockEntity button, float partialTick, List<SelectionPart> output) {
        var state = new BigRedButtonRenderState();
        extractButtonState(button, state, partialTick);
        var mounted = state.mounted;
        var level = button.getLevel();
        if (mounted == null || level == null || !MountedBlockProjection.entityOnly(mounted, button)) {
            return;
        }
        var mount = ButtonMount.forMounted(mounted, level.registryAccess());
        var pose = new PoseStack();
        withCapPose(state, pose, () -> {
            for (var part : ModelBlockSelection.shapeParts(mount.outline(Direction.UP, false))) {
                pose.pushPose();
                part.apply(pose);
                output.add(new SelectionPart(part.geometry(), pose.last().pose()));
                pose.popPose();
            }
        });
    }

    @Override
    public AABB getRenderBoundingBox(BigRedButtonBlockEntity button) {
        var state = new BigRedButtonRenderState();
        extractButtonState(button, state, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        AABB base = new AABB(button.getBlockPos());
        if (state.mounted == null) {
            return base;
        }
        var fit = MountedBlockProjection.fit(state.mounted, button);
        var mountedState = state.mounted.state();
        var bounds = new AABB.Builder();
        var pose = new PoseStack();
        for (float progress : new float[]{state.progress, 0, 1}) {
            state.progress = progress;
            withCapPose(state, pose, () -> {
                includeBounds(bounds, fit.bounds(), pose);
                if (mountedState.is(ModBlocks.SMART_BLOCK_PLACER)) {
                    fit.apply(pose);
                    includeBounds(bounds, SmartBlockPlacerRenderer.dancingBounds(mountedState), pose);
                }
            });
        }
        return base.minmax(bounds.build().move(button.getBlockPos()));
    }

    private static void includeBounds(AABB.Builder bounds, AABB box, PoseStack pose) {
        bounds.include(new Vector3f((float) box.minX, (float) box.minY, (float) box.minZ).mulPosition(pose.last().pose()));
        bounds.include(new Vector3f((float) box.maxX, (float) box.maxY, (float) box.maxZ).mulPosition(pose.last().pose()));
    }

    @Override
    public void submit(BigRedButtonRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        withCapPose(state, pose, () -> {
            if (state.projection != null) {
                state.projection.submit(pose, collector, camera, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            } else if (state.model != null) {
                state.model.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
        });
    }

    private static void withCapPose(BigRedButtonRenderState state, PoseStack pose, Runnable draw) {
        pose.pushPose();
        try {
            applyCapPose(state, pose);
            draw.run();
        } finally {
            pose.popPose();
        }
    }

    private static void applyCapPose(BigRedButtonRenderState state, PoseStack pose) {
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(BlockModelRotation.get(MountedBlockView.rotation(state.facing)).transformation().getMatrix());
        pose.translate(-0.5, -0.5, -0.5);
        if (state.squashes()) {
            float width = Mth.lerp(state.progress, 1, ButtonCap.PRESSED_WIDTH_SCALE);
            float height = Mth.lerp(state.progress, 1, ButtonCap.PRESSED_HEIGHT_SCALE);
            pose.translate(0.5, ButtonMount.FLOOR, 0.5);
            pose.scale(width, height, width);
            pose.translate(-0.5, -ButtonMount.FLOOR, -0.5);
        } else {
            pose.translate(0, -state.progress * 2 / 16, 0);
        }
    }
}

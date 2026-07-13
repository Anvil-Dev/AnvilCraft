package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.ControlValveBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.ControlValveRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

public class ControlValveBlockEntityRenderer implements BlockEntityRenderer<ControlValveBlockEntity, ControlValveRenderState> {
    public static final StandaloneModelKey<BlockStateModel> HANDWHEEL =
        new StandaloneModelKey<>(() -> "AnvilCraft: Control Valve Handwheel Model");

    private static final float BASE_ANGLE_DEG = 0.0f;
    private static final float INDICATOR_HALF = 0.0625f;
    private static final float INDICATOR_DEPTH = 0.295f;

    @SuppressWarnings("deprecation")
    private static final RenderType FLUID_RENDER_TYPE = RenderType.create(
        AnvilCraft.of("control_valve_filter").toString(),
        RenderSetup.builder(RenderPipelines.TRANSLUCENT_BLOCK)
            .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
            .useLightmap()
            .sortOnUpload()
            .createRenderSetup()
    );

    @SuppressWarnings("unused")
    public ControlValveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public ControlValveRenderState createRenderState() {
        return new ControlValveRenderState();
    }

    @Override
    public void extractRenderState(
        ControlValveBlockEntity be,
        ControlValveRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = be.getBlockState();
        if (!(blockState.getBlock() instanceof ControlValveBlock)) return;

        state.setFacing(be.getFacing());
        state.setAxis(blockState.getValue(ControlValveBlock.AXIS));
        state.setMaxRate(be.getMaxRate());
        state.setHandwheel(FeatureRendererSupport.initialize(HANDWHEEL, be));

        FluidStack filter = be.getFilter(0);
        state.setFilterResource(filter.isEmpty() ? null : FluidResource.of(filter));

        if (be.isLocked()) {
            this.spawnRedstoneIndicator(be);
        }
    }

    @Override
    public void submit(
        ControlValveRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        this.submitHandwheel(state, poseStack, submitNodeCollector);
        this.submitFluidIndicators(state, poseStack, submitNodeCollector);
    }

    private void submitHandwheel(
        ControlValveRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector
    ) {
        BlockModelRenderState handwheel = state.getHandwheel();
        if (handwheel == null) return;

        float ratio = (ControlValveBlockEntity.MAX_RATE - state.getMaxRate()) / (float) ControlValveBlockEntity.MAX_RATE;
        float spinDeg = BASE_ANGLE_DEG - 90.0f * ratio;
        Direction facing = state.getFacing();
        Direction.Axis axis = state.getAxis();
        if (axis == Direction.Axis.Z || (axis == Direction.Axis.Y && facing.getAxis() == Direction.Axis.Z)) {
            spinDeg += 90.0f;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        applyUpToFacing(poseStack, facing);
        poseStack.mulPose(Axis.YP.rotationDegrees(spinDeg));
        poseStack.translate(-0.5, -0.5, -0.5);
        handwheel.submit(
            poseStack,
            submitNodeCollector,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            0
        );
        poseStack.popPose();
    }

    private void submitFluidIndicators(
        ControlValveRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector
    ) {
        FluidResource resource = state.getFilterResource();
        if (resource == null || resource.isEmpty()) return;

        FluidModel model = FluidRenderHelper.getModel(
            Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
            resource.getFluid()
        );
        var tintSource = model.fluidTintSource();
        if (tintSource == null) return;
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        int tintColor = tintSource.colorAsStack(resource.toStack(1));

        float h = INDICATOR_HALF;
        float y = INDICATOR_DEPTH;
        for (Direction side : Direction.values()) {
            if (side.getAxis() == state.getAxis() || side == state.getFacing()) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            applyUpToFacing(poseStack, side);
            submitNodeCollector.submitCustomGeometry(
                poseStack,
                FLUID_RENDER_TYPE,
                (pose, buffer) -> FluidRenderHelper.INSTANCE.renderFluidBox(
                    sprite,
                    resource,
                    -h,
                    y - h,
                    -h,
                    h,
                    y + h,
                    h,
                    tintColor,
                    buffer,
                    pose,
                    state.lightCoords,
                    true,
                    false
                )
            );
            poseStack.popPose();
        }
    }

    private void spawnRedstoneIndicator(ControlValveBlockEntity be) {
        Level level = be.getLevel();
        if (level == null || !level.isClientSide()) return;
        RandomSource random = level.getRandom();
        if (level.getGameTime() % 20 != 0 || random.nextFloat() >= 0.3f) return;

        Direction facing = be.getFacing();
        BlockPos pos = be.getBlockPos();
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.425;
        double y = pos.getY() + 0.5 + facing.getStepY() * 0.425;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.425;
        level.addParticle(
            DustParticleOptions.REDSTONE,
            x + (random.nextDouble() - 0.5) * 0.15,
            y + (random.nextDouble() - 0.5) * 0.15,
            z + (random.nextDouble() - 0.5) * 0.15,
            0,
            0.01,
            0
        );
    }

    @Override
    public AABB getRenderBoundingBox(ControlValveBlockEntity blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), 2, 2, 2);
    }

    private static void applyUpToFacing(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            default -> poseStack.mulPose(Axis.XP.rotationDegrees(0));
        }
    }
}

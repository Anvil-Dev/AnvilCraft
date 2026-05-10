package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.client.event.ClientTickRecorder;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FishTankRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class FishTankRenderer extends BaseFluidHandlerHolderRenderer<FishTankBlockEntity, FishTankRenderState> {
    public static final StandaloneModelKey<BlockStateModel> FIRE = new StandaloneModelKey<>(
        () -> "AnvilCraft: Fish Tank Fire Model"
    );
    private static final float TANK_W = 1 / 16F + 0.001F; // avoiding Z-fighting
    private final RandomSource random = RandomSource.create();
    private final ItemModelResolver resolver;

    public FishTankRenderer(BlockEntityRendererProvider.Context ctx) {
        this.resolver = ctx.itemModelResolver();
    }

    @Override
    public FishTankRenderState createRenderState() {
        return new FishTankRenderState();
    }

    @Override
    protected void updateTankW(
        FishTankBlockEntity be,
        FishTankRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        state.setTankW(TANK_W);
    }

    @Override
    public void extractRenderState(
        FishTankBlockEntity be,
        FishTankRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.setIgnited(be.isIgnited());
        for (ItemStack stack : ItemHandlerUtil.getNonEmptyItemsFromHandler(be.getItemHandler())) {
            state.getStacks().put(stack, FeatureRendererSupport.initialize(stack, this.resolver));
        }
        state.setFire(FeatureRendererSupport.initialize(FishTankRenderer.FIRE, be));
    }

    @Override
    public void submit(FishTankRenderState state, PoseStack pose, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, pose, submitNodeCollector, camera);
        if (state.isIgnited()) {
            pose.pushPose();
            pose.translate(0, state.getMaxY() - (1 - TANK_W), 0);
            state.getFire().submit(
                pose,
                submitNodeCollector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
            );
            pose.popPose();
        }
        if (state.getStacks().isEmpty()) return;
        FishTankRenderer.submitItemsInTank(
            state.getStacks(),
            pose,
            submitNodeCollector,
            this.random,
            state.getFill(),
            state.lightCoords
        );
    }

    // Thanks for Create Mod, logics in this method are mostly from it.
    private static void submitItemsInTank(
        Map<ItemStack, ItemClusterRenderState> items,
        PoseStack pose,
        SubmitNodeCollector collector,
        RandomSource random,
        float fill,
        int light
    ) {
        final float randomOffsetDeg = random.nextIntBetweenInclusive(0, 50) - 25;

        pose.pushPose();
        pose.translate(0.5F, TANK_W, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(randomOffsetDeg));

        int itemCount = items.size();
        float y = Mth.clamp(fill - TANK_W - 1 / 8F, TANK_W, 1 - TANK_W - 1 / 8F);
        float partAngleDeg = 360F / itemCount;
        Vec3 vec = itemCount == 1 ? new Vec3(0, y, 0) : new Vec3(0.125, y, 0);
        for (Map.Entry<ItemStack, ItemClusterRenderState> entry : items.entrySet()) {
            final ItemStack stack = entry.getKey();
            final ItemClusterRenderState cluster = entry.getValue();
            pose.pushPose();

            if (fill > 0) {
                pose.translate(
                    0,
                    (Mth.sin(ClientTickRecorder.getTicks() / 12F + partAngleDeg * itemCount) + 1.5F) * 1 / 32F,
                    0
                );
            }

            float angle = Mth.DEG_TO_RAD * (partAngleDeg * itemCount);
            double sin = Mth.sin(angle);
            double cos = Mth.cos(angle);
            pose.translate(vec.x * cos + vec.z * sin, vec.y, vec.z * cos - vec.x * sin);
            pose.mulPose(
                new Quaternionf()
                    .rotateY(Mth.DEG_TO_RAD * (partAngleDeg * itemCount + 35))
                    .rotateX(Mth.DEG_TO_RAD * 65)
            );
            for (int i = 0; i <= stack.getCount() / 8; i++) {
                pose.pushPose();

                float radius = 1 / 16F;
                pose.translate(
                    0 + (random.nextFloat() - 0.5F) * 2 * radius,
                    0 + (random.nextFloat() - 0.5F) * 2 * radius,
                    0 + (random.nextFloat() - 0.5F) * 2 * radius
                );
                cluster.item.submit(
                    pose,
                    collector,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    0
                );
                pose.popPose();
            }
            pose.popPose();

            itemCount--;
        }
        pose.popPose();
    }
}

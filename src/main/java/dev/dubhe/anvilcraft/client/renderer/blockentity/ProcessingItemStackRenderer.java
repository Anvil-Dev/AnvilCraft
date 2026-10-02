package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.item.CheckValveItem;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.ProcessingTableRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.selection.SelectionModel;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import dev.dubhe.anvilcraft.item.block.PipeBlockItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ProcessingItemStackRenderer<T extends BlockEntity & IItemResourceHandlerHolder>
    implements BlockEntityRenderer<T, ProcessingTableRenderState>, ModelSelectionRenderer<T> {
    private final ItemModelResolver resolver;
    private final RandomSource random = RandomSource.create();

    protected ProcessingItemStackRenderer(BlockEntityRendererProvider.Context context) {
        this.resolver = context.itemModelResolver();
    }

    @Override
    public ProcessingTableRenderState createRenderState() {
        return new ProcessingTableRenderState();
    }

    protected boolean isBlockStateRenderEnabled() {
        return true;
    }

    protected boolean isBlockStateRenderBlocked(T table) {
        return false;
    }

    protected float getItemBaseXRotationDeg() {
        return 1;
    }

    protected float progress(T table, float partialTick) {
        return 0;
    }

    protected List<StandaloneModelKey<BlockStateModel>> models() {
        return List.of();
    }

    protected void visitModels(float progress, PoseStack pose, ModelConsumer consumer) {
    }

    @Override
    public void collectSelectionModels(T table, float partialTick, PoseStack pose, ModelConsumer consumer) {
        this.visitModels(this.progress(table, partialTick), pose, consumer);
    }

    @Override
    public void collectPreviewModels(T table, float partialTick, PoseStack pose, ModelConsumer consumer) {
        this.visitModels(0, pose, consumer);
    }

    @Override
    public void extractRenderState(
        T table, ProcessingTableRenderState state, float partialTick,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTick, cameraPosition, breakProgress);
        state.progress = this.progress(table, partialTick);
        state.models.clear();
        for (var model : this.models()) state.models.put(SelectionModel.standalone(model), FeatureRendererSupport.initialize(model, table));
        state.itemCount = 0;
        if (table.getLevel() == null) return;
        List<ItemStack> items = ItemHandlerUtil.getNonEmptyItemsFromHandler(table.getItemHandler());
        state.itemCount = items.size();
        while (state.items.size() < items.size()) state.items.add(new ProcessingTableRenderState.DisplayedItem());
        Set<Item> blockKinds = new HashSet<>();
        for (int index = 0; index < items.size(); index++) {
            ItemStack stack = items.get(index);
            var displayed = state.items.get(index);
            displayed.poseCount = 0;
            this.resolver.updateForTopItem(displayed.item, stack, ItemDisplayContext.GROUND, table.getLevel(), null, 0);
            displayed.blockModel = isBlockItem(stack) && displayed.item.isThreeDimensional();
            if (displayed.blockModel) blockKinds.add(stack.getItem());
        }
        if (items.isEmpty()) return;
        this.random.setSeed(ItemHandlerUtil.hash(table.getItemHandler()));
        float randomOffset = this.random.nextIntBetweenInclusive(0, 50) - 25;
        float partAngle = 360F / items.size();
        boolean enlarge = blockKinds.size() == 1 && this.isBlockStateRenderEnabled() && !this.isBlockStateRenderBlocked(table);
        var pose = new PoseStack();
        pose.translate(0.5F, 0.8F, 0.5F);
        for (int index = 0; index < items.size(); index++) {
            var displayed = state.items.get(index);
            pose.pushPose();
            if (displayed.blockModel && enlarge) {
                pose.translate(0, -0.25F, 0);
                pose.scale(2.8F, 2.8F, 2.8F);
                displayed.addPose(pose.last().pose());
            } else {
                int remaining = items.size() - index;
                pose.mulPose(Axis.YP.rotationDegrees(randomOffset));
                float angle = Mth.DEG_TO_RAD * (partAngle * remaining);
                float radius = items.size() == 1 ? 0 : 0.125F;
                pose.translate(radius * Mth.cos(angle), 0, -radius * Mth.sin(angle));
                pose.mulPose(new Quaternionf().rotateY(Mth.DEG_TO_RAD * (partAngle * remaining + 35))
                    .rotateX(Mth.DEG_TO_RAD * this.getItemBaseXRotationDeg()));
                for (int layer = 0; layer <= items.get(index).getCount() / 8; layer++) {
                    pose.pushPose();
                    pose.translate((this.random.nextFloat() - 0.5F) / 8F,
                        (this.random.nextFloat() - 0.5F) / 8F, (this.random.nextFloat() - 0.5F) / 8F);
                    displayed.addPose(pose.last().pose());
                    pose.popPose();
                }
            }
            pose.popPose();
        }
    }

    private static boolean isBlockItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem || stack.getItem() instanceof PipeBlockItem
            || stack.getItem() instanceof CheckValveItem;
    }

    @Override
    public void submit(ProcessingTableRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        this.visitModels(state.progress, pose, (model, partPose) -> state.models.get(model)
            .submitModel(ModRenderTypes.CUTOUT_BLOCK, partPose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0));
        for (int index = 0; index < state.itemCount; index++) {
            var displayed = state.items.get(index);
            for (int copy = 0; copy < displayed.poseCount; copy++) {
                pose.pushPose();
                pose.mulPose(displayed.poses.get(copy));
                displayed.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
        }
    }
}

package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.client.selection.ModelPartSelection;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class AutoEnchantingTableBlockEntityRenderer
    implements BlockEntityRenderer<AutoEnchantingTableBlockEntity, AutoEnchantingTableBlockEntityRenderer.State>,
    ModelSelectionRenderer<AutoEnchantingTableBlockEntity> {
    private static final float[][] FLUID_BOXES = {{0.375F, 0, 0.625F, 0.125F}, {0, 0.375F, 0.125F, 0.625F},
        {0.375F, 0.875F, 0.625F, 1}, {0.875F, 0.375F, 1, 0.625F}};
    private final SpriteGetter sprites;
    private final BookModel bookModel;
    private final ModelPart bookRoot;
    private final ModelPartSelection bookSelection = new ModelPartSelection();
    private final Map<AutoEnchantingTableBlockEntity, BookPose> poses = new WeakHashMap<>();

    public AutoEnchantingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.bookRoot = context.bakeLayer(ModelLayers.BOOK);
        this.bookModel = new BookModel(this.bookRoot);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AutoEnchantingTableBlockEntity entity, State state, float partialTick,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breaking);
        state.book = this.bookPose(entity, partialTick);
        var item = entity.getItem(AutoEnchantingTableBlockEntity.SLOT_INPUT);
        if (item.isEmpty()) item = entity.getItem(AutoEnchantingTableBlockEntity.SLOT_OUTPUT);
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
            state.item, item, ItemDisplayContext.FIXED, entity.getLevel(), null, 0);
        state.fluid = entity.getFluidTank().getResource(0);
        state.fill = Mth.clamp((float) entity.getFluidTank().getAmountAsInt(0) / AutoEnchantingTableBlockEntity.FLUID_CAPACITY, 0, 1);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        applyBookPose(pose, state.book);
        collector.submitModel(this.bookModel, BookModel.State.forAnimation(state.book.time(), 0, 0, state.book.open()),
            pose, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, EnchantTableRenderer.BOOK_TEXTURE,
            this.sprites, 0, state.breakProgress);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.5, state.book.height(), 0.5);
        pose.mulPose(Axis.YP.rotation(state.book.time() * 0.02F));
        pose.scale(0.4F, 0.4F, 0.4F);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        if (state.fluid.isEmpty() || state.fill <= 0) return;
        var model = FluidRenderHelper.getModel(Minecraft.getInstance().getModelManager().getFluidStateModelSet(), state.fluid.getFluid());
        int color = model.fluidTintSource() == null ? -1 : model.fluidTintSource().colorAsStack(state.fluid.toStack(1));
        var sprite = model.stillMaterial().sprite();
        collector.submitCustomGeometry(pose, BaseFluidHandlerHolderRenderer.FLUID_RENDER_TYPE, (fluidPose, vertices) -> {
            float minY = 0.3135F;
            float maxY = 0.3115F + 0.375F * state.fill;
            for (float[] box : FLUID_BOXES) {
                FluidRenderHelper.INSTANCE.renderFluidBox(sprite, state.fluid,
                    box[0] + 0.001F, minY, box[1] + 0.001F, box[2] - 0.001F, maxY, box[3] - 0.001F,
                    color, vertices, fluidPose, state.lightCoords, true, false);
            }
        });
    }

    @Override
    public void collectSelectionParts(AutoEnchantingTableBlockEntity entity, float partialTick, List<SelectionPart> output) {
        if (entity.getLevel() == null) return;
        var book = this.bookPose(entity, partialTick);
        PoseStack pose = new PoseStack();
        applyBookPose(pose, book);
        this.bookModel.setupAnim(BookModel.State.forAnimation(book.time(), 0, 0, book.open()));
        this.bookSelection.collect(this.bookRoot, pose, output);
    }

    private BookPose bookPose(AutoEnchantingTableBlockEntity entity, float partialTick) {
        var previous = this.poses.get(entity);
        if (previous != null && previous.frame() == ModelBlockSelection.frame()) return previous;
        boolean input = !entity.getItem(AutoEnchantingTableBlockEntity.SLOT_INPUT).isEmpty();
        float oldOpen = entity.getBookOpen();
        float oldHeight = entity.getBookHeight();
        entity.setBookOpen(Mth.approach(oldOpen, input ? 1 : 0, 0.06F));
        if (input || !entity.getItem(AutoEnchantingTableBlockEntity.SLOT_OUTPUT).isEmpty()) {
            entity.setBookHeight(Mth.approach(oldHeight, input ? 1 : 1.3F, 0.06F));
        }
        float time = entity.getLevel() == null ? 0 : entity.getLevel().getGameTime() + partialTick;
        var result = new BookPose(ModelBlockSelection.frame(), time, Mth.lerp(partialTick, oldOpen, entity.getBookOpen()),
            Mth.lerp(partialTick, oldHeight, entity.getBookHeight()));
        this.poses.put(entity, result);
        return result;
    }

    private static void applyBookPose(PoseStack pose, BookPose book) {
        pose.translate(0.5, 0.725 + Mth.sin(book.time() * 0.1F) * 0.01F, 0.5);
        pose.mulPose(Axis.YP.rotation(-book.time() * 0.02F));
        pose.mulPose(Axis.ZP.rotationDegrees(80));
    }

    @Override
    public AABB getRenderBoundingBox(AutoEnchantingTableBlockEntity entity) {
        var pos = entity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
    }

    private record BookPose(long frame, float time, float open, float height) {
    }

    public static final class State extends BlockEntityRenderState {
        private BookPose book = new BookPose(-1, 0, 0, 0);
        private final ItemStackRenderState item = new ItemStackRenderState();
        private FluidResource fluid = FluidResource.EMPTY;
        private float fill;
    }
}

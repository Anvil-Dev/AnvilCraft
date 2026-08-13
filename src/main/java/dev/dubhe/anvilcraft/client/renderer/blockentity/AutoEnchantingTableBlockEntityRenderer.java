package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
public class AutoEnchantingTableBlockEntityRenderer implements BlockEntityRenderer<AutoEnchantingTableBlockEntity> {
    @SuppressWarnings("deprecation")
    private static final Material BOOK_LOCATION = new Material(
        TextureAtlas.LOCATION_BLOCKS,
        ResourceLocation.withDefaultNamespace("entity/enchanting_table_book")
    );

    private final BookModel bookModel;
    private final ItemRenderer itemRenderer;

    public AutoEnchantingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
        AutoEnchantingTableBlockEntity be,
        float partialTick,
        PoseStack pose,
        MultiBufferSource ms,
        int packedLight,
        int packedOverlay
    ) {
        Level level = be.getLevel();
        if (level == null) return;
        ItemStack input = be.getItemHandler().getStackInSlot(0);
        ItemStack output = be.getItemHandler().getStackInSlot(1);
        boolean hasInput = !input.isEmpty();
        boolean hasOutput = !output.isEmpty();

        // 书本展开程度：输入格有物品时展开，否则闭合
        float targetOpen = hasInput ? 1.0F : 0.0F;
        float bookOpenO = be.getBookOpen();
        be.setBookOpen(Mth.approach(bookOpenO, targetOpen, 0.06F));
        float open = Mth.lerp(partialTick, bookOpenO, be.getBookOpen());

        float time = (float) level.getGameTime() + partialTick;
        // 待机缓慢旋转（与原版附魔台玩家远离时一致的速度）
        float bookRot = time * 0.02F;

        this.renderBook(pose, ms, packedLight, packedOverlay, time, open, bookRot);

        ItemStack item = hasInput ? input : output;
        if (!item.isEmpty()) {
            this.renderItem(level, item, pose, ms, packedLight, packedOverlay, bookRot);
        }

        // 容器内流体：从底部按比例填充，四个侧面可见
        FluidStack fluid = be.getFluidHandler().getFluidInTank(0);
        if (!fluid.isEmpty()) {
            float fill = Mth.clamp(
                (float) fluid.getAmount() / be.getFluidHandler().getTankCapacity(0), 0.0F, 1.0F);
            this.renderFluid(fluid, pose, ms, fill, packedLight);
        }
    }

    private void renderBook(
        PoseStack pose,
        MultiBufferSource ms,
        int packedLight,
        int packedOverlay,
        float time,
        float open,
        float bookRot
    ) {
        pose.pushPose();
        // 方块高度为 12/16，书本中心放在台面附近
        pose.translate(0.5, 0.625, 0.5);
        pose.translate(0.0, 0.1 + Mth.sin(time * 0.1F) * 0.01F, 0.0);
        pose.mulPose(Axis.YP.rotation(-bookRot));
        pose.mulPose(Axis.ZP.rotationDegrees(80.0F));
        this.bookModel.setupAnim(time, 0.0F, 0.0F, open);
        VertexConsumer vertexConsumer = BOOK_LOCATION.buffer(ms, RenderType::entityCutout);
        this.bookModel.render(pose, vertexConsumer, packedLight, packedOverlay, -1);
        pose.popPose();
    }

    private void renderItem(
        Level level,
        ItemStack stack,
        PoseStack pose,
        MultiBufferSource ms,
        int packedLight,
        int packedOverlay,
        float bookRot
    ) {
        pose.pushPose();
        pose.translate(0.5, 1.0, 0.5);
        // 与书本反向同速旋转
        pose.mulPose(Axis.YP.rotation(bookRot));
        pose.scale(0.4F, 0.4F, 0.4F);
        this.itemRenderer.renderStatic(
            stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, pose, ms, level, 0);
        pose.popPose();
    }

    private void renderFluid(FluidStack stack, PoseStack pose, MultiBufferSource ms, float fill, int packedLight) {
        float minY = 0.3125F;
        float maxY = minY + 0.375F * fill;
        this.renderSingleFluid(stack, pose, ms, 0.375F, minY, 0.0F, 0.625F, maxY, 0.125F, packedLight);
        this.renderSingleFluid(stack, pose, ms, 0.0F, minY, 0.375F, 0.125F, maxY, 0.625F, packedLight);
        this.renderSingleFluid(stack, pose, ms, 0.375F, minY, 0.875F, 0.625F, maxY, 1.0F, packedLight);
        this.renderSingleFluid(stack, pose, ms, 0.875F, minY, 0.375F, 1.0F, maxY, 0.625F, packedLight);
    }

    private void renderSingleFluid(
        FluidStack stack,
        PoseStack pose,
        MultiBufferSource ms,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        int packedLight
    ) {
        FluidRenderHelper.INSTANCE.renderFluidBox(
            stack,
            minX + 0.001F,
            minY + 0.001F,
            minZ + 0.001F,
            maxX - 0.001F,
            maxY - 0.001F,
            maxZ - 0.001F,
            ms,
            pose,
            packedLight,
            true,
            false
        );
    }

    @Override
    public AABB getRenderBoundingBox(AutoEnchantingTableBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            pos.getX() + 1.0,
            pos.getY() + 1.5,
            pos.getZ() + 1.0
        );
    }
}

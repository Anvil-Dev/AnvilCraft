package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.itemhandler.PollableItemHandler;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.client.event.ClientTickRecorder;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.joml.Quaternionf;

import java.util.List;

public class FishTankBlockEntityRenderer implements BlockEntityRenderer<FishTankBlockEntity> {
    private final RandomSource random = RandomSource.create();

    public FishTankBlockEntityRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public void render(
        FishTankBlockEntity tank,
        float partialTick,
        PoseStack pose,
        MultiBufferSource source,
        int light,
        int overlay
    ) {
        FluidTank fluid = tank.getFluidHandler();
        float minY = TANK_W;
        float maxY = TANK_W;
        float fill = 0;
        if (!fluid.isEmpty()) {
            fill = Math.min((float) fluid.getFluidAmount() / fluid.getCapacity(), 1);

            // Top and bottom positions of the fluid inside the tank
            float height = 1 - 2 * TANK_W;

            minY = TANK_W;
            maxY = minY + height;
        }

        PollableItemHandler handler = tank.getItemHandler();
        this.random.setSeed(ItemHandlerUtil.hash(handler));
        FishTankBlockEntityRenderer.drawItemsInTank(
            tank.getLevel(),
            ItemHandlerUtil.getNonEmptyItemsFromHandler(handler),
            fill,
            Minecraft.getInstance().getItemRenderer(),
            pose,
            source,
            this.random,
            light,
            overlay
        );
        FishTankBlockEntityRenderer.drawFluidInTank(pose, source, light, fluid, minY, maxY);
    }

    private static final float TANK_W = 1 / 16F + 0.001F; // avoiding Z-fighting

    // Thanks for Create Mod, logics in this method are mostly from it.
    private static void drawItemsInTank(
        Level level,
        List<ItemStack> items,
        float fill,
        ItemRenderer renderer,
        PoseStack pose,
        MultiBufferSource source,
        RandomSource random,
        int light,
        int overlay
    ) {
        if (items.isEmpty()) return;
        final float randomOffsetDeg = random.nextIntBetweenInclusive(0, 50) - 25;

        pose.pushPose();
        pose.translate(0.5F, TANK_W, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(randomOffsetDeg));

        int itemCount = items.size();
        float y = Mth.clamp(fill - TANK_W - 1 / 8F, TANK_W, 1 - TANK_W - 1 / 8F);
        float partAngleDeg = 360F / itemCount;
        Vec3 vec = itemCount == 1 ? new Vec3(0, y, 0) : new Vec3(0.125, y, 0);
        for (ItemStack stack : items) {
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
                renderer.renderStatic(
                    stack,
                    ItemDisplayContext.GROUND,
                    light,
                    overlay,
                    pose,
                    source,
                    level,
                    0
                );

                pose.popPose();
            }
            pose.popPose();

            itemCount--;
        }
        pose.popPose();
        if (source instanceof MultiBufferSource.BufferSource buffer) buffer.endBatch();
    }

    private static void drawFluidInTank(PoseStack pose, MultiBufferSource source, int light, FluidTank fluid, float minY, float maxY) {
        if (fluid.isEmpty()) return;
        FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid.getFluid(),
            TANK_W,
            minY,
            TANK_W,
            1 - TANK_W,
            maxY,
            1 - TANK_W,
            source,
            pose,
            light,
            true,
            false
        );
        if (source instanceof MultiBufferSource.BufferSource buffer) buffer.endBatch();
    }
}

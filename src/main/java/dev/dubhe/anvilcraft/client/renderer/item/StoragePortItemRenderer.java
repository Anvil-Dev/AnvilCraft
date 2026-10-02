package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.tooltip.StoragePortItemTooltip;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientStoragePortTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/** 外壳由组合模型中的普通模型绘制，此层仅添加六面标记。 */
public class StoragePortItemRenderer implements SpecialModelRenderer<StoragePortItemRenderer.Argument> {
    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        ItemStack marked = StoragePortItemTooltip.markedItem(StoragePortItemTooltip.blockEntityTag(stack),
            ClientStoragePortTooltip.registries());
        if (marked.isEmpty()) return null;
        TrackingItemStackRenderState state = new TrackingItemStackRenderState();
        Minecraft client = Minecraft.getInstance();
        client.getItemModelResolver().updateForTopItem(state, marked, ItemDisplayContext.FIXED, client.level, null, 0);
        return new Argument(state, state.isAnimated() ? new Object() : state.getModelIdentity());
    }

    @Override
    public void submit(
        @Nullable Argument argument, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
        boolean hasFoil, int outlineColor
    ) {
        if (argument == null) return;
        for (Direction direction : Direction.values()) {
            pose.pushPose();
            pose.translate(0.5 + direction.getStepX() * 0.4, 0.5 + direction.getStepY() * 0.4, 0.5 + direction.getStepZ() * 0.4);
            pose.scale(0.8F, 0.8F, 0.8F);
            if (direction.getAxis() == Direction.Axis.X) {
                pose.mulPose(Axis.YP.rotationDegrees(90));
            } else if (direction.getAxis() == Direction.Axis.Y) {
                pose.mulPose(Axis.XP.rotationDegrees(90));
                if (direction == Direction.UP) pose.mulPose(Axis.ZP.rotationDegrees(180));
            }
            argument.item().submit(pose, collector, light, overlay, outlineColor);
            pose.popPose();
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) output.accept(new Vector3f(x, y, z));
            }
        }
    }

    /** 静态标记复用原生模型身份，动画标记每帧刷新，避免图集缓存冻结动画。 */
    public record Argument(TrackingItemStackRenderState item, Object identity) {
        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof Argument argument && this.identity.equals(argument.identity);
        }

        @Override
        public int hashCode() {
            return this.identity.hashCode();
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Argument> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public StoragePortItemRenderer bake(BakingContext context) {
            return new StoragePortItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}

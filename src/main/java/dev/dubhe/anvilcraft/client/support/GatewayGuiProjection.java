package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.client.renderer.item.CelestialForgingAnvilItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** 保留物品图集和 PIP 渲染，在提交折跃门顶点时还原完整界面的投影坐标。 */
public final class GatewayGuiProjection {
    private static @Nullable GuiItemRenderState item;
    private static @Nullable Matrix4f projection;

    private GatewayGuiProjection() {
    }

    public static boolean isProjected(TrackingItemStackRenderState state) {
        if (!(state.getModelIdentity() instanceof List<?> elements)) return false;
        for (Object element : elements) {
            if (element instanceof CelestialForgingAnvilItemRenderer.Argument argument
                && argument.state().getEffectiveBodyData() instanceof SpecialCelestialBodyData special
                && special.usesEndGatewayModel()) return true;
        }
        return false;
    }

    public static void mark(TrackingItemStackRenderState state, Matrix3x2f pose, int x, int y) {
        if (state.getModelIdentity() instanceof List<?> elements) {
            for (Object element : elements) {
                if (element instanceof CelestialForgingAnvilItemRenderer.Argument) {
                    state.setAnimated();
                    break;
                }
            }
        }
        if (isProjected(state)) state.appendModelIdentityElement(new Location(new Matrix3x2f(pose), x, y));
    }

    public static <T extends @Nullable Object> T withItem(GuiItemRenderState current, Supplier<T> action) {
        GuiItemRenderState previous = item;
        item = current;
        try {
            return action.get();
        } finally {
            item = previous;
        }
    }

    public static void withAtlas(int textureSize, int slotSize, int slotX, int slotY, Runnable action) {
        if (item == null) {
            action.run();
            return;
        }
        Matrix4f mapping = gui(item.pose()).translate(item.x(), item.y(), 0)
            .scale(16.0F / slotSize, 16.0F / slotSize, 1)
            .translate(-slotX * slotSize, -slotY * slotSize, 0)
            .translate(0, textureSize, 0).scale(textureSize, -textureSize, 1);
        withProjection(mapping, action);
    }

    public static void withProjection(Matrix4f current, Runnable action) {
        Matrix4f previous = projection;
        projection = current;
        try {
            action.run();
        } finally {
            projection = previous;
        }
    }

    public static @Nullable Matrix4f current() {
        return projection;
    }

    public static Matrix4f pip(PictureInPictureRenderState state) {
        return gui(state.pose()).translate(state.x0(), state.y1(), 0)
            .scale(state.x1() - state.x0(), state.y0() - state.y1(), 1);
    }

    private static Matrix4f gui(Matrix3x2fc transform) {
        var window = Minecraft.getInstance().getWindow();
        Matrix4f pose = new Matrix4f().m00(transform.m00()).m01(transform.m01())
            .m10(transform.m10()).m11(transform.m11()).m30(transform.m20()).m31(transform.m21());
        return new Matrix4f().translation(0, 1, 0)
            .scale(1.0F / window.getGuiScaledWidth(), -1.0F / window.getGuiScaledHeight(), 1).mul(pose);
    }

    private record Location(Matrix3x2f pose, int x, int y) {
    }
}

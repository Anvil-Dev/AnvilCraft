package dev.dubhe.anvilcraft.client.renderer.item;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 物品栏裁剪：把渲染尺寸超过物品格子的物品模型裁剪到格子范围内。
 *
 * <p>通过 {@link #register(Item...)} 或 {@link #register(Predicate)} 配置需要裁剪的物品；
 * 渲染时把绘制限制在以物品实际屏幕位置为左上角、边长 {@value #SLOT_SIZE} 的物品格内，
 * 超出部分被裁掉。
 *
 * <p>物品的 {@code (x, y)} 可能是<b>姿态局部坐标</b>（如容器屏把姿态平移了
 * {@code leftPos/topPos} 后才渲染槽位），因此开启裁剪时用姿态矩阵把坐标换算到
 * <b>绝对屏幕坐标</b>，否则裁剪窗口会与物品实际位置错位。
 */
public class ItemSlotClipping {
    /** 标准物品格边长（px）。 */
    public static final int SLOT_SIZE = 16;

    private static final Set<Item> CLIPPED_ITEMS = new HashSet<>();
    private static final List<Predicate<ItemStack>> CLIPPED_PREDICATES = new ArrayList<>();
    private static final ThreadLocal<Boolean> CLIPPING_DISABLED = ThreadLocal.withInitial(() -> false);

    private ItemSlotClipping() {
    }

    /** 注册需要裁剪到物品格内的物品。 */
    public static void register(Item... items) {
        for (Item item : items) {
            CLIPPED_ITEMS.add(item);
        }
    }

    /** 注册需要裁剪的自定义判定（例如按物品标签、物品类型等）。 */
    public static void register(Predicate<ItemStack> predicate) {
        CLIPPED_PREDICATES.add(predicate);
    }

    /** 该物品堆当前是否会被物品格裁剪。 */
    public static boolean shouldClip(ItemStack stack) {
        if (CLIPPING_DISABLED.get()) {
            return false;
        }
        if (stack.isEmpty()) {
            return false;
        }
        if (CLIPPED_ITEMS.contains(stack.getItem())) {
            return true;
        }
        for (Predicate<ItemStack> predicate : CLIPPED_PREDICATES) {
            if (predicate.test(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在 {@code render} 执行期间临时禁用物品格裁剪，
     * 用于轮盘等不以物品格为边界的渲染场景。
     */
    public static void runWithoutClip(Runnable render) {
        boolean previous = CLIPPING_DISABLED.get();
        CLIPPING_DISABLED.set(true);
        try {
            render.run();
        } finally {
            CLIPPING_DISABLED.set(previous);
        }
    }

    /**
     * 开启对 {@code (x, y)} 处物品的裁剪（姿态局部坐标 → 绝对屏幕坐标换算）。
     * 需与 {@link #disableClip} 配对调用。
     */
    public static void enableClip(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        if (!shouldClip(stack)) {
            return;
        }
        Vector3f screenPos = new Vector3f();
        guiGraphics.pose().last().pose().transformPosition(x, y, 0.0f, screenPos);
        int screenX = Math.round(screenPos.x());
        int screenY = Math.round(screenPos.y());
        guiGraphics.enableScissor(screenX, screenY, screenX + SLOT_SIZE, screenY + SLOT_SIZE);
    }

    /** 关闭由 {@link #enableClip} 开启的裁剪。 */
    public static void disableClip(GuiGraphics guiGraphics) {
        guiGraphics.disableScissor();
    }

    /**
     * 包裹一次物品渲染：仅当 {@code stack} 已注册裁剪时，把 {@code render} 的绘制限制在
     * 物品格范围内；否则原样执行。
     */
    public static void clip(GuiGraphics guiGraphics, ItemStack stack, int x, int y, Runnable render) {
        if (!shouldClip(stack)) {
            render.run();
            return;
        }
        enableClip(guiGraphics, stack, x, y);
        render.run();
        disableClip(guiGraphics);
    }
}

package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;

/**
 * EMI 对仓储界面的适配（见 #4720）。
 *
 * <p>EMI 对 {@code HandledScreen} 的兜底布局使用容器的
 * {@code leftPos/topPos/背景宽高} 推断界面边界；仓储界面（300x222）在屏幕居中绘制，
 * leftPos 很大，导致 EMI 把侧栏/面板按这个偏大的矩形布局到屏幕中部而非屏幕边缘。
 * 这里通过 EMI 官方 API 为 {@link StorageScreen} 注册全屏边界，使面板按整个屏幕布局。</p>
 *
 * <p>类标注 EMI 的 {@link EmiEntrypoint}（EMI 加载后扫描并调用 {@link #register}）。</p>
 */
@EmiEntrypoint
public class AnvilCraftEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        // noinspection UnstableApiUsage
        registry.addScreenBoundsProvider(StorageScreen.class, screen ->
            new Bounds(0, 0, screen.width, screen.height)
        );
    }
}

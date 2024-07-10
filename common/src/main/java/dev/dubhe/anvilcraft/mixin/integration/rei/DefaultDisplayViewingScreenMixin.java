package dev.dubhe.anvilcraft.mixin.integration.rei;

import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen;
import me.shedaniel.rei.impl.client.gui.screen.DefaultDisplayViewingScreen;
import me.shedaniel.rei.impl.display.DisplaySpec;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = DefaultDisplayViewingScreen.class, remap = false)
public abstract class DefaultDisplayViewingScreenMixin extends AbstractDisplayViewingScreen {
    protected DefaultDisplayViewingScreenMixin(
        Map<DisplayCategory<?>, List<DisplaySpec>> categoryMap, @Nullable CategoryIdentifier<?> category
    ) {
        super(categoryMap, category);
    }

    @Shadow
    public abstract List<DisplaySpec> getCurrentDisplayed();

    /**
     * @reason 我们需要重定向修改原guiWidth的函数以实现修改rei获取ui宽度的代码, 以适配较宽的配方,
     *     如果未来rei恢复了原逻辑, 我们会弃用改mixin. 此mixin作者有尝试使用MixinExtras修改局部变量, 但是并未成功.
     */
    @Redirect(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;max(II)I"
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Ljava/util/List;clear()V"
            ),
            to = @At(
                value = "INVOKE",
                target = "Lme/shedaniel/rei/impl/client/gui/widget/TabContainerWidget;initTabsSize(I)V",
                remap = false
            )
        ),
        remap = false
    )
    private int init(int a, int b) {
        int maxWidthDisplay = CollectionUtils.<DisplaySpec, Integer>mapAndMax(
            this.getCurrentDisplayed(),
            display -> this.getCurrentCategory().getDisplayWidth(display.provideInternalDisplay()),
            Comparator.naturalOrder()
        ).orElse(150);
        return Math.max(maxWidthDisplay + 10 + 14 + 14, 190);
    }
}

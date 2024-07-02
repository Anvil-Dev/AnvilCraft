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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Mixin(DefaultDisplayViewingScreen.class)
public abstract class DefaultDisplayViewingScreenMixin extends AbstractDisplayViewingScreen {
    protected DefaultDisplayViewingScreenMixin(
        Map<DisplayCategory<?>, List<DisplaySpec>> categoryMap, @Nullable CategoryIdentifier<?> category
    ) {
        super(categoryMap, category);
    }

    @Shadow
    public abstract List<DisplaySpec> getCurrentDisplayed();

    @Inject(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lme/shedaniel/rei/impl/client/gui/widget/TabContainerWidget;tabSize()I"
        )
    )
    private void init(CallbackInfo ci) {
        int maxWidthDisplay = CollectionUtils.<DisplaySpec, Integer>mapAndMax(
            this.getCurrentDisplayed(),
            display -> this.getCurrentCategory().getDisplayWidth(display.provideInternalDisplay()),
            Comparator.naturalOrder()
        ).orElse(150);
        int guiWidth = Math.max(maxWidthDisplay + 10 + 14 + 14, 190);
        this.tabs.initTabsSize(guiWidth);
    }
}

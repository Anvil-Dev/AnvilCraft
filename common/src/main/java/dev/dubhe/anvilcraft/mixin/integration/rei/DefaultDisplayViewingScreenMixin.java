package dev.dubhe.anvilcraft.mixin.integration.rei;

import me.shedaniel.rei.impl.client.gui.screen.DefaultDisplayViewingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@SuppressWarnings("UnstableApiUsage")
@Mixin(DefaultDisplayViewingScreen.class)
public class DefaultDisplayViewingScreenMixin {
    @ModifyConstant(
        method = "init",
        constant = @Constant(intValue = 190)
    )
    private int init(int constant) {
        return 250;
    }
}

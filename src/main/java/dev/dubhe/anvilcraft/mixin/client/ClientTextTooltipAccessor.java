package dev.dubhe.anvilcraft.mixin.client;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientTextTooltip.class)
public interface ClientTextTooltipAccessor {
    @Accessor("text")
    FormattedCharSequence anvilcraft$getText();
}

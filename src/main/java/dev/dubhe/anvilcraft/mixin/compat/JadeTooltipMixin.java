package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.mixin.client.ClientTextTooltipAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import snownee.jade.JadeClient;

import java.util.ArrayList;
import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
abstract class JadeTooltipMixin {
    @Shadow private ItemStack tooltipStack;

    @WrapMethod(method = "setTooltipForNextFrameInternal")
    private void anvilcraft$keepOneModName(
        Font font, List<ClientTooltipComponent> lines, int x, int y, ClientTooltipPositioner positioner,
        @Nullable Identifier style, boolean replaceExisting, Operation<Void> original
    ) {
        List<ClientTooltipComponent> prepared = new ArrayList<>(lines);
        original.call(font, prepared, x, y, positioner, style, replaceExisting);
        Component modName = JadeClient.appendModName(this.tooltipStack);
        if (modName == null) return;
        String name = anvilcraft$text(modName.getVisualOrderText());
        if (name.isEmpty()) return;
        boolean found = false;
        for (int index = 1; index < prepared.size(); index++) {
            if (!name.equals(anvilcraft$text(prepared.get(index)))) continue;
            if (found) prepared.remove(index--);
            else found = true;
        }
    }

    @Unique
    private static @Nullable String anvilcraft$text(ClientTooltipComponent component) {
        if (!(component instanceof ClientTextTooltipAccessor access)) return null;
        return anvilcraft$text(access.anvilcraft$getText());
    }

    @Unique
    private static String anvilcraft$text(FormattedCharSequence sequence) {
        StringBuilder result = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            result.appendCodePoint(codePoint);
            return true;
        });
        return result.toString();
    }
}

package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.awt.Font;

@Mixin(targets = {
    "dev.anvilcraft.resource.ageratum.client.gui.GuideScreen",
    "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDBlockComponent",
    "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDCodeBlockComponent",
    "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent",
    "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDListComponent",
    "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTableComponent",
    "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDBlockComponent",
    "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDEntityComponent",
    "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDItemComponent",
    "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDLatexComponent",
    "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDNBTStructureComponent"
}, remap = false)
abstract class HandbookFontMixin {
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/gui/GuiGraphicsExtractor;anvillib$text(Ljava/awt/Font;Ljava/lang/String;IIIZ)V"), require = 0)
    private static void anvilcraft$string(
        GuiGraphicsExtractor graphics, Font font, @Nullable String text, int x, int y,
        int color, boolean shadow, Operation<Void> original
    ) {
        if (text != null) graphics.text(Minecraft.getInstance().font, text, x, y, anvilcraft$legacyColor(color), shadow);
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/gui/GuiGraphicsExtractor;anvillib$text(Ljava/awt/Font;"
        + "Lnet/minecraft/network/chat/Component;IIIZ)V"), require = 0)
    private static void anvilcraft$component(
        GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color, boolean shadow, Operation<Void> original
    ) {
        graphics.text(Minecraft.getInstance().font, text, x, y, anvilcraft$legacyColor(color), shadow);
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/gui/GuiGraphicsExtractor;anvillib$text(Ljava/awt/Font;"
        + "Lnet/minecraft/util/FormattedCharSequence;IIIZ)V"), require = 0)
    private static void anvilcraft$sequence(
        GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int x, int y,
        int color, boolean shadow, Operation<Void> original
    ) {
        graphics.text(Minecraft.getInstance().font, text, x, y, anvilcraft$legacyColor(color), shadow);
    }

    @Unique
    private static int anvilcraft$legacyColor(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }
}

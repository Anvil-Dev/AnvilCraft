package dev.dubhe.anvilcraft.mixin.client;

import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.dubhe.anvilcraft.integration.ageratum.component.MDDirectoryComponent;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MarkdownParser.class, remap = false)
abstract class HandbookDirectoryMixin {
    @Inject(method = "trySelfClosingExtensionBlock", at = @At("HEAD"), cancellable = true)
    private static void anvilcraft$directory(
        Identifier location, String line, CallbackInfoReturnable<MDComponent> cir
    ) {
        var matcher = AgeratumConstants.Patterns.EXTENSION_TAG_OPEN_PATTERN.matcher(line.trim());
        if (!matcher.matches()) return;
        String name = matcher.group(1);
        if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
        if (name.equals("directory") || name.equals("ageratum:directory")) {
            cir.setReturnValue(new MDDirectoryComponent());
        }
    }

    @Inject(method = "parseDocument", at = @At("RETURN"), cancellable = true)
    private void anvilcraft$expandDirectory(Identifier location, String markdown, CallbackInfoReturnable<MDDocument> cir) {
        cir.setReturnValue(MDDirectoryComponent.expand(cir.getReturnValue()));
    }
}

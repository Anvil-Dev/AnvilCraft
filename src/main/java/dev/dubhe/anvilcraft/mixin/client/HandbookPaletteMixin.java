package dev.dubhe.anvilcraft.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = {
    "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent",
    "dev.anvilcraft.resource.ageratum.client.registries.BuiltinInlineComponents"
}, remap = false)
abstract class HandbookPaletteMixin {
    @ModifyConstant(method = "*", constant = @Constant(intValue = 0x66CCFF), require = 0)
    private static int anvilcraft$linkColor(int color) {
        return 0x075D7F;
    }
}

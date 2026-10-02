package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.URI;

@Mixin(value = GuideScreen.class, remap = false)
abstract class HandbookLinkMixin {
    @WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE", target = "Ljava/net/URI;getRawPath()Ljava/lang/String;"))
    private String anvilcraft$preserveLinkTarget(URI uri, Operation<String> original) {
        return uri.toString();
    }
}

package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.building.BuildingRodClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class BuildingRodKeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$buildingRodKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().handle()
            && BuildingRodClient.handleKeyboardInput(event.key(), event.scancode(), action, event.modifiers())) ci.cancel();
    }
}

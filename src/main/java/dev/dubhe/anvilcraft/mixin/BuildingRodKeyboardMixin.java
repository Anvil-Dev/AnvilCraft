package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.building.BuildingRodClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class BuildingRodKeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$buildingRodKey(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        // Mafglib 可在 NeoForge 按键事件发出前取消输入，建筑杖操作需先于该路径处理。
        if (window == Minecraft.getInstance().getWindow().getWindow()
            && BuildingRodClient.handleKeyboardInput(key, scanCode, action, modifiers)) {
            ci.cancel();
        }
    }
}

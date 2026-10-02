package dev.dubhe.anvilcraft.mixin.client;

import dev.anvilcraft.lib.v2.cube.client.CubeHighlightEvents;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CubeHighlightEvents.class, remap = false)
abstract class CubeHighlightEventsMixin {
    @Inject(method = "highlight", at = @At("HEAD"), cancellable = true)
    private static void anvilcraft$useCombinedOutline(ExtractBlockOutlineRenderStateEvent event, CallbackInfo callback) {
        // 新版提取事件不能取消，避免默认单格描边与本模组的整机/动态描边重复绘制。
        if (AnvilCraft.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(event.getBlockState().getBlock()).getNamespace())) {
            callback.cancel();
        }
    }
}

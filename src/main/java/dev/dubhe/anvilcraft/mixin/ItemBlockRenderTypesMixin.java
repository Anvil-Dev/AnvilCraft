package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {
    @Inject(method = "lambda$static$0", at = @At("RETURN"))
    private static void registerRenderTypes(HashMap<Block, RenderType> map, CallbackInfo ci) {
        map.put(ModBlocks.SPECTRAL_ANVIL.get(), RenderType.translucent());
    }
}

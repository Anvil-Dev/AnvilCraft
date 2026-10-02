package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsBlockEntityRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class RuinsClientLevelMixin {
    @Inject(method = "onBlockEntityAdded", at = @At("TAIL"))
    private void anvilcraft$ruinsVisibility(BlockEntity entity, CallbackInfo ci) {
        if (entity instanceof RuinsBlockEntity ruins) RuinsBlockEntityRenderer.updateVisibility(ruins);
    }
}

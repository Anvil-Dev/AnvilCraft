package dev.dubhe.anvilcraft.mixin.workaround;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumModelProvider;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Mixin(RegistrumModelProvider.class)
public class RegistrumModelProviderMixin {
    @Inject(
        method = "getKnownBlocks",
        at = @At("HEAD"),
        cancellable = true
    )
    void makeMinecraftHappyWithHandmadeBlockStatesJson(CallbackInfoReturnable<Stream<? extends Holder<Block>>> cir){
        cir.setReturnValue(Stream.empty());
        cir.cancel();
    }
}

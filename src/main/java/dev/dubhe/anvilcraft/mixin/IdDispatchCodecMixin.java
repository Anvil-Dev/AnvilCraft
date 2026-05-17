package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.AnvilCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.IdDispatchCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IdDispatchCodec.class)
public class IdDispatchCodecMixin {
    @Inject(
        method = "encode(Lio/netty/buffer/ByteBuf;Ljava/lang/Object;)V",
        at = @At(
            value = "INVOKE",
            target = "Lio/netty/handler/codec/EncoderException;<init>(Ljava/lang/String;Ljava/lang/Throwable;)V"
        )
    )
    private <B extends ByteBuf, V> void encode(B output, V value, CallbackInfo ci, @Local(name = "var7") Exception var7) {
        AnvilCraft.LOGGER.error(var7.getLocalizedMessage(), var7);
    }
}

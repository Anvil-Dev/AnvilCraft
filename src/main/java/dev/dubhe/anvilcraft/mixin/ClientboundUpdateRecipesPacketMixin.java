package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ClientboundUpdateRecipesPacket.class)
abstract class ClientboundUpdateRecipesPacketMixin {
    @Redirect(
        method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/FriendlyByteBuf;"
                + "readList(Lnet/minecraft/network/FriendlyByteBuf$Reader;)Ljava/util/List;"
        )
    )
    private <T> @NotNull List<T> init(@NotNull FriendlyByteBuf instance, FriendlyByteBuf.Reader<T> elementReader) {
        try {
            return instance.readList(elementReader);
        } catch (Exception e) {
            AnvilCraft.LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }
}

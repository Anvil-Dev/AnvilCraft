package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.PlayerEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract ClientLevel getLevel();

    @Inject(
        method = "handleLogin",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;broadcastOptions()V")
    )
    private void handleLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            AnvilCraft.EVENT_BUS.post(new PlayerEvent.ClientPlayerJoin(player, player.getOnPos(), this.getLevel()));
        }
    }
}

package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.hud.EnergyWeaponUseHUD;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public record WeaponChargeProgressPacket(int item, int hand, int elapsed, int duration, boolean repeating, float tickRate)
    implements IClientboundPacket {
    public static final Type<WeaponChargeProgressPacket> TYPE = IPacket.type(AnvilCraft.of("weapon_charge_progress"));
    public static final StreamCodec<ByteBuf, WeaponChargeProgressPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, WeaponChargeProgressPacket::item,
        ByteBufCodecs.VAR_INT, WeaponChargeProgressPacket::hand,
        ByteBufCodecs.VAR_INT, WeaponChargeProgressPacket::elapsed,
        ByteBufCodecs.VAR_INT, WeaponChargeProgressPacket::duration,
        ByteBufCodecs.BOOL, WeaponChargeProgressPacket::repeating,
        ByteBufCodecs.FLOAT, WeaponChargeProgressPacket::tickRate,
        WeaponChargeProgressPacket::new
    );

    public static void sync(Player player, ItemStack stack, int elapsed, int duration, boolean repeating) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.getUseItem() != stack) return;
        float tickRate = player.level().tickRateManager().tickrate();
        int interval = Math.max(1, Math.round(tickRate / 20));
        boolean boundary = elapsed == 1 || repeating && duration > 0 && elapsed % duration == 0;
        if (!boundary && player.tickCount % interval != 0) return;
        PacketDistributor.sendToPlayer(serverPlayer, new WeaponChargeProgressPacket(
            BuiltInRegistries.ITEM.getId(stack.getItem()), player.getUsedItemHand().ordinal(), elapsed, duration, repeating, tickRate));
    }

    public float progressAfter(double milliseconds) {
        if (this.duration <= 0) return -1;
        // Bound prediction when the server stalls; the next authoritative sample supplies the phase again.
        double predictionLimit = 2000.0 / Math.clamp(this.tickRate, 1, 20);
        double ticks = this.elapsed + Math.clamp(milliseconds, 0, predictionLimit) * this.tickRate / 1000;
        return this.repeating ? (float) (ticks % this.duration / this.duration) : (float) Math.clamp(ticks / this.duration, 0, 1);
    }

    @Override
    public Type<WeaponChargeProgressPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        EnergyWeaponUseHUD.update(this);
    }
}

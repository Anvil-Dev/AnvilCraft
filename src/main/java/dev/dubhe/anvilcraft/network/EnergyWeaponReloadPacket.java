package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.item.EnergyWeaponFirstPersonRenderer;
import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponReload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record EnergyWeaponReloadPacket(ItemStack weapon, ItemStack capacitor, int slot, int ticks) implements IClientboundPacket {
    public static final Type<EnergyWeaponReloadPacket> TYPE = IPacket.type(AnvilCraft.of("energy_weapon_reload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnergyWeaponReloadPacket> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC, EnergyWeaponReloadPacket::weapon,
        ItemStack.OPTIONAL_STREAM_CODEC, EnergyWeaponReloadPacket::capacitor,
        ByteBufCodecs.VAR_INT, EnergyWeaponReloadPacket::slot,
        ByteBufCodecs.VAR_INT, EnergyWeaponReloadPacket::ticks,
        EnergyWeaponReloadPacket::new
    );

    @Override
    public Type<EnergyWeaponReloadPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (this.weapon.isEmpty()) {
            player.getPersistentData().remove(EnergyWeaponReload.CLIENT_SLOT);
        } else {
            player.getPersistentData().putInt(EnergyWeaponReload.CLIENT_SLOT, this.slot);
        }
        EnergyWeaponFirstPersonRenderer.updateReload(this.weapon, this.capacitor, this.ticks, this.slot);
    }
}

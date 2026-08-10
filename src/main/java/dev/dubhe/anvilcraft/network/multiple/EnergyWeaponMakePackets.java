package dev.dubhe.anvilcraft.network.multiple;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.EnergyWeaponMakeMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.entity.player.Player;

public class EnergyWeaponMakePackets {
    private static <T extends IPacket> Type<T> of(String id) {
        return IPacket.type(AnvilCraft.of("energy_weapon_" + id));
    }

    public record Make() implements IServerboundPacket {
        public static final Type<Make> TYPE = EnergyWeaponMakePackets.of("make");
        public static final StreamCodec<ByteBuf, Make> STREAM_CODEC = StreamCodec.unit(new Make());

        @Override
        public Type<Make> type() {
            return Make.TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof EnergyWeaponMakeMenu menu)) return;
            menu.make(player);
        }
    }

    public record Select(int index) implements IServerboundPacket {
        public static final Type<Select> TYPE = EnergyWeaponMakePackets.of("select");
        public static final StreamCodec<ByteBuf, Select> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Select::index,
            Select::new
        );

        @Override
        public Type<Select> type() {
            return Select.TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            if (!(player.containerMenu instanceof EnergyWeaponMakeMenu menu)) return;
            menu.setSelectedIndex(this.index);
        }
    }
}

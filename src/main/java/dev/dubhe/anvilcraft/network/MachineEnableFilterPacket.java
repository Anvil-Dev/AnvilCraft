package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.IFilterScreen;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public record MachineEnableFilterPacket(boolean filterEnabled) implements ISensitiveBiPacket {
    public static final Type<MachineEnableFilterPacket> TYPE = IPacket.type(AnvilCraft.of("machine_record_material"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineEnableFilterPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        MachineEnableFilterPacket::filterEnabled,
        MachineEnableFilterPacket::new
    );

    @Override
    public Type<MachineEnableFilterPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (Minecraft.getInstance().screen instanceof IFilterScreen<?> screen) {
            screen.setFilterEnabled(this.filterEnabled());
            screen.flush();
        }
    }

    @Override
    public void handleOnServer(Player player) {
        if (!player.hasContainerOpen()) return;
        if (!(player.containerMenu instanceof IFilterMenu menu)) return;
        menu.setFilterEnabled(this.filterEnabled());
        menu.flush();
        if (!this.filterEnabled()) {
            for (int i = 0; i < menu.getFilteredItems().size(); i++) {
                ItemStack stack = menu.getFilteredItems().get(i);
                if (stack.isEmpty()) continue;
                SlotFilterChangePacket pack = new SlotFilterChangePacket(i, stack);
                PacketDistributor.sendToPlayer(Util.cast(player), pack);
            }
        }
        PacketDistributor.sendToPlayer(Util.cast(player), this);
    }
}

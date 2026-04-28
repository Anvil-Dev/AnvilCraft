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

public record SlotFilterChangePacket(int index, ItemStack filter) implements ISensitiveBiPacket {
    public static final Type<SlotFilterChangePacket> TYPE = IPacket.type(AnvilCraft.of("slot_filter_change"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotFilterChangePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        SlotFilterChangePacket::index,
        ItemStack.OPTIONAL_STREAM_CODEC,
        SlotFilterChangePacket::filter,
        SlotFilterChangePacket::new
    );

    /**
     * 更改过滤
     *
     * @param index  槽位
     * @param filter 过滤
     */
    public SlotFilterChangePacket(int index, ItemStack filter, boolean forceCount) {
        this(index, forceCount ? filter.copyWithCount(1) : filter.copy());
    }

    @Override
    public Type<SlotFilterChangePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(Minecraft.getInstance().screen instanceof IFilterScreen<?> screen)) return;
        screen.setFilter(this.index, this.filter);
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof IFilterMenu menu)) return;
        menu.setFilter(this.index, this.filter);
        menu.flush();
        PacketDistributor.sendToPlayer(Util.cast(player), this);
    }
}

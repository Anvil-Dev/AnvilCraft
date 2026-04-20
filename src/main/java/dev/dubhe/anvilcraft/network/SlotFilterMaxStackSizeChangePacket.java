package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.IFilterScreen;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public record SlotFilterMaxStackSizeChangePacket(int index, int maxStackSize) implements ISensitiveBiPacket {
    public static final Type<SlotFilterMaxStackSizeChangePacket> TYPE = IPacket.type(AnvilCraft.of("slot_filter_max_stack_size_change"));
    public static final StreamCodec<ByteBuf, SlotFilterMaxStackSizeChangePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        SlotFilterMaxStackSizeChangePacket::index,
        ByteBufCodecs.VAR_INT,
        SlotFilterMaxStackSizeChangePacket::maxStackSize,
        SlotFilterMaxStackSizeChangePacket::new
    );

    @Override
    public Type<SlotFilterMaxStackSizeChangePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(Minecraft.getInstance().screen instanceof IFilterScreen<?> screen)) return;
        IFilterBlockEntity filter = screen.getFilterMenu().getFilterBlockEntity();
        filter.setSlotLimit(this.index, this.maxStackSize);
    }

    @Override
    public void handleOnServer(Player player) {
        if (!player.hasContainerOpen()) return;
        if (!(player.containerMenu instanceof IFilterMenu menu)) return;
        IFilterBlockEntity filter = menu.getFilterBlockEntity();
        filter.setSlotLimit(this.index, this.maxStackSize);
        if (filter instanceof BlockEntity be) {
            be.setChanged();
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
        menu.flush();
        PacketDistributor.sendToPlayer(Util.cast(player), this);
    }
}
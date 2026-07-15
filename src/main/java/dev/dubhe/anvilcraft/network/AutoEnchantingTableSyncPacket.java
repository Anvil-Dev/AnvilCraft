package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Set;

public record AutoEnchantingTableSyncPacket(BlockPos pos, List<Integer> ids) implements IServerboundPacket {
    private static final Type<AutoEnchantingTableSyncPacket> TYPE =
        IPacket.type(AnvilCraft.of("auto_enchanting_table"));
    public static final StreamCodec<ByteBuf, AutoEnchantingTableSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        AutoEnchantingTableSyncPacket::pos,
        ByteBufCodecs.INT.apply(ByteBufCodecs.list()),
        AutoEnchantingTableSyncPacket::ids,
        AutoEnchantingTableSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        Level level = player.level();
        BlockEntity blockEntity = level.getBlockEntity(this.pos);
        if (blockEntity instanceof AutoEnchantingTableBlockEntity autoEnchantingTableBlockEntity) {
            Set<Integer> selectedEnchantmentList = autoEnchantingTableBlockEntity.getSelectedEnchantmentSet();
            selectedEnchantmentList.clear();
            selectedEnchantmentList.addAll(this.ids);
            autoEnchantingTableBlockEntity.setOpenMenu(false);
            autoEnchantingTableBlockEntity.onChange();
        }
    }
}

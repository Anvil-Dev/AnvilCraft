package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockInfo;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockSection;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record SlidingEntitySyncPacket(int id, List<SlidingBlockInfo> infos, Direction moveDirection) implements IClientboundPacket {
    public static final Type<SlidingEntitySyncPacket> TYPE = IPacket.type(AnvilCraft.of("sliding_entity_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlidingEntitySyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        SlidingEntitySyncPacket::id,
        SlidingBlockInfo.STREAM_CODEC.apply(ByteBufCodecs.list()),
        SlidingEntitySyncPacket::infos,
        Direction.STREAM_CODEC,
        SlidingEntitySyncPacket::moveDirection,
        SlidingEntitySyncPacket::new
    );

    @Override
    public Type<SlidingEntitySyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(player.level().getEntity(this.id) instanceof SlidingBlockEntity sliding)) return;
        sliding.setSection(new SlidingBlockSection(this.infos));
        sliding.setMoveDirection(this.moveDirection);
    }
}

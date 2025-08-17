package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.DragonRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record DragonRodDevourPacket(ResourceKey<Level> levelKey, InteractionHand hand, BlockPos pos,
                                    Direction blockFace) implements CustomPacketPayload {
    public static final Type<DragonRodDevourPacket> TYPE = new Type<>(AnvilCraft.of("dragon_rod_devour"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DragonRodDevourPacket> STREAM_CODEC = StreamCodec.of(
        DragonRodDevourPacket::encode, DragonRodDevourPacket::decode
    );
    public static final IPayloadHandler<DragonRodDevourPacket> HANDLER = DragonRodDevourPacket::serverHandler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, DragonRodDevourPacket packet) {
        buf.writeResourceKey(packet.levelKey);
        buf.writeEnum(packet.hand);
        buf.writeBlockPos(packet.pos);
        buf.writeEnum(packet.blockFace);
    }

    private static DragonRodDevourPacket decode(RegistryFriendlyByteBuf buf) {
        return new DragonRodDevourPacket(
            buf.readResourceKey(ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("level"))),
            buf.readEnum(InteractionHand.class),
            buf.readBlockPos(),
            buf.readEnum(Direction.class)
        );
    }

    private static void serverHandler(DragonRodDevourPacket packet, IPayloadContext ctx) {
        Player player = ctx.player();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        ServerLevel level = serverPlayer.server.overworld().getServer().getLevel(packet.levelKey);
        if (level == null) return;
        ctx.enqueueWork(() -> DragonRodItem.devourBlock(
            level, player, packet.hand,
            packet.pos, level.getBlockState(packet.pos), packet.blockFace
        ));
    }
}

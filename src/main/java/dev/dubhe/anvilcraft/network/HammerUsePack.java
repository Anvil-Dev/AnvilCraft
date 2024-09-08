package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public class HammerUsePack implements CustomPacketPayload {
    public static final Type<HammerUsePack> TYPE = new Type<>(AnvilCraft.of("hammer_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HammerUsePack> STREAM_CODEC = StreamCodec.ofMember(
            HammerUsePack::encode, HammerUsePack::new
    );
    public static final IPayloadHandler<HammerUsePack> HANDLER = HammerUsePack::serverHandler;


    private final BlockPos pos;
    private final InteractionHand hand;

    public HammerUsePack(BlockPos pos, InteractionHand hand) {
        this.pos = pos;
        this.hand = hand;
    }

    public HammerUsePack(@NotNull RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.hand = buf.readEnum(InteractionHand.class);
    }

    public void encode(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeEnum(this.hand);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void serverHandler(HammerUsePack data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            ItemStack itemInHand = player.getItemInHand(data.hand);
            if (!(player.level() instanceof ServerLevel level)) return;
            AnvilHammerItem.useBlock(player, data.pos, level, itemInHand);
        });
    }
}

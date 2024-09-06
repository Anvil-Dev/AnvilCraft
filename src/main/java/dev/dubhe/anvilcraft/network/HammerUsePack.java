package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class HammerUsePack implements Packet {
    private final BlockPos pos;
    private final InteractionHand hand;

    public HammerUsePack(BlockPos pos, InteractionHand hand) {
        this.pos = pos;
        this.hand = hand;
    }

    public HammerUsePack(@NotNull FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.hand = buf.readEnum(InteractionHand.class);
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.HAMMER_USE;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeEnum(this.hand);
    }

    @Override
    public void handler(@NotNull MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            ItemStack itemInHand = player.getItemInHand(this.hand);
            if (!(player.level() instanceof ServerLevel level)) return;
            AnvilHammerItem.useBlock(player, this.pos, level, itemInHand);
        });
    }
}

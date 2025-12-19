package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.PillBoxItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record UsePillBoxPacket() implements CustomPacketPayload {
    public static final Type<UsePillBoxPacket> TYPE = new Type<>(AnvilCraft.of("use_pill_box"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UsePillBoxPacket> STREAM_CODEC =
        StreamCodec.ofMember(UsePillBoxPacket::encode, UsePillBoxPacket::decode);
    public static final IPayloadHandler<UsePillBoxPacket> HANDLER = UsePillBoxPacket::serverHandler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(RegistryFriendlyByteBuf buf) {
    }

    private static UsePillBoxPacket decode(RegistryFriendlyByteBuf buf) {
        return new UsePillBoxPacket();
    }

    private void serverHandler(IPayloadContext context) {
        Player player = context.player();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item.is(ModItems.PILL_BOX)) {
                if (player.getCooldowns().isOnCooldown(item.getItem())) {
                    return;
                }
                PillBoxItem.use(item, player);
                player.containerMenu.sendAllDataToRemote();
            }
        }
    }
}

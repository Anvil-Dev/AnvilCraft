package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.item.PillBoxItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record UsePillBoxPacket() implements IServerboundPacket {
    public static final Type<UsePillBoxPacket> TYPE = IPacket.type(AnvilCraft.of("use_pill_box"));
    public static final StreamCodec<ByteBuf, UsePillBoxPacket> STREAM_CODEC = StreamCodec.unit(
        new UsePillBoxPacket()
    );

    @Override
    public Type<UsePillBoxPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        for (ItemStack stack : PocketInventory.carriedItems(player)) {
            if (!stack.is(ModItems.PILL_BOX)) continue;
            if (player.getCooldowns().isOnCooldown(stack.getItem())) return;
            if (!PillBoxItem.use(stack, player).getResult().consumesAction()) continue;
            PocketInventory.get(player).syncChanges(player);
            player.containerMenu.sendAllDataToRemote();
            return;
        }
    }
}

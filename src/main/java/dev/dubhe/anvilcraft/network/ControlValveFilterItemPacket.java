package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.ControlValveScreen;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.ControlValveMenu;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 双向：设置控制阀过滤格中的过滤器物品。
 *
 * <p>客户端放入 / 取回过滤器时发往服务端；服务端存入后回传，令双方一致。
 * 过滤器内容决定本阀门允许多种流体通过。
 */
public record ControlValveFilterItemPacket(ItemStack filterItem) implements ISensitiveBiPacket {
    private static final int MAX_FILTER_ITEMS = new FilterContent().list().size();
    public static final Type<ControlValveFilterItemPacket> TYPE =
        IPacket.type(AnvilCraft.of("control_valve_filter_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ControlValveFilterItemPacket> STREAM_CODEC =
        StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            ControlValveFilterItemPacket::filterItem,
            ControlValveFilterItemPacket::new
        );

    @Override
    public Type<ControlValveFilterItemPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (Minecraft.getInstance().screen instanceof ControlValveScreen screen) {
            screen.setFilterItem(this.filterItem);
        }
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof ControlValveMenu menu) || menu.getBlockEntity() == null) {
            return;
        }
        FilterContent content = this.filterItem.get(ModComponents.FILTER_CONTENT);
        if ((!this.filterItem.isEmpty() && !this.filterItem.is(ModItems.FILTER))
            || (content != null && content.list().size() > MAX_FILTER_ITEMS)) {
            PacketDistributor.sendToPlayer(
                Util.cast(player), new ControlValveFilterItemPacket(menu.getBlockEntity().getFilterItem().copy())
            );
            return;
        }
        menu.getBlockEntity().setFilterItem(this.filterItem);
        menu.getBlockEntity().sendUpdate();
        PacketDistributor.sendToPlayer(
            Util.cast(player), new ControlValveFilterItemPacket(menu.getBlockEntity().getFilterItem().copy())
        );
    }
}

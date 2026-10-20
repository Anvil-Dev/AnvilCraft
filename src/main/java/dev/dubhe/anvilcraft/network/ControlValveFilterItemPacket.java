package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.ControlValveScreen;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.ControlValveMenu;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public record ControlValveFilterItemPacket(ItemStack filterItem) implements ISensitiveBiPacket {
    private static final int MAX_FILTER_ITEMS = new FilterContent().list().size();
    public static final Type<ControlValveFilterItemPacket> TYPE = IPacket.type(AnvilCraft.of("control_valve_filter_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ControlValveFilterItemPacket> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC, ControlValveFilterItemPacket::filterItem, ControlValveFilterItemPacket::new
    );

    @Override
    public Type<ControlValveFilterItemPacket> type() {
        return ControlValveFilterItemPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (Minecraft.getInstance().screen instanceof ControlValveScreen screen) screen.setFilterItem(this.filterItem);
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.containerMenu instanceof ControlValveMenu menu)
            || menu.getBlockEntity() == null || !menu.stillValid(player)) return;
        if (!this.filterItem.isEmpty() && !this.filterItem.is(ModItems.FILTER)) return;
        FilterContent content = this.filterItem.get(ModComponents.FILTER_CONTENT);
        if (content != null && content.list().size() > ControlValveFilterItemPacket.MAX_FILTER_ITEMS) return;
        menu.getBlockEntity().setFilterItem(this.filterItem);
        menu.getBlockEntity().sendUpdate();
        PacketDistributor.sendToPlayer(serverPlayer, this);
    }
}

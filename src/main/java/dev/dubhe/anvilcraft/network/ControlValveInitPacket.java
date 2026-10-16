package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.ControlValveScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 服务端 → 客户端：打开控制阀 GUI 时同步当前最大流速与过滤内容。
 *
 * <p>过滤内容包含手动放入的单流体白名单与过滤格中的过滤器物品（用于渲染流体图标）。</p>
 */
public record ControlValveInitPacket(int maxRate, FluidStack filter, ItemStack filterItem) implements IClientboundPacket {
    public static final Type<ControlValveInitPacket> TYPE = IPacket.type(AnvilCraft.of("control_valve_init"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ControlValveInitPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        ControlValveInitPacket::maxRate,
        FluidStack.OPTIONAL_STREAM_CODEC,
        ControlValveInitPacket::filter,
        ItemStack.OPTIONAL_STREAM_CODEC,
        ControlValveInitPacket::filterItem,
        ControlValveInitPacket::new
    );

    @Override
    public Type<ControlValveInitPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (Minecraft.getInstance().screen instanceof ControlValveScreen screen) {
            screen.setValue(this.maxRate);
            screen.setFilter(0, this.filter);
            screen.setFilterItem(this.filterItem);
        }
    }
}

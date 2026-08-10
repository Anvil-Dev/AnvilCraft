package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record UpdateDisplayItemPacket(ItemStack displayItem, BlockPos pos) implements IClientboundPacket {
    public static final Type<UpdateDisplayItemPacket> TYPE = IPacket.type(AnvilCraft.of("client_update_display_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateDisplayItemPacket> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC,
        UpdateDisplayItemPacket::displayItem,
        BlockPos.STREAM_CODEC,
        UpdateDisplayItemPacket::pos,
        UpdateDisplayItemPacket::new
    );

    @Override
    public Type<UpdateDisplayItemPacket> type() {
        return UpdateDisplayItemPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        Level level = player.level();
        BlockState state = level.getBlockState(this.pos);
        if (
            state.isAir()
            || !state.hasBlockEntity()
            || level.getBlockEntity(this.pos) instanceof IHasDisplayItem
        ) {
            IHasDisplayItem be = (IHasDisplayItem) level.getBlockEntity(this.pos);
            if (be == null) return;
            be.updateDisplayItem(this.displayItem);
        }
    }
}

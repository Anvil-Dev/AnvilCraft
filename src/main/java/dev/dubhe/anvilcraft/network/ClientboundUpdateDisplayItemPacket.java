package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.init.ModNetworks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ClientboundUpdateDisplayItemPacket implements Packet {
    private final ItemStack displayItem;
    private final BlockPos pos;

    public ClientboundUpdateDisplayItemPacket(ItemStack displayItem, BlockPos pos) {
        this.displayItem = displayItem;
        this.pos = pos;
    }

    public ClientboundUpdateDisplayItemPacket(@NotNull FriendlyByteBuf buf) {
        this.displayItem = buf.readItem();
        this.pos = buf.readBlockPos();
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.CLIENT_UPDATE_DISPLAY_ITEM;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeItem(displayItem);
        buf.writeBlockPos(pos);
    }

    @Override
    public void handler() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.level == null) return;
            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir()
                    || !state.hasBlockEntity()
                    || mc.level.getBlockEntity(pos) instanceof IHasDisplayItem
            ) {
                IHasDisplayItem be = (IHasDisplayItem) mc.level.getBlockEntity(pos);
                if (be == null) return; // make idea happy
                be.updateDisplayItem(displayItem);
            }
        });

    }
}

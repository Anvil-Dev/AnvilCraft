package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.InventoryUtil;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;

import java.util.List;

public record Eternal() {
    public static final Eternal INSTANCE = new Eternal();
    public static final Codec<Eternal> CODEC = Codec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, Eternal> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void tick(ServerPlayer player) {
        List<ItemStack> eternalItems = InventoryUtil.getItems(
            player.getInventory(),
            stack -> stack.has(ModComponents.ETERNAL)
        );

        for (ItemStack stack : eternalItems) {
            if (stack.has(DataComponents.DAMAGE)) {
                stack.set(DataComponents.DAMAGE, 0);
            }
            if (!stack.has(DataComponents.UNBREAKABLE)) {
                stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
            }
        }
    }
}

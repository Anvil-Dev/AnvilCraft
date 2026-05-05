package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.cache.RecipeCaches;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.HashMap;
import java.util.Map;

public record RecipeCacheSyncPacket(Map<ItemStack, RecipeHolder<JewelCraftingRecipe>> data) implements IClientboundPacket {
    public static final Type<RecipeCacheSyncPacket> TYPE = IPacket.type(AnvilCraft.of("recipe_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeCacheSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(
            HashMap::new,
            ItemStack.STREAM_CODEC,
            Util.cast(RecipeHolder.STREAM_CODEC)
        ),
        RecipeCacheSyncPacket::data,
        RecipeCacheSyncPacket::new
    );

    @Override
    public Type<RecipeCacheSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        RecipeCaches.networkSynced(this.data);
    }
}

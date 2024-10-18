package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.cache.RecipeCaches;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record RecipeCacheSyncPacket(
    Map<ItemStack, RecipeHolder<JewelCraftingRecipe>> data
) implements CustomPacketPayload {

    public static final Type<RecipeCacheSyncPacket> TYPE = new Type<>(AnvilCraft.of("recipe_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeCacheSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(
            HashMap::new,
            net.minecraft.world.item.ItemStack.STREAM_CODEC,
            (StreamCodec<RegistryFriendlyByteBuf,RecipeHolder<JewelCraftingRecipe>>) (Object) RecipeHolder.STREAM_CODEC
        ),
        RecipeCacheSyncPacket::data,
        RecipeCacheSyncPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void acceptClient(RecipeCacheSyncPacket packet, IPayloadContext ctx) {
        RecipeCaches.networkSynced(packet.data);
    }
}

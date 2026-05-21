package dev.dubhe.anvilcraft.recipe.sync;

import dev.dubhe.anvilcraft.network.RecipesSyncPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class RecipesRecord {
    public static final MutableRecipeMap RECIPES = MutableRecipeMap.create();

    public static void sync2C(
        BiConsumer<RecipesSyncPacket, RecipesSyncPacket[]> sender,
        Iterable<RecipeHolder<?>> recipes,
        RegistryAccess registries
    ) {
        List<RecipesSyncPacket> packets = new ArrayList<>();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
            Unpooled.buffer(RecipesSyncPacket.INITIAL_BUFFER_SIZE),
            registries,
            ConnectionType.NEOFORGE
        );
        RecipesSyncPacket packet = new RecipesSyncPacket(new ArrayList<>(), buf);
        for (RecipeHolder<?> recipe : recipes) {
            RecipeHolder.STREAM_CODEC.encode(buf, recipe);
            if (buf.writerIndex() < RecipesSyncPacket.LIMIT) continue;
            packets.add(packet);
            buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(RecipesSyncPacket.INITIAL_BUFFER_SIZE),
                registries,
                ConnectionType.NEOFORGE
            );
            packet = new RecipesSyncPacket(new ArrayList<>(), buf);
        }
        if (buf.writerIndex() > 0) packets.add(packet);
        if (packets.isEmpty()) return;
        RecipesSyncPacket[] dest = new RecipesSyncPacket[packets.size() - 1];
        System.arraycopy(packets.toArray(RecipesSyncPacket[]::new), 1, dest, 0, packets.size() - 1);
        sender.accept(packets.getFirst(), dest);
    }
}

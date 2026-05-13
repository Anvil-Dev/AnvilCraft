package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record RecipesSyncPacket(
    List<RecipeHolder<?>> recipes,
    @Nullable RegistryFriendlyByteBuf encoded
) implements IClientboundPacket {
    private static final int ABSOLUTE_LIMIT = 2097151;
    public static final int LIMIT = ABSOLUTE_LIMIT / 2;
    public static final int INITIAL_BUFFER_SIZE = 2048;
    public static final Type<RecipesSyncPacket> TYPE = IPacket.type(AnvilCraft.of("recipes_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipesSyncPacket> STREAM_CODEC = StreamCodec.of(
        RecipesSyncPacket::encode,
        RecipesSyncPacket::decode
    );

    public static void encode(RegistryFriendlyByteBuf buf, RecipesSyncPacket packet) {
        buf.ensureWritable(packet.encoded.readableBytes());
        packet.encoded.getBytes(packet.encoded.readerIndex(), buf, packet.encoded.readableBytes());
    }

    public static RecipesSyncPacket decode(RegistryFriendlyByteBuf buf) {
        List<RecipeHolder<?>> recipes = new ArrayList<>();
        while (buf.readerIndex() < buf.writerIndex()) {
            recipes.add(RecipeHolder.STREAM_CODEC.decode(buf));
        }
        return new RecipesSyncPacket(recipes, null);
    }

    @Override
    public Type<RecipesSyncPacket> type() {
        return RecipesSyncPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        RecipesRecord.RECIPES.addAll(this.recipes);
    }
}

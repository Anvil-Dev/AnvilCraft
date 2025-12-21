package dev.dubhe.anvilcraft.api.sc.upgrade;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.recipe.util.ISerializer;
import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public interface IUpgrade<T extends IUpgrade<T>> {
    Codec<IUpgrade<?>> CODEC = Codec.lazyInitialized(
        () -> ModRegistries.UPGRADE_TYPE_REGISTRY.byNameCodec().dispatch(IUpgrade::getType, IUpgrade.Type::codec)
    );
    StreamCodec<RegistryFriendlyByteBuf, IUpgrade<?>> STREAM_CODEC = ByteBufCodecs.registry(ModRegistries.UPGRADE_TYPE_KEY)
        .dispatch(IUpgrade::getType, IUpgrade.Type::streamCodec);

    Type<T> getType();

    boolean tryUpgrade(Player player, ItemStack stack);

    static boolean playerHas(Player player, Predicate<ItemStack> filter) {
        return InventoryUtil.hasItemInCompat(player, filter);
    }

    void sync(T upgrade);

    interface Type<T extends IUpgrade<T>> extends ISerializer<T> {
        Codec<Type<?>> CODEC = ModRegistries.UPGRADE_TYPE_REGISTRY.byNameCodec();
        StreamCodec<RegistryFriendlyByteBuf, Type<?>> STREAM_CODEC = CodecUtil.codec2Stream(CODEC);

        T create();
    }
}

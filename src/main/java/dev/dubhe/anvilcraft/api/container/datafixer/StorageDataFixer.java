package dev.dubhe.anvilcraft.api.container.datafixer;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.function.UnaryOperator;

public record StorageDataFixer(ResourceLocation id, double version, UnaryOperator<CompoundTag> fixer) {
    public static final Codec<StorageDataFixer> CODEC = Codec.lazyInitialized(ModRegistries.FIXER_REGISTRY::byNameCodec);
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageDataFixer> STREAM_CODEC = ByteBufCodecs.registry(
        ModRegistries.FIXER_KEY
    );
}

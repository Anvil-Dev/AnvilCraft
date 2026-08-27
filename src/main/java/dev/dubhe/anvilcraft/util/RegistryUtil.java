package dev.dubhe.anvilcraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

public class RegistryUtil {
    public static <T> Codec<Holder.Reference<T>> referenceHolderWithLifecycle(Codec<ResourceLocation> idCodec, Registry<T> registry) {
        Codec<Holder.Reference<T>> codec = idCodec
            .comapFlatMap(
                id -> registry.getHolder(id)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + registry.key() + ": " + id)),
                ref -> ref.key().location()
            );
        return ExtraCodecs.overrideLifecycle(
            codec, ref -> registry.registrationInfo(ref.key()).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental())
        );
    }

    public static <T> DataResult<Holder.Reference<T>> safeCastToReference(ResourceKey<Registry<T>> key, Holder<T> value) {
        return value.getDelegate() instanceof Holder.Reference<T> reference
               ? DataResult.success(reference)
               : DataResult.error(() -> "Unregistered holder in " + key + ": " + value);
    }
}

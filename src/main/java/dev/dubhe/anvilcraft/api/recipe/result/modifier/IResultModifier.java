package dev.dubhe.anvilcraft.api.recipe.result.modifier;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public interface IResultModifier {
    Codec<IResultModifier> CODEC = Codec.lazyInitialized(() -> ModRegistries.MODIFIER_TYPE_REGISTRY
        .byNameCodec().dispatch(IResultModifier::type, Type::codec));
    StreamCodec<RegistryFriendlyByteBuf, IResultModifier> STREAM_CODEC = StreamCodec.recursive(
        streamCodec -> ByteBufCodecs.registry(ModRegistries.MODIFIER_KEY)
            .dispatch(IResultModifier::type, Type::streamCodec));

    void modify(ResultContext ctx);

    Type<? extends IResultModifier> type();

    static ItemStack getInput(ResultContext ctx, RecipeInputSlot slot) {
        return ctx.getInput(slot);
    }

    interface Type<T extends IResultModifier> extends ISerializer<T> {
    }
}

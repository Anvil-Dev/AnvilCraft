package dev.dubhe.anvilcraft.recipe.multiple.result.modifier;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.recipe.multiple.result.ResultContext;
import dev.dubhe.anvilcraft.recipe.anvil.ISerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public interface IResultModifier {
    int TEMPLATE = -2;
    int MATERIAL = -1;
    Codec<IResultModifier> CODEC = Codec.lazyInitialized(() -> ModRegistries.MODIFIER_TYPE_REGISTRY
        .byNameCodec().dispatch(IResultModifier::type, Type::codec));
    StreamCodec<RegistryFriendlyByteBuf, IResultModifier> STREAM_CODEC = StreamCodec.recursive(
        streamCodec -> ByteBufCodecs.registry(ModRegistries.MODIFIER_KEY)
            .dispatch(IResultModifier::type, Type::streamCodec));

    void modify(ResultContext ctx);

    Type<? extends IResultModifier> type();

    static ItemStack getInput(ResultContext ctx, int input) {
        return switch (input) {
            case TEMPLATE -> ctx.getTemplate();
            case MATERIAL -> ctx.getMaterial();
            default -> ctx.getInput(input);
        };
    }

    interface Type<T extends IResultModifier> extends ISerializer<T> {
    }

    abstract class BaseBuilder<T extends BaseBuilder<T>> {
        protected int input;

        abstract T getThis();

        public T input(int inputIndex) {
            this.input = inputIndex;
            return this.getThis();
        }
    }
}

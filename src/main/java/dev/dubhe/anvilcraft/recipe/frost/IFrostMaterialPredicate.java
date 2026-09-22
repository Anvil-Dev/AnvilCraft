package dev.dubhe.anvilcraft.recipe.frost;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 浮霜锻造系列配方的材料槽要求。
 *
 * <p>材料是否合法由放入的模板与装备（以及配方本身）决定，因此判断时需要传入配方与输入。</p>
 */
public interface IFrostMaterialPredicate {
    Codec<IFrostMaterialPredicate> CODEC = Codec.lazyInitialized(() -> ModRegistries.FROST_MATERIAL_PREDICATE_TYPE
        .byNameCodec().dispatch(IFrostMaterialPredicate::type, Type::codec));
    StreamCodec<RegistryFriendlyByteBuf, IFrostMaterialPredicate> STREAM_CODEC = ByteBufCodecs
        .registry(ModRegistryKeys.FROST_MATERIAL_PREDICATE_TYPE)
        .dispatch(IFrostMaterialPredicate::type, Type::streamCodec);

    /**
     * 判断材料槽中的材料是否合法。
     *
     * <p>装备槽为空时也会被调用以判断材料能否放入，此时可以借助配方中可能放入的装备判断。</p>
     */
    boolean test(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input);

    /**
     * 取出结果时需要消耗的材料数量，材料不合法时为 0。
     */
    int cost(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input);

    Type<? extends IFrostMaterialPredicate> type();

    interface Type<T extends IFrostMaterialPredicate> extends ISerializer<T> {
    }
}

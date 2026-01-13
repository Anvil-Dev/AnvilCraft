package dev.dubhe.anvilcraft.api.recipe.number;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.init.recipe.ModNumberProviderTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record EnchantmentLevelProvider(
    RecipeInputSlot slot,
    ResourceKey<Enchantment> enchantment,
    LevelBasedValue amount
) implements INumberProvider {
    public static final MapCodec<EnchantmentLevelProvider> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        RecipeInputSlot.CODEC
            .forGetter(EnchantmentLevelProvider::slot),
        ResourceKey.codec(Registries.ENCHANTMENT)
            .fieldOf("enchantment")
            .forGetter(EnchantmentLevelProvider::enchantment),
        LevelBasedValue.CODEC
            .fieldOf("amount")
            .forGetter(EnchantmentLevelProvider::amount)
    ).apply(ins, EnchantmentLevelProvider::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantmentLevelProvider> STREAM_CODEC = StreamCodec.composite(
        RecipeInputSlot.STREAM_CODEC,
        EnchantmentLevelProvider::slot,
        ResourceKey.streamCodec(Registries.ENCHANTMENT),
        EnchantmentLevelProvider::enchantment,
        ByteBufCodecs.fromCodecWithRegistries(LevelBasedValue.CODEC),
        EnchantmentLevelProvider::amount,
        EnchantmentLevelProvider::new
    );

    public static EnchantmentLevelProvider enchantment(RecipeInputSlot slot, ResourceKey<Enchantment> enchantment, float amount) {
        return new EnchantmentLevelProvider(slot, enchantment, LevelBasedValue.constant(amount));
    }

    @Override
    public float getFloat(ResultContext ctx) {
        return this.amount.calculate(ctx.getInput(this.slot).getEnchantmentLevel(ctx.getRegistries().holderOrThrow(this.enchantment)));
    }

    @Override
    public Type type() {
        return ModNumberProviderTypes.ENCHANTMENT_LEVEL.get();
    }

    public static class Type implements INumberProvider.Type<EnchantmentLevelProvider> {
        @Override
        public MapCodec<EnchantmentLevelProvider> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EnchantmentLevelProvider> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

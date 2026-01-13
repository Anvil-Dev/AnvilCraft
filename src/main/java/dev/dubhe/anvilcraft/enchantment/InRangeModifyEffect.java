package dev.dubhe.anvilcraft.enchantment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;

public record InRangeModifyEffect(
    LevelBasedValue min,
    LevelBasedValue max,
    EnchantmentValueEffect modifier
) implements EnchantmentValueEffect {
    private static final LevelBasedValue MIN = new LevelBasedValue.Constant(0F);
    private static final LevelBasedValue MAX = new LevelBasedValue.Constant(Float.MAX_VALUE);
    public static final MapCodec<InRangeModifyEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        LevelBasedValue.CODEC
            .optionalFieldOf("min", MIN)
            .forGetter(InRangeModifyEffect::min),
        LevelBasedValue.CODEC
            .optionalFieldOf("max", MAX)
            .forGetter(InRangeModifyEffect::max),
        EnchantmentValueEffect.CODEC
            .fieldOf("modifier")
            .forGetter(InRangeModifyEffect::modifier)
    ).apply(inst, InRangeModifyEffect::new));

    public static InRangeModifyEffect min(int min, EnchantmentValueEffect modifier) {
        return new InRangeModifyEffect(new LevelBasedValue.Constant(min), MAX, modifier);
    }

    public static InRangeModifyEffect max(int max, EnchantmentValueEffect modifier) {
        return new InRangeModifyEffect(MIN, new LevelBasedValue.Constant(max), modifier);
    }

    public static InRangeModifyEffect range(int min, int max, EnchantmentValueEffect modifier) {
        return new InRangeModifyEffect(new LevelBasedValue.Constant(min), new LevelBasedValue.Constant(max), modifier);
    }

    @Override
    public float process(int enchantmentLevel, RandomSource random, float value) {
        float min = this.min.calculate(enchantmentLevel);
        float max = this.max.calculate(enchantmentLevel);
        if (value < min || value > max) return value;
        return this.modifier.process(enchantmentLevel, random, value);
    }

    @Override
    public MapCodec<? extends EnchantmentValueEffect> codec() {
        return CODEC;
    }
}

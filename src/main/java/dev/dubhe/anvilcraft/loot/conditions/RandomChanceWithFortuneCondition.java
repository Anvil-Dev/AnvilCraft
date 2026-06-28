package dev.dubhe.anvilcraft.loot.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.loot.ModLootItemConditions;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public record RandomChanceWithFortuneCondition(
    float unenchantedChance,
    LevelBasedValue enchantedChance,
    Holder<Enchantment> enchantment
) implements LootItemCondition {

    public static final MapCodec<RandomChanceWithFortuneCondition> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("unenchanted_chance").forGetter(RandomChanceWithFortuneCondition::unenchantedChance),
            LevelBasedValue.CODEC.fieldOf("enchanted_chance").forGetter(RandomChanceWithFortuneCondition::enchantedChance),
            Enchantment.CODEC.fieldOf("enchantment").forGetter(RandomChanceWithFortuneCondition::enchantment)
        ).apply(instance, RandomChanceWithFortuneCondition::new)
    );

    @Override
    public LootItemConditionType getType() {
        return ModLootItemConditions.RANDOM_CHANCE_WITH_FORTUNE.get();
    }

    @Override
    public boolean test(LootContext context) {
        ItemStack itemstack = context.getParamOrNull(LootContextParams.TOOL);
        int i = itemstack != null
            ? EnchantmentHelper.getTagEnchantmentLevel(this.enchantment, itemstack)
            : 0;
        float f = i > 0 ? this.enchantedChance.calculate(i) : this.unenchantedChance;
        return context.getRandom().nextFloat() < f;
    }
}

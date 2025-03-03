package dev.dubhe.anvilcraft.item;

import com.google.common.base.Suppliers;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public enum ModTiers implements Tier {
    AMETHYST(
        751,
        4.0f,
        1.0f,
        10,
        () -> Ingredient.of(Items.AMETHYST_SHARD),
        ModBlockTags.INCORRECT_FOR_AMETHYST_TOOL
    ),
    EMBER_METAL(
        2031,
        10.0f,
        1.0f,
        22,
        () -> Ingredient.of(ModItems.EMBER_METAL_INGOT),
        ModBlockTags.INCORRECT_FOR_EMBER_TOOL
    );
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantmentValue;
    private final Supplier<Ingredient> repairIngredient;
    private final TagKey<Block> incorrectBlockTags;

    ModTiers(
        int uses,
        float speed,
        float damage,
        int enchantmentValue,
        @NotNull Supplier<Ingredient> supplier,
        TagKey<Block> incorrectBlockTags) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantmentValue = enchantmentValue;
        this.repairIngredient = Suppliers.memoize(supplier::get);
        this.incorrectBlockTags = incorrectBlockTags;
    }

    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return this.speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.damage;
    }

    @Override
    public @NotNull TagKey<Block> getIncorrectBlocksForDrops() {
        return incorrectBlockTags;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public @NotNull Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }
}

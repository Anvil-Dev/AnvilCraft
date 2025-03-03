package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.ItemCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.StampingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

public class ItemCrushRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ItemCrushRecipe.builder()
            .requires(Tags.Items.CROPS_WHEAT)
            .result(new ItemStack(ModItems.FLOUR.get()))
            .result(ChanceItemStack.of(new ItemStack(ModItems.FLOUR.get())).withChance(0.5f))
            .save(provider);
        ItemCrushRecipe.builder()
            .requires(ItemTags.LOGS)
            .result(new ItemStack(ModItems.WOOD_FIBER.asItem()))
            .result(new ItemStack(ModItems.RESIN.get()))
            .save(provider);
        StampingRecipe.builder()
            .requires(ModItems.GEODE)
            .result(new ItemStack(Items.AMETHYST_SHARD, 4))
            .result(ChanceItemStack.of(new ItemStack(ModItems.TOPAZ.get())).withChance(0.25f))
            .result(ChanceItemStack.of(new ItemStack(ModItems.SAPPHIRE.get())).withChance(0.25f))
            .result(ChanceItemStack.of(new ItemStack(ModItems.RUBY.get())).withChance(0.25f))
            .save(provider, AnvilCraft.of("stamping/geode_gems"));
        StampingRecipe.builder()
            .requires(Items.COCOA_BEANS)
            .result(new ItemStack(ModItems.COCOA_BUTTER.asItem()))
            .result(new ItemStack(ModItems.COCOA_POWDER.asItem()))
            .save(provider);
        StampingRecipe.builder()
            .requires(ModItems.PRISMARINE_CLUSTER)
            .result(new ItemStack(Items.PRISMARINE_CRYSTALS, 2))
            .result(new ItemStack(Items.PRISMARINE_SHARD))
            .result(ChanceItemStack.of(new ItemStack(Items.PRISMARINE_CRYSTALS)).withChance(0.5f))
            .result(ChanceItemStack.of(new ItemStack(ModItems.PRISMARINE_BLADE.asItem())).withChance(0.15f))
            .save(provider);

        armor(provider, Items.CHAINMAIL_HELMET, Items.CHAIN);
        armor(provider, Items.CHAINMAIL_CHESTPLATE, Items.CHAIN);
        armor(provider, Items.CHAINMAIL_LEGGINGS, Items.CHAIN);
        armor(provider, Items.CHAINMAIL_BOOTS, Items.CHAIN);

        armor(provider, Items.LEATHER_HELMET, Items.LEATHER);
        armor(provider, Items.LEATHER_CHESTPLATE, Items.LEATHER);
        armor(provider, Items.LEATHER_LEGGINGS, Items.LEATHER);
        armor(provider, Items.LEATHER_BOOTS, Items.LEATHER);
        armor(provider, Items.LEATHER_HORSE_ARMOR, Items.LEATHER);

        tool(provider, Items.IRON_SWORD, Items.IRON_INGOT);
        tool(provider, Items.IRON_PICKAXE, Items.IRON_INGOT);
        tool(provider, Items.IRON_AXE, Items.IRON_INGOT);
        tool(provider, Items.IRON_HOE, Items.IRON_INGOT);
        tool(provider, Items.IRON_SHOVEL, Items.IRON_INGOT);
        armor(provider, Items.IRON_HELMET, Items.IRON_INGOT);
        armor(provider, Items.IRON_CHESTPLATE, Items.IRON_INGOT);
        armor(provider, Items.IRON_LEGGINGS, Items.IRON_INGOT);
        armor(provider, Items.IRON_BOOTS, Items.IRON_INGOT);
        armor(provider, Items.IRON_HORSE_ARMOR, Items.IRON_INGOT);

        tool(provider, Items.GOLDEN_SWORD, Items.GOLD_INGOT);
        tool(provider, Items.GOLDEN_PICKAXE, Items.GOLD_INGOT);
        tool(provider, Items.GOLDEN_AXE, Items.GOLD_INGOT);
        tool(provider, Items.GOLDEN_HOE, Items.GOLD_INGOT);
        tool(provider, Items.GOLDEN_SHOVEL, Items.GOLD_INGOT);
        armor(provider, Items.GOLDEN_HELMET, Items.GOLD_INGOT);
        armor(provider, Items.GOLDEN_CHESTPLATE, Items.GOLD_INGOT);
        armor(provider, Items.GOLDEN_LEGGINGS, Items.GOLD_INGOT);
        armor(provider, Items.GOLDEN_BOOTS, Items.GOLD_INGOT);
        armor(provider, Items.GOLDEN_HORSE_ARMOR, Items.GOLD_INGOT);

        tool(provider, Items.DIAMOND_SWORD, Items.DIAMOND);
        tool(provider, Items.DIAMOND_PICKAXE, Items.DIAMOND);
        tool(provider, Items.DIAMOND_AXE, Items.DIAMOND);
        tool(provider, Items.DIAMOND_HOE, Items.DIAMOND);
        tool(provider, Items.DIAMOND_SHOVEL, Items.DIAMOND);
        armor(provider, Items.DIAMOND_HELMET, Items.DIAMOND);
        armor(provider, Items.DIAMOND_CHESTPLATE, Items.DIAMOND);
        armor(provider, Items.DIAMOND_LEGGINGS, Items.DIAMOND);
        armor(provider, Items.DIAMOND_BOOTS, Items.DIAMOND);
        armor(provider, Items.DIAMOND_HORSE_ARMOR, Items.DIAMOND);
    }

    private static void itemCrush(RegistrateRecipeProvider provider, ItemLike input, ItemStack result) {
        ItemCrushRecipe.builder().requires(input).result(result).save(provider);
    }

    private static void tool(RegistrateRecipeProvider provider, ItemLike tool, ItemLike result) {
        ItemCrushRecipe.builder()
            .requires(tool)
            .result(ChanceItemStack.of(new ItemStack(result)).withChance(0.5f))
            .save(provider, AnvilCraft.of("item_crush/tool_%s_2_%s".formatted(getName(tool), getName(result))));
    }

    private static void armor(RegistrateRecipeProvider provider, ItemLike armor, ItemLike result) {
        ItemCrushRecipe.builder()
            .requires(armor)
            .result(ChanceItemStack.of(new ItemStack(result)).withChance(0.5f))
            .result(ChanceItemStack.of(new ItemStack(result)).withChance(0.5f))
            .save(provider, AnvilCraft.of("item_crush/armor_%s_2_%s".formatted(getName(armor), getName(result))));
    }

    private static @NotNull String getName(@NotNull ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }
}

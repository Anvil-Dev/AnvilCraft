package dev.dubhe.anvilcraft.porting;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.JewelCraftingMenu;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.generate.JewelCraftingRecipeGeneratingCache;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class JewelSourcePortTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_jewel_catalogue", JewelSourcePortTests::catalogue,
        "port_jewel_matching", JewelSourcePortTests::matching,
        "port_jewel_codecs", JewelSourcePortTests::codecs,
        "port_jewel_menu_batch", JewelSourcePortTests::menuBatch,
        "port_jewel_menu_curse", JewelSourcePortTests::menuCurse,
        "port_jewel_autofill", JewelSourcePortTests::autofill
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_jewel_source"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 150, 0, true))));
    }

    private static JewelCraftingRecipe recipe(GameTestHelper helper, String name) {
        var holder = helper.getLevel().getServer().getRecipeManager().recipeMap()
            .byKey(ResourceKey.create(Registries.RECIPE, AnvilCraft.of("jewel_crafting/" + name)));
        if (holder == null) throw new IllegalStateException("Missing jewel recipe " + name);
        return (JewelCraftingRecipe) holder.value();
    }

    private static void catalogue(GameTestHelper helper) {
        for (String name : List.of("minecraft_flower_banner_pattern_from_minecraft_flower_banner_pattern_for_banner_patterns",
            "minecraft_music_disc_creator_from_minecraft_music_disc_creator_for_music_discs",
            "minecraft_silence_armor_trim_smithing_template_from_minecraft_silence_armor_trim_smithing_template_for_trim_templates",
            "minecraft_angler_pottery_sherd_from_minecraft_angler_pottery_sherd_for_pottery_sherds")) {
            helper.assertTrue(!recipe(helper, "generated/" + name).hasVanishingCurse(), "Generated duplication families remain uncursed");
        }
        var manager = helper.getLevel().getServer().getRecipeManager().recipeMap();
        var cache = new JewelCraftingRecipeGeneratingCache(helper.getLevel().registryAccess(), manager.values());
        var first = cache.buildRecipes().orElseThrow();
        var second = cache.buildRecipes().orElseThrow();
        helper.assertTrue(first.size() == second.size() && first.stream().map(holder -> holder.id()).distinct().count() == first.size(),
            "Repeated generation has stable unique IDs");
        helper.assertTrue(first.stream().noneMatch(holder -> holder.value().result().is(Items.DIAMOND)),
            "Trim materials must not be mistaken for trim templates");
        helper.assertTrue(recipe(helper, "totem_of_undying").hasVanishingCurse() && recipe(helper, "elytra").hasVanishingCurse(),
            "Default curse remains enabled for valuable static outputs");
        helper.assertTrue(!recipe(helper, "trial_key").hasVanishingCurse() && !recipe(helper, "ominous_trial_key").hasVanishingCurse()
            && !recipe(helper, "ominous_bottle").hasVanishingCurse(), "Trial outputs follow explicit source exemptions");
        helper.succeed();
    }

    private static void matching(GameTestHelper helper) {
        var recipe = recipe(helper, "totem_of_undying");
        var input = new JewelCraftingRecipe.Input(new ItemStack(Items.STONE),
            List.of(ModItems.ROYAL_STEEL_INGOT.asStack(), new ItemStack(Items.EMERALD, 2), new ItemStack(Items.GOLD_BLOCK)));
        helper.assertTrue(recipe.matches(input, helper.getLevel()) && recipe.mergedIngredients().size() == 3,
            "Material matching is unordered and independent of the prototype; menu selects the recipe separately");
        helper.assertTrue(recipe.assemble(input).is(Items.TOTEM_OF_UNDYING), "Assembly uses the declared output");
        var shortInput = new JewelCraftingRecipe.Input(ItemStack.EMPTY,
            List.of(ModItems.ROYAL_STEEL_INGOT.asStack(), new ItemStack(Items.EMERALD), new ItemStack(Items.GOLD_BLOCK)));
        helper.assertTrue(!recipe.matches(shortInput, helper.getLevel()), "Merged material counts are required");
        helper.succeed();
    }

    private static void codecs(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var codec = JewelCraftingRecipe.SERIALIZER.codec().codec();
        var legacy = JsonParser.parseString("{\"ingredients\":[\"minecraft:paper\"],\"result\":{\"id\":\"minecraft:diamond\"}}");
        helper.assertTrue(codec.parse(ops, legacy).getOrThrow().hasVanishingCurse(), "Omitted source curse field defaults to true");
        var recipe = recipe(helper, "trial_key");
        var encoded = codec.encodeStart(ops, recipe).getOrThrow().getAsJsonObject();
        helper.assertTrue(!encoded.get("has_vanishing_curse").getAsBoolean() && encoded.has("result") && !encoded.has("source"),
            "JSON uses the source output/curse contract");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            JewelCraftingRecipe.SERIALIZER.streamCodec().encode(buffer, recipe);
            var decoded = JewelCraftingRecipe.SERIALIZER.streamCodec().decode(buffer);
            helper.assertTrue(!decoded.hasVanishingCurse() && decoded.result().is(Items.TRIAL_KEY)
                && decoded.mergedIngredients().size() == 2, "Network sync retains output, groups and curse flag");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static JewelCraftingMenu menu(GameTestHelper helper, StorageFluidRpcTests.Fixture fixture) {
        var player = fixture.player();
        player.getInventory().clearContent();
        var pos = helper.absolutePos(new BlockPos(2, 15, 2));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.JEWEL_CRAFTING_TABLE.getDefaultState());
        var menu = new JewelCraftingMenu(ModMenuTypes.JEWEL_CRAFTING.get(), 12, player.getInventory(),
            ContainerLevelAccess.create(helper.getLevel(), pos));
        player.containerMenu = menu;
        return menu;
    }

    private static void menuBatch(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var menu = menu(helper, fixture);
            var prototype = new ItemStack(Items.FLOWER_BANNER_PATTERN, 7);
            prototype.set(DataComponents.CUSTOM_NAME, Component.literal("Do not copy this name"));
            menu.getSlot(1).set(prototype);
            final int acceptedPrototypeCount = menu.getSlot(1).getItem().getCount();
            menu.getSlot(2).set(new ItemStack(Items.PAPER, 3));
            menu.getSlot(3).set(new ItemStack(Items.INK_SAC, 3));
            var result = menu.getSlot(0).getItem();
            helper.assertTrue(result.is(Items.FLOWER_BANNER_PATTERN) && result.getCount() == 1
                && result.get(DataComponents.CUSTOM_NAME) == null && result.getEnchantments().isEmpty(),
                "Generated output does not copy prototype count, custom name or forced curse");
            menu.quickMoveStack(fixture.player(), 0);
            helper.assertTrue(fixture.player().getInventory().countItem(Items.FLOWER_BANNER_PATTERN) == 3
                && menu.getSlot(1).getItem().getCount() == acceptedPrototypeCount
                && !menu.getSlot(2).hasItem() && !menu.getSlot(3).hasItem(),
                "Batch state: inventory=" + fixture.player().getInventory().countItem(Items.FLOWER_BANNER_PATTERN)
                    + " prototype=" + menu.getSlot(1).getItem().getCount() + " paper=" + menu.getSlot(2).getItem().getCount()
                    + " ink=" + menu.getSlot(3).getItem().getCount());
            helper.assertTrue(!menu.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND)), "Unrelated material cannot select a trim recipe");
            menu.removed(fixture.player());
        }
        helper.succeed();
    }

    private static void menuCurse(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var menu = menu(helper, fixture);
            menu.getSlot(1).set(new ItemStack(Items.TOTEM_OF_UNDYING));
            menu.getSlot(2).set(new ItemStack(Items.PAPER));
            menu.getSlot(3).set(new ItemStack(Items.INK_SAC));
            helper.assertTrue(!menu.getSlot(0).hasItem(), "Selected output cannot be replaced by an unrelated recipe matching materials");
            menu.getSlot(2).set(new ItemStack(Items.GOLD_BLOCK));
            menu.getSlot(3).set(new ItemStack(Items.EMERALD, 2));
            menu.getSlot(4).set(ModItems.ROYAL_STEEL_INGOT.asStack());
            var output = menu.getSlot(0).getItem();
            helper.assertTrue(output.is(Items.TOTEM_OF_UNDYING) && output.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(helper.getLevel().registryAccess().holderOrThrow(Enchantments.VANISHING_CURSE)) == 1,
                "Default cursed recipes add the source vanishing enchantment");
            helper.assertTrue(menu.getSlot(1).getItem().getEnchantments().isEmpty(), "Preview does not mutate the prototype");
            menu.removed(fixture.player());
        }
        helper.succeed();
    }

    private static void autofill(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var menu = menu(helper, fixture);
            menu.getSlot(1).set(new ItemStack(Items.HEAVY_CORE));
            var inventory = fixture.player().getInventory();
            inventory.setItem(9, ModBlocks.HEAVY_IRON_BLOCK.asStack(32));
            inventory.setItem(10, ModBlocks.HEAVY_IRON_BLOCK.asStack(32));
            var lead = recipe(helper, "heavy_core").mergedIngredients().get(1).getKey().getValues().get(0).value();
            inventory.setItem(11, new ItemStack(lead, 32));
            inventory.setItem(12, new ItemStack(lead, 32));
            inventory.setItem(13, ModBlocks.SPACE_OVERCOMPRESSOR.asStack());
            menu.autoFill();
            helper.assertTrue(menu.getSlot(2).getItem().getCount() == 64 && menu.getSlot(3).getItem().getCount() == 64
                && menu.getSlot(0).getItem().is(Items.HEAVY_CORE), "Autofill combines partial stacks to satisfy grouped costs");
            menu.quickMoveStack(fixture.player(), 0);
            helper.assertTrue(inventory.countItem(Items.HEAVY_CORE) == 1 && !menu.getSlot(2).hasItem() && !menu.getSlot(3).hasItem(),
                "One heavy-core craft consumes exactly sixty-four of both block ingredients");
            menu.removed(fixture.player());
        }
        helper.succeed();
    }
}

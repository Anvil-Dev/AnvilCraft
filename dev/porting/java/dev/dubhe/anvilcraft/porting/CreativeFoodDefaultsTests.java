package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CreativeFoodDefaultsTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_creative_food_default", CreativeFoodDefaultsTests::defaults,
        "port_creative_food_canning", CreativeFoodDefaultsTests::canning,
        "port_creative_food_consumption", CreativeFoodDefaultsTests::consumption,
        "port_creative_food_apple", CreativeFoodDefaultsTests::apple
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_creative_food"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static void defaults(GameTestHelper h) {
        var stack = new ItemStack(ModFoodItems.CANNED_FOOD.get());
        var food = stack.get(DataComponents.FOOD);
        h.assertTrue(food != null && food.nutrition() == 10 && food.saturation() == 16 && !food.canAlwaysEat(), "Default stew nutrition");
        h.assertTrue(stack.get(ModComponents.DISPLAY_ITEM).stored().is(ModFoodItems.BEEF_MUSHROOM_STEW), "Default contained stew");
        h.assertTrue(stack.get(DataComponents.CONSUMABLE).consumeSeconds() == 0.8F, "Source fast consumption time");
        var lines = new ArrayList<Component>();
        ModFoodItems.CANNED_FOOD.get().appendItemTooltip(stack, Item.TooltipContext.of(h.getLevel()),
            TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
        h.assertTrue(lines.equals(java.util.List.of(ModFoodItems.BEEF_MUSHROOM_STEW.asStack().getHoverName())), "Default content tooltip");
        h.succeed();
    }

    private static void canning(GameTestHelper h) {
        int[] expected = {4, 7, 9, 11, 12};
        for (int count = 1; count <= 5; count++) {
            var food = new ItemStack(Items.APPLE, count);
            var can = ModFoodItems.CANNED_FOOD.get().setFood(ModFoodItems.CANNED_FOOD.asStack(), food);
            h.assertTrue(can.get(DataComponents.FOOD).nutrition() == expected[count - 1], "Source canning multiplier " + count);
            h.assertTrue(can.get(DataComponents.CONSUMABLE).consumeSeconds() == 0.8F, "Custom cans remain edible");
            food.setCount(1);
            h.assertTrue(can.get(ModComponents.DISPLAY_ITEM).stored().getCount() == count, "Contained food is copied");
            var lines = new ArrayList<Component>();
            ModFoodItems.CANNED_FOOD.get().appendItemTooltip(can, Item.TooltipContext.of(h.getLevel()),
                TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
            if (count > 1) {
                h.assertTrue(lines.size() == 1 && lines.getFirst().getContents() instanceof TranslatableContents text
                    && text.getKey().equals("tooltip.anvilcraft.item_count") && text.getArgs()[1].equals(count),
                    "Localized content quantity");
            }
        }
        h.succeed();
    }

    private static void consumption(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        var can = new ItemStack(ModFoodItems.CANNED_FOOD.get(), 2);
        h.assertTrue(can.getUseDuration(player) == 16, "Can uses sixteen ticks");
        var result = can.finishUsingItem(h.getLevel(), player);
        h.assertTrue(result.is(ModFoodItems.CANNED_FOOD) && result.getCount() == 1
            && player.getFoodData().getFoodLevel() == 10, "Can consumption changes stack and nutrition");
        h.succeed();
    }

    private static void apple(GameTestHelper h) {
        var stack = ModFoodItems.CURSED_GOLDEN_APPLE.asStack();
        var food = stack.get(DataComponents.FOOD);
        h.assertTrue(food.nutrition() == 4 && Math.abs(food.saturation() - 2.4F) < 1e-6 && food.canAlwaysEat(), "Apple food contract");
        h.assertTrue(stack.getUseDuration(h.makeMockPlayer(GameType.SURVIVAL)) == 32, "Apple uses normal eating duration");
        h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(
            ResourceKey.create(Registries.RECIPE, AnvilCraft.of("cursed_golden_apple"))).isPresent(), "Apple crafting recipe loads");
        h.succeed();
    }
}

package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.client.init.ModCreativeVariantGroups;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CreativeVariantTests {
    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION,
            registry -> registry.register(AnvilCraft.of("port_creative_variants"), CreativeVariantTests::variants));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_creative"));
        event.registerTest(AnvilCraft.of("port_creative_variants"), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of("port_creative_variants")),
            new TestData<>(environment, AnvilCraft.of("port_empty"), 100, 0, true)));
    }

    private static void variants(GameTestHelper helper) {
        boolean previous = AnvilCraft.CLIENT_CONFIG.creativeVariantPickerEnabled;
        ModCreativeVariantGroups.register();
        try {
            for (Function<Color, ItemLike> family : List.<Function<Color, ItemLike>>of(
                ModBlocks.REINFORCED_CONCRETES::get, ModBlocks.REINFORCED_CONCRETE_SLABS::get,
                ModBlocks.REINFORCED_CONCRETE_STAIRS::get, ModBlocks.REINFORCED_CONCRETE_WALLS::get, ModItems.CEMENT_BUCKETS::get)) {
                List<ItemStack> stacks = new ArrayList<>();
                for (Color color : Color.values()) stacks.add(family.apply(color).asItem().getDefaultInstance());
                AnvilCraft.CLIENT_CONFIG.creativeVariantPickerEnabled = true;
                for (ItemStack stack : stacks) {
                    var variants = CreativeVariantPickerRegistry.createVariants(stack).orElseThrow();
                    helper.assertTrue(variants.size() == 16, "All sixteen colors expose the same family");
                    for (int i = 0; i < variants.size(); i++) {
                        helper.assertTrue(ItemStack.matches(variants.get(i), stacks.get(i)),
                            "Source color order and components are preserved");
                    }
                    variants.getFirst().setCount(5);
                    helper.assertTrue(CreativeVariantPickerRegistry.createVariants(stack).orElseThrow().getFirst().getCount() == 1,
                        "Variant lists do not leak mutable stack state");
                }
                var folded = CreativeVariantPickerRegistry.fold(stacks);
                helper.assertTrue(folded.size() == 1 && ItemStack.matches(folded.iterator().next(), stacks.getFirst()),
                    "Complete families fold to their source representative");
                folded.clear();
                folded.add(stacks.getFirst());
                helper.assertTrue(stacks.size() == 16, "Exposed tab collections remain mutable without mutating original contents");
                helper.assertTrue(CreativeVariantPickerRegistry.fold(stacks.subList(0, 15)).size() == 15,
                    "Incomplete groups are never silently removed");
                AnvilCraft.CLIENT_CONFIG.creativeVariantPickerEnabled = false;
                helper.assertTrue(CreativeVariantPickerRegistry.createVariants(stacks.getFirst()).isEmpty()
                    && CreativeVariantPickerRegistry.fold(stacks).size() == 16, "Disabled option preserves the original full family");
            }
        } finally {
            AnvilCraft.CLIENT_CONFIG.creativeVariantPickerEnabled = previous;
        }
        helper.succeed();
    }
}

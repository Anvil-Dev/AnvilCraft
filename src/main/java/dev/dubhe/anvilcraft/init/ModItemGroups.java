package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@SuppressWarnings("unused")
public class ModItemGroups {
    private static final DeferredRegister<CreativeModeTab> DF =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnvilCraft.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_TOOL =
        DF.register("tools", () -> CreativeModeTab.builder()
            .icon(ModItems.ANVIL_HAMMER::asStack)
            .displayItems((ctx, entries) -> {
            })
            .title(REGISTRATE.addLang("itemGroup", AnvilCraft.of("tools"), "AnvilCraft: Utilities"))
            .withTabsAfter(
                AnvilCraft.of("ingredients"),
                AnvilCraft.of("functional_block"),
                AnvilCraft.of("building_block"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_INGREDIENTS =
        DF.register("ingredients", () -> CreativeModeTab.builder()
            .icon(ModItems.MAGNET_INGOT::asStack)
            .displayItems((ctx, entries) -> {
            })
            .title(REGISTRATE.addLang("itemGroup", AnvilCraft.of("ingredients"), "AnvilCraft: Ingredients"))
            .withTabsBefore(ANVILCRAFT_TOOL.getId())
            .withTabsAfter(AnvilCraft.of("functional_block"), AnvilCraft.of("building_block"))
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_FUNCTION_BLOCK =
        DF.register("functional_block", () -> CreativeModeTab.builder()
            .icon(ModBlocks.ROYAL_ANVIL::asStack)
            .displayItems((ctx, entries) -> {
                entries.accept(Items.IRON_TRAPDOOR.getDefaultInstance());
                entries.accept(Items.CAULDRON.getDefaultInstance());
                entries.accept(Items.CAMPFIRE.getDefaultInstance());
                entries.accept(Items.STONECUTTER.getDefaultInstance());
                entries.accept(Items.SCAFFOLDING.getDefaultInstance());
                entries.accept(Items.ANVIL.getDefaultInstance());
                entries.accept(Items.CHIPPED_ANVIL.getDefaultInstance());
                entries.accept(Items.DAMAGED_ANVIL.getDefaultInstance());
            })
            .title(REGISTRATE.addLang(
                "itemGroup", AnvilCraft.of("functional_block"), "AnvilCraft: Functional Block"))
            .withTabsBefore(ANVILCRAFT_TOOL.getId(), ANVILCRAFT_INGREDIENTS.getId())
            .withTabsAfter(AnvilCraft.of("building_block"))
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_BUILD_BLOCK =
        DF.register("building_block", () -> CreativeModeTab.builder()
            .icon(ModBlocks.REINFORCED_CONCRETES.get(Color.WHITE)::asStack)
            .displayItems((ctx, entries) -> {
            })
            .title(REGISTRATE.addLang(
                "itemGroup", AnvilCraft.of("building_block"), "AnvilCraft: Building Block"))
            .withTabsBefore(
                ANVILCRAFT_TOOL.getId(), ANVILCRAFT_INGREDIENTS.getId(), ANVILCRAFT_FUNCTION_BLOCK.getId())
            .build());

    public static void register(IEventBus modEventBus) {
        DF.register(modEventBus);
    }
}
